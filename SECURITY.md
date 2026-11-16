# Security policy

This project is an early MVP. Only the current source version is maintained; there is no production support commitment.

Do not open a public issue containing secrets or a working exploit against a real environment. Use [GitHub private vulnerability reporting](https://github.com/riadh-mnasri/terraform-change-review/security/advisories/new) for confidential reports. If the form is unavailable, contact the maintainer privately through the contact details on their GitHub profile; do not publish vulnerability details in an issue.

Threat model: an MCP caller may supply a path; a plan may contain sensitive or hostile data. The server restricts file access to a configured root, bounds input size, minimizes returned values, applies sensitivity masks and suppresses parser excerpts. It has no execute/apply tools and makes no network calls during analysis.

Non-goals: sandboxing a compromised local host, discovering every unmarked secret, protecting data after an AI client receives it, validating cloud state, or proving that an infrastructure change is safe. Other Claude Code tools are outside this server's permission boundary.
