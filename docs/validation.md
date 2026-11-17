# Local validation

Validated with OpenJDK 17.0.19 on macOS, 2026-11-17.

- `./mvnw --batch-mode --no-transfer-progress clean verify`: 37 tests passed, zero failures/errors/skips, with Maven 3.9.9. The wrapper checksum check explicitly reads stdin for compatibility with macOS and GNU `sha256sum`.
- Engine checks: destructive changes, both replacement orderings, unknown ancestors and nested values, IAM additions/removals, active ingress filtering, protection toggles, output inventory, incomplete plans and unsupported input.
- Data boundaries: nested/ancestor/before sensitivity masks, suppression of unrelated values and parser excerpts, path traversal/symlink rejection, oversized input rejection and Markdown escaping.
- Official MCP SDK client initialized a real subprocess, discovered both tools, reviewed a fixture, rejected an escaping path and received a sanitized malformed-input error.
- Packaged shaded JAR separately passed a legacy MCP stdio initialize/tool-call smoke test.
- Packaged CLI: risky fixture returns 1 by default, label-only fixture returns 0; incomplete fixture reports complete=false.
- Risky demo: 6 changed resources, 4 HIGH findings, 2 MEDIUM findings, 1 resource without specialized checks. Synthetic secret marker was absent from returned reports.
- Google provider fixtures: five complete exports generated with Terraform 1.15.7 / hashicorp/google 7.13.0 against fabricated local state. Project/bucket IAM members and bindings, firewall and bucket transitions pass the expected finding/evidence assertions. The risky update produces 9 HIGH and 1 MEDIUM findings; the label scenario produces none. Recovery still reports two IAM replacements. See [generation and limits](google-provider-validation.md).
- The Google provider lockfile was generated with `terraform providers lock` for macOS/Linux AMD64/ARM64; Terraform reported packages signed by HashiCorp. Generation succeeded both with a local provider mirror and with the standard registry installation, using `-refresh=false` and no apply. Initial provider execution required running outside the process sandbox to permit local plugin communication.

Claude Code 2.1.326 was tested on 2026-11-17 with an explicit MCP configuration, no built-in tools, only the two review tools allowed, session persistence disabled and a USD 2 maximum API budget. Its initialization event reported `terraform-review` connected and both tools available. The default selected model was `claude-opus-5-5`. The run stopped before any tool invocation with `Credit balance is too low`, `is_error: true`, exit 1 and reported cost USD 0. The result subtype was `success` despite the error flag, so subtype alone is not a valid pass condition. No successful model-generated review is claimed. Raw transcripts remain local; the [Claude Code guide](claude-code.md) documents reproduction and acceptance criteria. MCP tool execution is separately verified by SDK integration tests.

Hosted CI for v0.1.0 and the subsequent GitHub Actions maintenance PR passed on Java 17 and 21. The v0.1.0 release JAR was downloaded, its published checksum verified, and its help command executed successfully.

Not validated: a complete Claude Code conversation, a live cloud project, production plans or a provider-version compatibility matrix. No cloud infrastructure was applied. Current hosted CI and publication results are available in the repository's GitHub Actions and Releases pages.
