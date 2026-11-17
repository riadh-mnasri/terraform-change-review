# Wiki source and publication

The Markdown pages here are the reviewed source for the [published GitHub Wiki](https://github.com/riadh-mnasri/terraform-change-review/wiki). Edit them through a branch and pull request in the main repository. Publish only after that PR is merged. Detailed contracts remain in `docs/`; wiki pages provide guided entry points.

After merge, a maintainer can clone `git@github.com:riadh-mnasri/terraform-change-review.wiki.git` into a temporary directory and copy these pages from the merged commit. Exclude this README. Rewrite local Markdown page links from `Page.md` to `Page` for GitHub Wiki routes; keep external repository documentation links unchanged. Include `_Sidebar.md` and `_Footer.md`. The current source covers onboarding, review interpretation, MCP setup, coverage, troubleshooting, community spaces and contribution guidance.

Inspect the wiki diff, commit it with the source PR/commit reference, and push to the wiki repository. Do not overwrite existing pages without reviewing their contents. Verify Home, all navigation links and the sidebar after publication. Wiki edits made directly through GitHub must be reconciled through a source PR before the next publication.

The README links to the published wiki and to this reviewed source. Retain this source as the authoritative version and reconcile any direct wiki edits through a pull request before the next publication.
