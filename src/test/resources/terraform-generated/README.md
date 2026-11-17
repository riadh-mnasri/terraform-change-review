# Terraform-generated plan fixtures

These are complete `terraform show -json` exports generated with Terraform 1.15.7
on macOS amd64. They use the built-in `terraform_data` provider and the adjacent
`main.tf`. No cloud provider, remote backend or real credentials were used.

Regenerate from the repository root with `scripts/generate-terraform-fixtures.sh`.
The script applies **only local terraform_data state** in a disposable directory,
then exports creation, replacement, destruction and no-op plans. It does not apply
a destruction/replacement or create any cloud infrastructure.

Timestamps, local resource UUIDs and some Terraform metadata change on regeneration;
tests assert behavior rather than exact bytes. All data, including the sensitive
output marker, is synthetic. Do not replace these with production plan exports.

These fixtures validate Terraform's plan envelope, unknown outputs, sensitivity
metadata and generic action analysis. GCP-specific checks have synthetic regression
fixtures; these plans do not establish compatibility with any Google provider version.
