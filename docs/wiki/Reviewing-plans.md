# Reviewing plans

Read the findings first, then check which parts of the plan were unknown, skipped or unsupported. A finding includes a stable rule ID, resource, severity, before/after evidence, possible impact and a next check. HIGH indicates priority for review, not a proven outage or exploit.

| Item | What it tells you |
|---|---|
| `findings` | Supported changes that triggered a rule |
| `checks` | Per-rule EXECUTED/SKIPPED status, or NOT_SUPPORTED for items without specialized rules |
| `changes[].unknownPaths` | Planned values Terraform cannot yet determine |
| `summary.complete` | Terraform plan completeness: true, false, or null when not reported |
| `limitations` | Conclusions the analyzer does not establish |

For example, removing an IAM condition for a retained principal and role produces `GCP-IAM-CONDITION-REMOVED`. If the provider plans a replacement, `TF-REPLACE` also requires attention. Restoring the condition can remove the IAM finding while retaining a replacement warning.

## Example: inspect the evidence before drawing a conclusion

Suppose a plan changes a firewall source from a private range to the IPv4 world range. The tool can establish that the source range changed and that the rule allows ingress. It can report the configured ports and target selectors when those fields are present. It cannot establish whether a route, target tag, organization policy or upstream control makes the VM reachable.

Use the report as a structured question list:

1. **What changed?** Read the resource address, action and changed paths.
2. **What triggered the finding?** Check the rule ID, severity and selected before/after evidence.
3. **What did not get evaluated?** Check skipped rules, unknown paths and unsupported resources.
4. **What must a human verify?** Follow the `nextCheck` and the documented limitations.

For instance, `GCP-FIREWALL-WORLD` means the analyzer saw an active ingress allow rule with a changed `0.0.0.0/0` or `::/0` source. It does not mean that an internet connection was proven.

The same discipline applies to IAM: a `GCP-IAM-ELEVATED` finding means a new assignment of one of the curated sensitive roles appears in the plan. It does not resolve effective permissions, inherited policy, conditions or organization controls.

## Exit codes and automation

| Code | Meaning |
|---|---|
| 0 | No findings reached the selected severity threshold |
| 1 | Findings reached the selected severity threshold |
| 2 | Invalid input or configuration |

The default is `--fail-on high`; alternatives are `medium` and `none`. Unknowns, skipped checks, unsupported resources and incomplete plans do not independently change the exit code. CI needs explicit policy for them. Exit 0 is never approval to apply.

### Example CI behavior

```sh
java -jar build/libs/terraform-change-review.jar \
  review plan.json --format json --fail-on high
```

Exit `1` means at least one HIGH finding met the selected threshold. It does not mean every risk was found. A separate CI policy should decide what to do when `summary.complete` is `false` or `null`, when a relevant check is `SKIPPED`, or when an item is `NOT_SUPPORTED`.

See the [schema 1.1 report contract](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/report-format.md), [worked review](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/example-review.md) and [rule definitions](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/rules.md).
