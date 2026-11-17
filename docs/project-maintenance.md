# Project maintenance and recent improvements

This guide explains how the repository's build, documentation and GitHub community features fit together. It is written for a maintainer or contributor who wants to understand the setup and repeat the work. The detailed technical guides linked below are the source of truth for commands and contracts.

## Project status at a glance

Terraform Change Review is an early-stage Kotlin/JVM command-line tool and read-only MCP server. It reads a local Terraform JSON plan, inventories changes, and reports deterministic findings with evidence and coverage information. It does not read live cloud state or decide whether a plan is safe to apply.

The repository's recent open-source maintenance work covers:

| Area | What is configured | Where to learn more |
|---|---|---|
| Build | Production implementation in Kotlin; Gradle Wrapper builds, tests and packages it | [Kotlin and Gradle migration](kotlin-gradle-migration.md) |
| Regression checks | GitHub Actions builds on Java 17 and 21 and runs a packaged CLI example | [Validation record](validation.md) |
| Contributor workflow | Every change, including docs, is proposed through a pull request | [Contributing](../CONTRIBUTING.md) |
| Repository agents | Two selectable GitHub Copilot profiles with scoped instructions | [GitHub Agents](github-agents.md) |
| Community | Discussions for open-ended questions; Issues for actionable tasks | [Community spaces](github-community.md) |
| Wiki | Reviewed Markdown source lives in `docs/wiki`; publishing is a separate maintainer step | [Wiki source and publication](wiki/README.md) |
| Contributors | README links to GitHub's automatically maintained contributor graph | [Contributors](../README.md#contributors) |
| Artifact publishing | Signed Maven Central publication is configured but requires namespace verification and secrets | [Publishing](publishing.md) |

These files describe repository configuration. A workflow or profile appearing in a branch does not mean it has already run on GitHub. In particular, the Central publication workflow is manual, and wiki source changes are published only after their source pull request is merged.

## The Kotlin and Gradle build

The production code was moved from Java to Kotlin while preserving the command-line behavior, MCP tools, report schema and JVM entry points. The existing tests remain Java so they also exercise Kotlin-to-Java interoperability. Maven files were replaced by Gradle Kotlin DSL files and the checked-in Wrapper.

Build the project from the repository root:

```sh
./gradlew --no-daemon clean build
```

Then run the packaged program against the synthetic risky example:

```sh
java -jar build/libs/terraform-change-review.jar \
  review examples/gcp-risky.json --format markdown --fail-on none
```

`--fail-on none` is useful for demonstrations because it returns a report without turning findings into a failing process exit. In CI, teams can instead choose `--fail-on high` or `--fail-on medium` to make findings at that threshold return exit code `1`. Exit code `0` means only that the configured threshold was not reached; it is not an approval to apply the plan.

The executable shaded JAR is for `java -jar` and the MCP subprocess. The ordinary JVM library JAR is the artifact intended for Maven Central. See the migration guide for artifact names, Java compatibility details and wrapper integrity checks.

## How changes reach the project

The repository contribution rule is the same for source, workflow and documentation edits: branch, pull request, review, and green CI before merge.

1. For a concrete defect or feature, search the [existing issues](https://github.com/riadh-mnasri/terraform-change-review/issues) first. A discussion can help shape an open-ended idea; an issue should capture work that has clear acceptance criteria.
2. Create a focused branch and make the smallest coherent change. Add a synthetic fixture when a behavior depends on plan data; never commit an actual customer plan.
3. Update the relevant README, wiki source or technical guide in the same change so examples and limitations match the code.
4. Run `./gradlew --no-daemon clean build` locally and include the result and any manual checks in the pull request description.
5. Let GitHub Actions repeat the build on Java 17 and 21. Review the diff and findings before merging.

For example, a proposal to flag a new firewall transition should describe the before/after values and expected rule ID in an issue, add a small synthetic plan to the tests, document what the rule can and cannot infer, then submit all of that through one pull request.

## GitHub community and contributor visibility

Use [Discussions](https://github.com/riadh-mnasri/terraform-change-review/discussions) when the answer may benefit more than one contributor or when there is no agreed implementation yet. The project has an introduction discussion and a question asking which Terraform changes reviewers prioritize. For a specific task such as adding a supported resource check, use an [Issue](https://github.com/riadh-mnasri/terraform-change-review/issues) with a testable outcome.

Do not put credentials, production plans, account identifiers or private infrastructure details in public threads. Terraform plan JSON can contain secrets even when a report attempts to minimize returned values.

The README links to GitHub's [contributors graph](https://github.com/riadh-mnasri/terraform-change-review/graphs/contributors), which GitHub updates as contributions are merged. This avoids maintaining a hand-written list that can become stale. The maintainer is named separately in the README.

## GitHub Agents tab

The files in `.github/agents/` define two repository-specific profiles: one for explaining plan evidence without editing, and one for focused open-source maintenance. They are selectable instructions for GitHub Copilot coding agent; adding profile files does not start a session or create a review automatically.

Example bounded task for the plan reviewer:

> Review `examples/gcp-risky.json`. List findings by severity, cite each rule ID and evidence, and call out unknown values and unsupported checks. Do not change files.

Example bounded task for the maintainer:

> Compare the Gradle commands in the getting-started wiki source with the README. Correct any mismatch, update the documentation, and prepare the change as a pull request.

Review an agent's diff and CI results like any other contribution. Profile permissions, how to start a session and the account usage considerations are documented in [GitHub Agents](github-agents.md).

## Wiki publishing

The pages in `docs/wiki/` are the reviewable source for the GitHub Wiki. Edit and merge those pages through the main repository pull request first. Then a maintainer copies the merged pages to the separate `.wiki.git` repository, translates local Markdown links into GitHub Wiki page links, checks the diff, and verifies navigation after pushing.

For example, a source link `[Getting started](Getting-started.md)` becomes a Wiki route such as `Getting-started` when copied to the Wiki. Do not copy `docs/wiki/README.md`; it explains the publishing procedure rather than serving as a Wiki page. The full steps and safeguards are in [Wiki source and publication](wiki/README.md).

## Maven Central publishing

The Gradle build defines a signed Maven publication for `io.github.riadh-mnasri:terraform-change-review`. It is not publicly available from Maven Central until the maintainer verifies the namespace, stores the four required credentials/signing values as GitHub Actions secrets, and runs the manual workflow.

After publication, a consumer could use:

```kotlin
repositories { mavenCentral() }
dependencies {
    implementation("io.github.riadh-mnasri:terraform-change-review:0.1.0")
}
```

Before publication this snippet is an example of the intended coordinate, not proof that the artifact can already be resolved. Published versions cannot be replaced, so verify the version and artifacts first. Never put Central Portal tokens or private signing keys in the repository or a pull request. See the [publishing guide](publishing.md) for one-time setup and the manual release steps.

## Further reading

- [README](../README.md) — install, run and interpret a review.
- [Report format](report-format.md) and [worked example](example-review.md) — understand the generated output.
- [Rules and limitations](rules.md) — see current specialized coverage and blind spots.
- [Architecture](architecture.md) — follow the plan from input validation to CLI/MCP output.
- [Validation](validation.md) — see what was exercised and what has not been validated.
