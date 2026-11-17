# GitHub Agents

The repository has two custom profiles under `.github/agents`. GitHub displays these profiles in the agent selector on the repository's **Agents** tab. They are task-specific instructions for GitHub Copilot coding agent; they do not start sessions by themselves.

| Profile | Tool access | Intended work |
|---|---|---|
| `Terraform plan reviewer` | Read and search only | Explain plan evidence, rule IDs, unknowns and coverage limits without editing files. |
| `Open-source maintainer` | Read, search and edit | Improve contributor documentation and release readiness while checking claims against code. |

Both profiles set `disable-model-invocation: true`. This keeps them available for explicit selection and avoids automatic invocation. Their boundaries are part of the product: the plan reviewer cannot edit files; the maintainer cannot run shell commands, publish releases, change repository settings, merge pull requests or contact people.

## Start a session

After the profile files are merged to the default branch:

1. Open the repository's [Agents tab](https://github.com/riadh-mnasri/terraform-change-review/agents).
2. Start a task and select the profile that matches the work.
3. Give the agent a bounded prompt, such as:

   > Review `examples/gcp-risky.json` with the repository's Terraform plan reviewer profile. List the highest-priority findings, cite rule IDs and evidence, explain unknowns and unsupported resources, and make no file changes.

   > Review the getting-started wiki page against the current README and Gradle commands. Propose a focused documentation correction and prepare it as a pull request.

4. Review the generated diff and CI results in the pull request. Do not merge an agent result without human review.

GitHub agent sessions can use AI credits and Actions minutes. Review account usage and repository policy before launching expensive or broad tasks. The repository's main contribution rule remains: every change goes through a pull request.

For GitHub's current session and profile behavior, see [custom agent configuration](https://docs.github.com/en/copilot/reference/custom-agents-configuration) and [managing agent sessions](https://docs.github.com/en/copilot/concepts/agent-capabilities/choosing-and-delegating-agent-work/agent-management).
