# Getting started

## Install a release

Java 17+ is required. Download the JAR and checksum manifest from [v0.1.0](https://github.com/riadh-mnasri/terraform-change-review/releases/tag/v0.1.0):

```sh
curl -fLO https://github.com/riadh-mnasri/terraform-change-review/releases/download/v0.1.0/terraform-change-review-0.1.0.jar
curl -fLO https://github.com/riadh-mnasri/terraform-change-review/releases/download/v0.1.0/SHA256SUMS
shasum -a 256 -c SHA256SUMS
java -jar terraform-change-review-0.1.0.jar --help
```

On Linux, `sha256sum -c SHA256SUMS` also works. The manifest checks integrity against the published assets; it is not a maintainer signature.

## Try a source example

Clone the [repository](https://github.com/riadh-mnasri/terraform-change-review), install a Java 17+ JDK, then run from its root:

```sh
./gradlew clean build
java -jar build/libs/terraform-change-review.jar review examples/gcp-risky.json --format markdown --fail-on none
java -jar build/libs/terraform-change-review.jar rules
```

The first build downloads Gradle and dependencies. Reviews run locally without cloud credentials or an LLM. The example produces four HIGH and two MEDIUM findings. `--fail-on none` is useful for a demonstration; it is not a safety assessment.

## Review your plan

From your infrastructure repository, generate a plan without applying it:

```sh
terraform plan -out=review.tfplan
terraform show -json review.tfplan > plan.json
java -jar /absolute/path/terraform-change-review-0.1.0.jar review plan.json --format json
```

Use the actual absolute path to the downloaded JAR, or the source build's `build/libs/terraform-change-review.jar`. CLI input is confined to the current directory and descendants after symlink resolution. Input is limited to 20 MiB.

Continue with [review interpretation](Reviewing-plans.md). Keep plans local and out of version control.
