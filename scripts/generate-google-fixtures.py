#!/usr/bin/env python3
"""Generate real Google-provider plans against synthetic local state. Never apply."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


REPOSITORY = Path(__file__).resolve().parent.parent
FIXTURES = REPOSITORY / "src/test/resources/google-generated"
TERRAFORM_VERSION = "1.15.7"


def synthetic_state(plan):
    """Seed known planned values; this is fabricated state, not an apply result."""
    resources = []
    for resource in plan["planned_values"]["root_module"]["resources"]:
        values = dict(resource["values"])
        values["id"] = "synthetic-" + resource["name"]
        resources.append({
            "mode": resource["mode"],
            "type": resource["type"],
            "name": resource["name"],
            "provider": 'provider["registry.terraform.io/hashicorp/google"]',
            "instances": [{"schema_version": resource["schema_version"], "attributes": values}],
        })
    return {
        "version": 4,
        "terraform_version": TERRAFORM_VERSION,
        "serial": 1,
        "lineage": "a53119e1-814b-40db-b1b2-000000000001",
        "outputs": {},
        "resources": resources,
    }


def main():
    with tempfile.TemporaryDirectory(prefix="terraform-review-google-") as temporary:
        workspace = Path(temporary)
        for name in ("main.tf", ".terraform.lock.hcl"):
            shutil.copyfile(FIXTURES / name, workspace / name)
        # Avoid ambient credentials, CLI arguments, variables, workspaces and logs.
        environment = {
            key: value for key, value in os.environ.items()
            if not key.startswith(("GOOGLE_", "GCLOUD_", "CLOUDSDK_", "TF_VAR_", "TF_CLI_ARGS", "TF_LOG"))
            and key not in ("TF_WORKSPACE", "TF_DATA_DIR", "TF_REATTACH_PROVIDERS")
        }
        environment.update(HOME=temporary, CLOUDSDK_CONFIG=str(workspace / "gcloud"),
                           TF_IN_AUTOMATION="1", CHECKPOINT_DISABLE="1")

        def terraform(*arguments, capture=False):
            result = subprocess.run(
                ["terraform", f"-chdir={workspace}", *arguments],
                check=True, env=environment, text=True,
                stdout=subprocess.PIPE if capture else None,
            )
            return json.loads(result.stdout) if capture else None

        version = terraform("version", "-json", capture=True)["terraform_version"]
        if version != TERRAFORM_VERSION:
            raise SystemExit(f"Expected Terraform {TERRAFORM_VERSION}, found {version}")
        terraform("init", "-backend=false", "-input=false", "-lockfile=readonly", "-no-color")
        terraform("validate", "-no-color")

        def plan(name, scenario, state=None):
            state_path = workspace / "terraform.tfstate"
            if state is None:
                state_path.unlink(missing_ok=True)
            else:
                state_path.write_text(json.dumps(state) + "\n")
            terraform("plan", "-refresh=false", "-input=false", "-lock=false", "-no-color",
                      f"-var=scenario={scenario}", f"-out={name}.tfplan")
            return terraform("show", "-json", f"{name}.tfplan", capture=True)

        plans = {}
        plans["create"] = plan("create", "baseline")
        baseline = synthetic_state(plans["create"])
        plans["risky-update"] = plan("risky-update", "risky", baseline)
        plans["condition-expression"] = plan("condition-expression", "expression", baseline)
        plans["labels-only"] = plan("labels-only", "labels", baseline)
        plans["recovery"] = plan("recovery", "baseline", synthetic_state(plans["risky-update"]))
        # Publish exports only after every plan succeeds; saved JSON is never edited.
        for name, exported in plans.items():
            (FIXTURES / f"{name}.json").write_text(json.dumps(exported, indent=2) + "\n")
        print(f"Wrote {len(plans)} plan exports to {FIXTURES}")


if __name__ == "__main__":
    main()
