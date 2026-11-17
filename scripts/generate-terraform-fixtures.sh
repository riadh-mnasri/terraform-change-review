#!/bin/sh
# Uses only Terraform's built-in terraform_data provider in a disposable directory.
# No cloud credentials, remote backend or external provider are used.
set -eu
repository=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
fixture_directory="$repository/src/test/resources/terraform-generated"
fixture_workspace=$(mktemp -d)
trap 'rm -rf "$fixture_workspace"' EXIT HUP INT TERM
cp "$fixture_directory/main.tf" "$fixture_workspace/main.tf"
terraform -chdir="$fixture_workspace" init -input=false -no-color
terraform -chdir="$fixture_workspace" plan -input=false -no-color -out=create.tfplan
terraform -chdir="$fixture_workspace" show -json create.tfplan > "$fixture_directory/create.json"
# This apply affects only local terraform_data state; it creates no cloud infrastructure.
terraform -chdir="$fixture_workspace" apply -input=false -no-color create.tfplan
terraform -chdir="$fixture_workspace" plan -input=false -no-color -var=label=after -out=replace.tfplan
terraform -chdir="$fixture_workspace" show -json replace.tfplan > "$fixture_directory/replace.json"
terraform -chdir="$fixture_workspace" plan -input=false -no-color -destroy -out=delete.tfplan
terraform -chdir="$fixture_workspace" show -json delete.tfplan > "$fixture_directory/delete.json"
terraform -chdir="$fixture_workspace" plan -input=false -no-color -out=noop.tfplan
terraform -chdir="$fixture_workspace" show -json noop.tfplan > "$fixture_directory/noop.json"
