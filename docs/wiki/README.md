# Wiki source and publication

The Markdown pages here are the reviewed source for the GitHub Wiki. Edit them through a branch and pull request in the main repository. Publish only after that PR is merged. Detailed contracts remain in `docs/`; wiki pages provide guided entry points.

The GitHub Wiki feature is enabled, but its separate Git repository was not accessible during preparation. GitHub may require a maintainer to create the first page through the Wiki tab before cloning it. Do not claim publication until the pages can be opened on GitHub.

After merge, a maintainer can clone `git@github.com:riadh-mnasri/terraform-change-review.wiki.git` into a temporary directory and copy these pages from the merged commit. Exclude this README. Rewrite local Markdown page links from `Page.md` to `Page` for GitHub Wiki routes; keep external repository documentation links unchanged. Include `_Sidebar.md` and `_Footer.md`.

Inspect the wiki diff, commit it with the source PR/commit reference, and push to the wiki repository. Do not overwrite existing pages without reviewing their contents. Verify Home, all navigation links and the sidebar after publication. Wiki edits made directly through GitHub must be reconciled through a source PR before the next publication.

The README links to the source Home page until first publication, avoiding a link to an empty wiki. Once the wiki is live, change that entry through a PR and retain this source as the authoritative reviewed content.
