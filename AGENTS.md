# Repository instructions

## HARD REQUIREMENT: johninnl identity only

- Use exactly `johninnl <349981574+johninnl@users.noreply.github.com>` for **every Git author and committer**, on every branch and tag history, including generated release commits, merges, cherry-picks, and amended commits. Never create or retain commits under another person, automation account, or bot identity.
- Do not add `Co-authored-by` trailers or commit-message attribution identifying another author. Do not introduce the previous personal identity into repository files, commit metadata, or release metadata.
- Before each commit or push, run `GITLAB_DESTINATION=gitlab node .gitlab/scripts/identity.cjs` to verify GitLab authentication as `johninnld`, effective Git identity and all reachable history. Verify `git var GIT_AUTHOR_IDENT` and `git var GIT_COMMITTER_IDENT`, including environment overrides. Local API authentication reads `~/.gitlab`; never log credentials.
- GitLab.com is the release publisher under `johninnld/nl-morphe-patches`. Local Git pushes use `~/gitlab`; automated releases use the separate protected/masked `JOHNINNL_GITLAB_TOKEN` owned by `johninnld`. Retain the exact johninnl Git metadata above and the existing Morphe signing identity. Never substitute a bot token or deploy key.
- Codeberg publication is stopped at the user's request. Do not push releases there, enable its Actions, or send GitLab credentials to another forge. Legacy forge profiles remain for history and are not the GitLab release path.
- Keep Dependabot version-update configuration disabled. Update dependencies under the required identity; security alerts may remain enabled.
- Treat these as mandatory requirements, not optional preferences. Preserve existing work when repairing identity history; do not silently bypass a failed identity check.

## Project workflow

- Publish stable releases only from `main`; `dev` publishes development prereleases. Promote unapproved app changes to `main` only after user confirmation. `.releaserc` and `.gitlab/gitlab-com.release.config.cjs` own release version selection and generated release files. Do not manually bump release versions.
- Preserve the source-based release-note and Morphe app-scope guards. Declare affected apps with `Release-Apps` when shared patch changes require it. Declare `Unchanged-Apps` only when a commit edits an app's files without changing what its patches produce, because every release bullet naming an app badges it for repatching in Morphe. A commit editing several apps' patch folders must declare each of them in one of the two trailers.
- Run `node --test tests/test_release_branding.cjs tests/test_preview_release.cjs tests/test_identity.cjs tests/test_codeberg.cjs tests/test_gitgud.cjs tests/test_gitlab.cjs tests/test_gitlab_publish.cjs` after release automation changes. Run `python3 -m unittest discover -s tests` for Python regression checks; use the release workflow's Gradle checks for Android changes.
- CI audits checked-out history; release automation also verifies the token owner and effective Git identities before publishing. These checks do not enforce local operations unless run, and cannot stop an already-authorized direct push before it reaches GitLab. Follow the identity requirements for every operation.
