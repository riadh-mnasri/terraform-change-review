# Community and project maintenance

This project is maintained in the open. Use the repository's community spaces for the kind of work they support:

| You want to… | Use… |
|---|---|
| Ask a broad question or share design feedback | [GitHub Discussions](https://github.com/riadh-mnasri/terraform-change-review/discussions) |
| Propose a feature or report a reproducible bug | [GitHub Issues](https://github.com/riadh-mnasri/terraform-change-review/issues) |
| Review an implementation | A pull request linked to an issue where possible |
| Run a project-specific coding assistant | The [Agents tab](https://github.com/riadh-mnasri/terraform-change-review/agents) |

## Discussions

The [project introduction](https://github.com/riadh-mnasri/terraform-change-review/discussions/22) explains what Terraform Change Review does and where its checks stop. The question about [Terraform review priorities](https://github.com/riadh-mnasri/terraform-change-review/discussions/23) invites practitioners to share which changes and evidence matter most in a review.

Use issues for tasks that need implementation and acceptance criteria. Do not post real plans, state, credentials, private infrastructure details or customer data in public threads.

## Agents

The repository contains two selectable custom profiles: a read-only Terraform plan reviewer and a documentation-focused open-source maintainer. See [how the profiles work](https://github.com/riadh-mnasri/terraform-change-review/blob/main/docs/github-agents.md). Agent output is proposed work; review its code, documentation claims and CI results in a pull request before merging.

## Contributors and releases

The [GitHub contributors page](https://github.com/riadh-mnasri/terraform-change-review/graphs/contributors) lists contributors automatically. [Riadh Mnasri](https://github.com/riadh-mnasri) started the project and maintains it.

The current standalone JAR is provided through [GitHub Releases](https://github.com/riadh-mnasri/terraform-change-review/releases). Maven Central publication is being prepared; until the maintainer verifies the namespace and configures signing and portal secrets, do not use the planned Maven coordinates as an available dependency.

All project source, including this wiki's Markdown, is reviewed through pull requests. The [contributor guide](Contributing.md) explains the workflow.
