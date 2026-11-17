terraform {
  required_version = ">= 1.4.0"
}
variable "label" {
  type = string
  default = "before"
}
resource "terraform_data" "example" {
  input = var.label
  triggers_replace = var.label
}
output "result" {
  value = terraform_data.example.output
}
output "secret" {
  sensitive = true
  value = "SYNTHETIC_SECRET_FOR_REDACTION_TEST"
}
