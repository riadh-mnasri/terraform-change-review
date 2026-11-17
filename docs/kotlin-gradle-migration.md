# Kotlin and Gradle migration

This project now uses Kotlin/JVM for its production code and Gradle for building, testing and packaging. The migration preserves the existing CLI, MCP tools, report schema and Java/JVM entry points.

## What changed

| Before | After | Why it matters |
|---|---|---|
| Java production sources in `src/main/java` | Kotlin production sources in `src/main/kotlin` | New implementation work uses Kotlin while staying on the JVM. |
| Maven `pom.xml` and Maven Wrapper | Gradle Kotlin DSL and Gradle Wrapper 9.7 | One build describes compilation, tests, runnable JAR and publication. |
| Maven Shade Plugin | Gradle Shadow plugin | The CLI still ships as a single executable JAR. |
| Maven Surefire / JUnit Jupiter | Gradle `test` task with JUnit Platform | The same existing test suite runs from Gradle. |

Production modules are all Kotlin: the bounded plan reader, validator, JSON redaction helpers, rule catalog, deterministic review engine, Markdown renderer, CLI and MCP server. The existing Java test sources remain in place as Java callers. That lets the test suite check that the Kotlin code still exposes the JVM API expected by Java users and the MCP subprocess test.

`Review` and its nested report types are Kotlin data classes emitted as JVM records. `Main` retains static `main` and `run` entry points for CLI and integration compatibility. The official MCP Java SDK remains in use through normal Kotlin/Java interoperability.

## Build and run

Requirements: Java 17 or later. The first wrapper build downloads the pinned Gradle distribution and dependencies from Maven Central.

```sh
./gradlew --no-daemon clean build
```

The build compiles both source sets, runs the 37 existing regression tests, and produces two artifacts:

- `build/libs/terraform-change-review.jar` — shaded executable JAR used by the CLI and MCP examples.
- `build/libs/terraform-change-review-0.1.0.jar` — the standard JVM library JAR used by the Maven publication.

Try the packaged CLI:

```sh
java -jar build/libs/terraform-change-review.jar \
  review examples/gcp-risky.json --format markdown --fail-on none
```

The Gradle Wrapper distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`; the wrapper JAR checksum is checked in CI using `gradle/wrapper/gradle-wrapper.jar.sha256`. Dokka generates the Kotlin API reference in Javadoc format for the Maven publication; Gradle's built-in Javadoc task alone has no Java production sources to document.

## CI and regression checks

The `Kotlin and Gradle verification` workflow runs `./gradlew --no-daemon clean build` on Java 17 and Java 21, checks the wrapper JAR checksum, and runs a packaged CLI smoke review. Release packaging also uses the Gradle Wrapper. The separate `Publish to Maven Central` workflow is manual and requires maintainer credentials and a signing key.

See the [validation record](validation.md) for the migration build results and the [contributing guide](../CONTRIBUTING.md) for the pull request process.
