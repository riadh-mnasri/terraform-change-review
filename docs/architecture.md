# Architecture decisions

## Separate analysis from presentation

`PlanReader` loads bounded, local JSON and rejects duplicate keys. `PlanValidator` validates structure, types, actions and value masks. `ReviewEngine` applies deterministic checks and builds immutable report records. `Main` exposes that engine via a CLI and the official MCP Java SDK over stdio. `Markdown` renders reports for human review. The implementation is Kotlin/JVM; tests retain Java callers to protect the JVM API used by existing integrations.

The analysis needs no cloud account or LLM. This makes examples reproducible, rules reviewable, and the same result usable in CI and a coding assistant.

## Scope the input

Review a saved JSON plan, not HCL alone. Planned before/after values and actions are the source of evidence. Format major versions other than 1 are rejected. Errored plans and unknown action combinations are rejected rather than silently interpreted.

The MCP server is confined to an explicit root, resolves symlinks and reads at most 20 MiB. Errors do not echo parser messages. The root is a boundary for normal callers, not a defense against concurrent filesystem mutation by another local process.

## Return evidence, not arbitrary plan content

An inventory contains addresses, types, actions and JSON Pointer paths. Only rules select values for evidence. Before/after masks are applied independently and recursively; an ancestor marked sensitive redacts the entire selected value. Variables, full state and output values are never returned.

This is data minimization, not a guarantee of secret discovery. Unmarked secrets in selected fields, resource names and paths can still be exposed. Returned strings may contain hostile instructions and must be treated as data by consumers.

## Distinguish coverage from verdict

`inventory-only` means no specialized semantic checks. `partial-specialized` means selected checks, never complete coverage. Every report states limitations. Unknown values skip dependent rules and remain visible in the inventory. Every applicable rule emits an EXECUTED/SKIPPED outcome with a stable reason code; NOT_SUPPORTED marks items without specialized checks. Missing Terraform completeness is reported as null. The summary's `complete` reflects Terraform plan completeness and deferred changes, not analyzer coverage.

The first version has no universal risk score: explicit findings and their evidence are more useful than an unexplained number. HIGH means priority for human review, not confirmed exploitability, outage or data loss.

## Extend through rules, not prompts

Rules have shared metadata in RuleCatalog (IDs, severity, scope and input paths), with deterministic evaluation code and regression tests. JsonSupport centralizes unknown markers, diff paths and redaction. Future policy adapters should preserve evidence paths, unknown-value handling and redaction. Checkov/OPA integration should reuse their results rather than duplicate their catalogs. Runtime/cloud enrichment would be optional and independently permissioned.

## Compatibility

Java 17 baseline, Kotlin 2.4.21 and Gradle 9.7. MCP SDK 2.1.0's legacy client/server API is used for stdio compatibility. The integration suite spawns a real server process and exercises initialization, discovery, successful calls and path-boundary rejection with the official SDK client. Claude Code connection and model-backed tool execution are tracked separately; see validation.md.
