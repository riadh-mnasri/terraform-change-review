# Contributing

Start with an issue describing the resource, before/after change, expected finding and a synthetic plan fixture. Do not include real plans, credentials, project identifiers or customer data.

Run `mvn clean verify` with Java 17+ before submitting a PR.

A new rule should include a stable ID, documented provider/resource scope, evidence paths, severity rationale and a concrete next check. Test the risky transition, an improvement, an unrelated update, unknown values and sensitivity masks. Avoid interpreting absence as false unless documented provider semantics justify it. Keep network calls and LLM calls out of the engine.

A change to input handling should include boundary, malformed-input and error-redaction tests. A change to MCP should exercise a real SDK client/server exchange. Describe observable behavior and limitations in the PR.
