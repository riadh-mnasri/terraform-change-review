# Changelog

## 0.1.0 — 2026-11-16

- Local Terraform JSON plan inventory across resources and outputs.
- Eleven deterministic checks for destructive actions and selected GCP changes, including IAM condition removal and expression changes.
- Strict plan envelope, action, value and sensitivity-mask validation; sanitized diagnostic codes.
- Per-rule executed/skipped status and explicit unsupported coverage; nullable plan completeness.
- Shared rule catalog, JSON/Markdown reports and configurable severity thresholds.
- Read-only stdio MCP tools using the official Java SDK.
- Local path confinement, bounded input, duplicate-key rejection and evidence redaction.
- Full Terraform-generated built-in provider fixtures plus synthetic GCP regressions.
- Maven Wrapper with checksum-pinned downloads, Java 17/21 CI and tag-triggered release artifacts with SHA-256 manifests.
