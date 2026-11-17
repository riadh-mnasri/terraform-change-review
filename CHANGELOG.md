# Changelog

## Unreleased

- Migrate production code and build from Java/Maven to Kotlin/Gradle while preserving the CLI and MCP interfaces.
- Add signed Maven Central publication configuration, repository-specific GitHub Copilot agent profiles, and a contributor link in the README.
- Expand the published wiki with troubleshooting, clearer contribution guidance, and Gradle-based instructions.
- Add reviewed wiki source pages and README diagrams for the analysis pipeline and review process.
- Add five Google-provider plan exports and regression tests for selected IAM, firewall and bucket transitions, generated against fabricated local state without cloud apply.
- Document fixture regeneration, tested versions, expected findings and limits; expand README guidance for interpreting reports.

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
