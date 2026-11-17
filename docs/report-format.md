# Report contract — schema 1.1

Each report contains `schemaVersion`, `summary`, `changes`, `findings`, `checks` and `limitations`.

- `summary.complete`: boolean or null; reflects Terraform's `complete` field, overridden to false if deferred changes are present. Null means not reported. Never a safety verdict.
- `summary.checksExecuted` / `checksSkipped`: counts of per-rule evaluations, not resource counts.
- `summary.itemsWithoutSpecializedChecks`: resources, data sources and outputs marked NOT_SUPPORTED.
- `summary.resourcesWithoutSpecializedChecks`: only resources/data sources with inventory-only coverage; excludes outputs.
- `changes`: action inventory, JSON Pointer changed/unknown paths, import flag and previous address. Provider-specific semantic coverage remains partial.
- `findings`: stable rule ID, severity, resource, title, selected redacted evidence, impact and next verification.
- `checks`: resource, ruleId, status and reasonCode. Synthetic ruleId `SPECIALIZED` records NOT_SUPPORTED.

Coverage reason codes:

| Status | reasonCode |
|---|---|
| EXECUTED | EVALUATED |
| SKIPPED | UNKNOWN_VALUE, MISSING_ATTRIBUTE, INVALID_ATTRIBUTE_TYPE, ACTION_NOT_APPLICABLE, DATA_RESOURCE |
| NOT_SUPPORTED | RESOURCE_TYPE_NOT_SUPPORTED, DATA_RESOURCE, OUTPUT_ONLY |

Rules that do not apply to a resource type are not listed individually; unsupported items receive a single SPECIALIZED outcome. Ordinary no-op resources/outputs are omitted. Imports/moves with no-op actions remain in the inventory.

An EXECUTED check only covers the rule's documented scope. For example, an executed IAM assignment check does not imply effective IAM was evaluated. Removing a condition is reported for principal-role pairs present both before and after; changing an expression is flagged for review, not evaluated as a logical implication.

CLI exit codes: 0 below selected finding threshold, 1 threshold reached, 2 invalid/unreadable input or configuration. Unknowns, skipped checks and incomplete plans do not independently change the exit code; consumers should inspect them explicitly. CLI errors contain diagnostic codes for structural validation failures and never input excerpts. MCP returns these failures as tool errors (`isError: true`).

This is the first tagged report contract. Consumers should tolerate additive fields and check schemaVersion before relying on field semantics.
