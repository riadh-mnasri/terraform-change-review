# Local validation

Validated with OpenJDK 17.0.19 on macOS, 2026-11-16.

- `mvn clean verify`: 19 tests passed, zero failures/errors/skips. Maven was run in offline mode after dependencies were fetched from Maven Central because direct Maven DNS resolution was unavailable in the execution environment.
- Engine checks: destructive changes, both replacement orderings, unknown ancestors and nested values, IAM additions/removals, active ingress filtering, protection toggles, output inventory, incomplete plans and unsupported input.
- Data boundaries: nested/ancestor/before sensitivity masks, suppression of unrelated values and parser excerpts, path traversal/symlink rejection, oversized input rejection and Markdown escaping.
- Official MCP SDK client initialized a real subprocess, discovered both tools, reviewed a fixture, rejected an escaping path and received a sanitized malformed-input error.
- Packaged shaded JAR separately passed a legacy MCP stdio initialize/tool-call smoke test.
- Packaged CLI: risky fixture returns 1 by default, label-only fixture returns 0; incomplete fixture reports complete=false.
- Risky demo: 6 changed resources, 4 HIGH findings, 2 MEDIUM findings, 1 resource without specialized checks. Synthetic secret marker was absent from returned reports.

Not validated: actual Claude Code account integration, a live cloud project, production plans, provider-version compatibility matrix, hosted GitHub Actions, and the Java 21 CI job. No infrastructure was applied and no external publication was performed.
