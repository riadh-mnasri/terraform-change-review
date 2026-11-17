# Contributing

Use a dedicated branch and pull request for every change, including documentation and dependency updates. Describe the user-visible result, validation performed and limitations. Wait for the Java 17 and 21 Gradle CI jobs before merging.

## Prepare a contribution

For a bug, include a minimal sanitized example and the behavior you expected. For a feature, explain the user problem, scope, acceptance criteria and any compatibility impact. Keep each pull request focused so maintainers can review its effect without guessing.

```sh
./gradlew --no-daemon clean build
```

This compiles production code, runs the committed regression suite and creates the executable CLI JAR. For a rules-only change, the suite uses committed JSON fixtures and does not regenerate them. To regenerate provider exports, follow the separate [Google fixture guide](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/google-provider-validation.md).

When proposing a rule, provide its resource scope, before/after transition, stable ID, severity rationale, selected evidence and a concrete next check. Cover risky changes, improvements, unrelated changes, unknowns and sensitivity masks. Use synthetic examples only. Do not include production plans, state, credentials or customer identifiers in issues or pull requests.

Keep the README, wiki and detailed rule documentation consistent. Clearly distinguish planned features, supported behavior, synthetic tests and live validation. Review wiki changes in the main repository before publishing them.

## Pull request checklist

- Explain the user-visible change and link the related issue or discussion.
- Include or update tests for both the risky transition and relevant non-risky or unknown cases.
- Keep fixtures synthetic and document how provider-generated plans were produced.
- Update the README, wiki source and detailed docs where their instructions or claims changed.
- Report the exact build/test command and any checks that were not run.
- Do not merge until Java 17 and Java 21 CI checks pass.

See [CONTRIBUTING.md](https://github.com/riadh-mnasri/terraform-change-review/blob/main/CONTRIBUTING.md), [architecture](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/architecture.md), [security reporting](https://github.com/riadh-mnasri/terraform-change-review/blob/main/SECURITY.md) and [troubleshooting](Troubleshooting.md). Do not publish real plans or security-sensitive reproduction data in public issues.
