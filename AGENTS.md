# Repository instructions

## HARD REQUIREMENT: johninnl identity only

- Use exactly `johninnl <349981574+johninnl@users.noreply.github.com>` for **every Git author and committer**, on every branch and tag history, including generated release commits, merges, cherry-picks, and amended commits. Never create or retain commits under another person, automation account, or bot identity.
- Do not add `Co-authored-by` trailers or commit-message attribution identifying another author. Do not introduce the previous personal identity into repository files, commit metadata, or release metadata.
- Before each commit or push, run `node .forgejo/scripts/identity.cjs --local` to verify Codeberg authentication as `JohninNL`, effective Git identity and all reachable history. Verify the effective author and committer with `git var GIT_AUTHOR_IDENT` and `git var GIT_COMMITTER_IDENT`, including environment overrides. Local API authentication reads `CODEBERG_TOKEN` or `CODEBERG_TOKEN_FILE` (default `~/.codeberg`); never log credentials.
- Authenticate every Codeberg push and release as `JohninNL`, retaining the exact `johninnl` Git metadata above. Local Git pushes use the account SSH key (`~/codeberg`); configure `JOHNINNL_CODEBERG_TOKEN` as an owner-account Actions secret for automated checkout, release commits/tags and publishing. Do not substitute a bot token or deploy key. The original GitHub remote and legacy workflows remain for history; Codeberg uses `.forgejo/workflows/`.
- Keep Dependabot version-update configuration disabled. Update dependencies under the required identity; security alerts may remain enabled.
- Treat these as mandatory requirements, not optional preferences. Preserve existing work when repairing identity history; do not silently bypass a failed identity check.

## Project workflow

- Publish stable releases only from `main`; `dev` publishes development prereleases. Promote unapproved app changes to `main` only after user confirmation. `.releaserc` and the Codeberg adapter `.forgejo/release.config.cjs` own release version selection and generated release files. Do not manually bump release versions.
- Preserve the source-based release-note and Morphe app-scope guards. Declare affected apps with `Release-Apps` when shared patch changes require it. Declare `Unchanged-Apps` only when a commit edits an app's files without changing what its patches produce, because every release bullet naming an app badges it for repatching in Morphe. A commit editing several apps' patch folders must declare each of them in one of the two trailers.
- Run `node --test tests/test_release_branding.cjs tests/test_preview_release.cjs tests/test_identity.cjs tests/test_codeberg.cjs` after release automation changes. Run `python3 -m unittest discover -s tests` for Python regression checks; use the release workflow's Gradle checks for Android changes.
- CI audits checked-out history; release automation also verifies the token owner and effective Git identities before publishing. These checks do not enforce local operations unless run, and cannot stop an already-authorized direct push before it reaches Codeberg. Follow the identity requirements for every operation.
