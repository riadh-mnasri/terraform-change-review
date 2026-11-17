# Contributing

Use a dedicated branch and pull request for every change, including documentation and dependencies. Describe the observable result, validation performed and limitations. Wait for the Java 17 and 21 checks before merging.

```sh
./mvnw --batch-mode --no-transfer-progress clean verify
```

When proposing a rule, provide its resource scope, before/after transition, stable ID, severity rationale, selected evidence and a concrete next check. Test risky changes, improvements, unrelated changes, unknowns and sensitivity masks. Use synthetic examples only.

Keep the README, wiki and detailed rule documentation consistent. Clearly distinguish planned features, supported behavior, synthetic tests and live validation. Review wiki changes in the main repository before publishing them.

See [CONTRIBUTING.md](https://github.com/riadh-mnasri/terraform-change-review/blob/main/CONTRIBUTING.md), [architecture](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/architecture.md) and [security reporting](https://github.com/riadh-mnasri/terraform-change-review/blob/main/SECURITY.md). Do not publish real plans or security-sensitive reproduction data in public issues.
