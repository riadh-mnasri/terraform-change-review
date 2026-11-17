# Local validation

Validated with OpenJDK 17.0.19 on macOS, 2026-11-17.

- `./mvnw --batch-mode --no-transfer-progress clean verify`: 32 tests passed, zero failures/errors/skips, with Maven 3.9.9. The wrapper checksum check explicitly reads stdin for compatibility with macOS and GNU `sha256sum`.
- Engine checks: destructive changes, both replacement orderings, unknown ancestors and nested values, IAM additions/removals, active ingress filtering, protection toggles, output inventory, incomplete plans and unsupported input.
- Data boundaries: nested/ancestor/before sensitivity masks, suppression of unrelated values and parser excerpts, path traversal/symlink rejection, oversized input rejection and Markdown escaping.
- Official MCP SDK client initialized a real subprocess, discovered both tools, reviewed a fixture, rejected an escaping path and received a sanitized malformed-input error.
- Packaged shaded JAR separately passed a legacy MCP stdio initialize/tool-call smoke test.
- Packaged CLI: risky fixture returns 1 by default, label-only fixture returns 0; incomplete fixture reports complete=false.
- Risky demo: 6 changed resources, 4 HIGH findings, 2 MEDIUM findings, 1 resource without specialized checks. Synthetic secret marker was absent from returned reports.

Claude Code 2.1.325 connected to the local server during earlier validation. The model-backed conversation smoke test could not finish because its API requests failed in the execution environment; MCP tool execution is separately verified by SDK integration tests.

Not validated locally: a complete Claude Code conversation, a live cloud project, production plans, provider-version compatibility matrix, hosted GitHub Actions, and the Java 21 CI job. No cloud infrastructure was applied. Hosted CI and publication results are available in the repository's GitHub Actions and Releases pages.
