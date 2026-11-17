# Contributing

Start with an issue describing the resource, before/after change, expected finding and a synthetic plan fixture. Do not include real plans, credentials, project identifiers or customer data.

Run `./gradlew --no-daemon clean build` with Java 17+ before submitting a PR.

Use a dedicated branch and pull request for every change, including documentation and dependency updates. Include the relevant validation results; wait for Java 17 and 21 CI before merging.

Keep the README's quick start, coverage and limitations consistent with the implementation. Document new fixture provenance and reproducible generation steps. Distinguish hand-written scenarios, provider-generated plans against fabricated state, and live cloud validation. The [Google fixture guide](docs/google-provider-validation.md) explains the pinned toolchain, expected findings and limits; normal Gradle tests do not regenerate fixtures or access GCP.

Register rule metadata in `RuleCatalog`, add evaluation in `ReviewEngine`, and document coverage outcomes. A new rule should include a stable ID, documented provider/resource scope, evidence paths, severity rationale and a concrete next check. Test the risky transition, an improvement, an unrelated update, unknown values and sensitivity masks. Avoid interpreting absence as false unless documented provider semantics justify it. Keep network calls and LLM calls out of the engine.

A change to input handling should include boundary, malformed-input and error-redaction tests. A change to MCP should exercise a real SDK client/server exchange. Describe observable behavior and limitations in the PR.

See [Maven Central publishing](docs/publishing.md) for the maintainer-only namespace, signing and credential setup. Never include Central Portal tokens or signing material in a pull request.
