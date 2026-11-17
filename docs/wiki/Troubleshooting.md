# Troubleshooting

## The CLI rejects the input

Create a JSON plan from a saved Terraform plan with `terraform show -json`. Pass the JSON file path, not the binary `.tfplan` file:

```sh
terraform plan -out=review.tfplan
terraform show -json review.tfplan > plan.json
java -jar /path/to/terraform-change-review.jar review plan.json
```

The CLI resolves relative paths from the current directory. The MCP server resolves paths inside the configured `--root`. Symlinks are resolved before the root boundary is checked. Input must be a regular JSON file no larger than 20 MiB.

Exit code `2` means the input or configuration was invalid. The diagnostic code identifies the rejected condition; parser errors do not echo input excerpts because plans can contain secrets.

## A value appears as unknown or a check is skipped

Terraform may not know a planned value until apply. Check `changes[].unknownPaths` and the related `checks` entry. A skipped check is not a clean result. Review the value's dependency and decide whether the plan is reviewable in its current form.

## No findings were returned

The tool only evaluates its implemented rules and selected fields. Review `checks`, unsupported resources, unknown paths, `summary.complete`, and `limitations`. Exit code `0` only means the selected severity threshold was not reached; it never approves an apply.

## Claude Code cannot connect to MCP

Confirm Java 17+ is available to the Claude Code process, use absolute paths in `claude mcp add`, and make sure the configured root exists. Run `claude mcp get terraform-review`, then use `/mcp` in Claude Code to inspect the connection. Test the same plan with the CLI to separate server startup problems from plan parsing problems.

The MCP server itself has no cloud credentials and cannot apply changes. A hosted assistant may receive tool output, so follow your organization's data policy before sharing real plan evidence.

## Gradle cannot build the project

Use the committed wrapper from the repository root and Java 17 or later:

```sh
./gradlew --no-daemon clean build
```

The first build requires network access to download Gradle and dependencies from Maven Central. The analyzer does not make network requests while reviewing a plan.

For a confirmed product bug, include the command, sanitized output, Java version and operating system in an issue. Never attach real plan files, state, credentials or unredacted logs.

Continue to [contributing](Contributing.md) or review the [validation record](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/validation.md).
