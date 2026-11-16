# Architecture decisions

## Separate analysis from presentation

`PlanReader` loads bounded, local JSON. `ReviewEngine` applies deterministic checks and builds immutable report records. `Main` exposes that engine via a CLI and the official Java MCP SDK over stdio. `Markdown` renders reports for human review.

The analysis needs no cloud account or LLM. This makes examples reproducible, rules reviewable, and the same result usable in CI and a coding assistant.

## Scope the input

Review a saved JSON plan, not HCL alone. Planned before/after values and actions are the source of evidence. Format major versions other than 1 are rejected. Errored plans and unknown action combinations are rejected rather than silently interpreted.

The MCP server is confined to an explicit root, resolves symlinks and reads at most 20 MiB. Errors do not echo parser messages. The root is a boundary for normal callers, not a defense against concurrent filesystem mutation by another local process.

## Return evidence, not arbitrary plan content

An inventory contains addresses, types, actions and JSON Pointer paths. Only rules select values for evidence. Before/after masks are applied independently and recursively; an ancestor marked sensitive redacts the entire selected value. Variables, full state and output values are never returned.

This is data minimization, not a guarantee of secret discovery. Unmarked secrets in selected fields, resource names and paths can still be exposed. Returned strings may contain hostile instructions and must be treated as data by consumers.

## Distinguish coverage from verdict

`inventory-only` means no specialized semantic checks. `partial-specialized` means selected checks, never complete coverage. Every report states limitations. Unknown values skip dependent rules and remain visible in the inventory. The summary's `complete` reflects Terraform plan completeness and deferred changes, not analyzer coverage.

The first version has no universal risk score: explicit findings and their evidence are more useful than an unexplained number. HIGH means priority for human review, not confirmed exploitability, outage or data loss.

## Extend through rules, not prompts

For now rules are code, with fixed IDs and regression tests. Future policy adapters should preserve evidence paths, unknown-value handling and redaction. Checkov/OPA integration should reuse their results rather than duplicate their catalogs. Runtime/cloud enrichment would be optional and independently permissioned.

## Compatibility

Java 17 baseline. MCP SDK 2.1.0's legacy client/server API is used for stdio compatibility. The integration suite spawns a real server process and exercises initialization, discovery, successful calls and path-boundary rejection with the official SDK client. Actual Claude Code account integration is a separate manual smoke test.
