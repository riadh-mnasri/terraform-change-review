# Reviewing plans

Read the findings first, then check which parts of the plan were unknown, skipped or unsupported. A finding includes a stable rule ID, resource, severity, before/after evidence, possible impact and a next check. HIGH indicates priority for review, not a proven outage or exploit.

| Item | What it tells you |
|---|---|
| `findings` | Supported changes that triggered a rule |
| `checks` | Per-rule EXECUTED/SKIPPED status, or NOT_SUPPORTED for items without specialized rules |
| `changes[].unknownPaths` | Planned values Terraform cannot yet determine |
| `summary.complete` | Terraform plan completeness: true, false, or null when not reported |
| `limitations` | Conclusions the analyzer does not establish |

For example, removing an IAM condition for a retained principal and role produces `GCP-IAM-CONDITION-REMOVED`. If the provider plans a replacement, `TF-REPLACE` also requires attention. Restoring the condition can remove the IAM finding while retaining a replacement warning.

## Exit codes and automation

| Code | Meaning |
|---|---|
| 0 | No findings reached the selected severity threshold |
| 1 | Findings reached the selected severity threshold |
| 2 | Invalid input or configuration |

The default is `--fail-on high`; alternatives are `medium` and `none`. Unknowns, skipped checks, unsupported resources and incomplete plans do not independently change the exit code. CI needs explicit policy for them. Exit 0 is never approval to apply.

See the [schema 1.1 report contract](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/report-format.md), [worked review](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/example-review.md) and [rule definitions](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/rules.md).
