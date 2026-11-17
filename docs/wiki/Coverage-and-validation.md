# Coverage and validation

Generic checks identify managed-resource deletions and replacements across providers. Inventories include resource and output changes. Specialized rules cover selected GCP IAM, firewall, bucket, GKE and Cloud SQL changes; coverage remains partial even for these types.

The inventory and specialized checks answer different questions. Every changed resource or output can appear in the inventory, but only the resource types and input paths named by a rule get a specialized semantic check. A report with `inventory-only` coverage still contains useful action and changed-path information; it must not be read as a clean bill of health.

| Validation layer | What it establishes |
|---|---|
| Hand-written regressions | Rule transitions, unknowns, redaction, invalid inputs and boundary cases |
| Built-in-provider Terraform exports | Full plan envelopes, outputs, sensitivity metadata and generic action reporting |
| Google-provider exports | Selected IAM, firewall and bucket plans from Terraform 1.15.7 / Google 7.13.0 against fabricated local state |
| MCP SDK integration | Initialization, discovery, tool calls and sanitized input rejection |
| Claude Code connection | Client connects; full model conversation remains blocked by account access |

The Gradle suite has 37 tests and runs on Java 17 and 21 in CI. It reads committed fixtures without requiring Terraform or cloud credentials. Fixture generation is a separate operation.

Google fixtures include creation, risky updates, condition-expression changes, labels and recovery. No GCP apply or refresh was performed. The fabricated state lacks some cloud-computed values; computed-only differences are expected. GKE and Cloud SQL still use hand-written scenarios.

The automated suite deliberately separates four kinds of evidence:

1. **Hand-written plans** target narrow rule boundaries such as unknown values, sensitivity masks and malformed envelopes.
2. **Terraform built-in provider plans** exercise realistic plan serialization and generic change inventory.
3. **Google provider plans** exercise selected Google resource schemas and replacement behavior against fabricated state.
4. **MCP integration tests** start the server as a subprocess and call it with the official SDK client.

All checked-in plan values are synthetic. The tests do not need cloud credentials, a backend or a live GCP project.

This is not a provider-version compatibility matrix. There is no live effective-IAM evaluation, network simulation, cost prediction, runtime-state inspection or guarantee of dependency impact.

Read the [Google generation guide](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/google-provider-validation.md), [rule scope](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/rules.md) and [validation record](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/validation.md). For local setup problems, see [troubleshooting](Troubleshooting.md).
