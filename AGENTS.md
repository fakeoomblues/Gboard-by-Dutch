# Repository instructions

## HARD REQUIREMENT: johninnl identity only

- Use exactly `johninnl <349981574+johninnl@users.noreply.github.com>` for **every Git author and committer**, on every branch and tag history, including generated release commits, merges, cherry-picks, and amended commits. Never create or retain commits under another person, automation account, or bot identity.
- Do not add `Co-authored-by` trailers or commit-message attribution identifying another author. Do not introduce the previous personal identity into repository files, commit metadata, or release metadata.
- Before each commit or push, run `GITLAB_DESTINATION=gitlab node .gitlab/scripts/identity.cjs` to verify GitLab authentication as `johninnld`, effective Git identity and all reachable history. Verify `git var GIT_AUTHOR_IDENT` and `git var GIT_COMMITTER_IDENT`, including environment overrides. Local API authentication reads `~/.ci` (or an explicit private token-file override); never log credentials.
- GitLab.com is the release publisher under `johninnld/nl-morphe-patches`. Local Git pushes use the verified GitLab SSH identity named by private configuration; local releases use `~/.ci`, while the optional hosted fallback uses protected/masked `JOHNINNL_GITLAB_TOKEN` owned by `johninnld`. Retain the exact johninnl Git metadata above and the existing Morphe signing identity. Never substitute a bot token or deploy key.
- GitLab remains the source/version authority. The user's later request restores Codeberg and GitGud release mirrors: publish the exact same signed MPP and canonical branch/tag commit to both in parallel. Use separate destination-owned credentials; never send GitLab credentials to another forge. Do not run independent version selection, rewrite per-forge source metadata or force conflicting refs. Hosted forge Actions remain opt-in.
- Keep Dependabot version-update configuration disabled. Update dependencies under the required identity; security alerts may remain enabled.
- Treat these as mandatory requirements, not optional preferences. Preserve existing work when repairing identity history; do not silently bypass a failed identity check.

## Project workflow

- Publish stable releases only from `main`; `dev` publishes development prereleases. Promote unapproved app changes to `main` only after user confirmation. `.releaserc` and `.gitlab/gitlab-com.release.config.cjs` own release version selection and generated release files. Do not manually bump release versions.
- Preserve the source-based release-note and Morphe app-scope guards. Declare affected apps with `Release-Apps` when shared patch changes require it. Declare `Unchanged-Apps` only when a commit edits an app's files without changing what its patches produce, because every release bullet naming an app badges it for repatching in Morphe. A commit editing several apps' patch folders must declare each of them in one of the two trailers.
- Run `node --test tests/test_release_branding.cjs tests/test_preview_release.cjs tests/test_identity.cjs tests/test_codeberg.cjs tests/test_gitgud.cjs tests/test_gitlab.cjs tests/test_gitlab_publish.cjs` after release automation changes. Run `python3 -m unittest discover -s tests` for Python regression checks; use the release workflow's Gradle checks for Android changes.
- CI audits checked-out history; release automation also verifies the token owner and effective Git identities before publishing. These checks do not enforce local operations unless run, and cannot stop an already-authorized direct push before it reaches GitLab. Follow the identity requirements for every operation.

## Local CI and publishing rules

Operational details, private-file inventory and both channel procedures are in [PUBLISHING.md](PUBLISHING.md). Keep rules here and implementation/setup details there.

- Prefer local build/test/release automation. Hosted jobs must not run on ordinary branch, merge-request, main/dev or mirror pushes; the GitLab fallback requires explicit web opt-in through the `run_cloud_ci=true` pipeline input. Keep arbitrary pipeline-variable overrides disabled. Legacy forge workflows remain manual.
- Use `scripts/local-release/verify.cjs` as the single complete verification command set. Do not replace required release/history, Python, Android, Medium or bilingual documentation checks with a smaller passing subset.
- Maintain `scripts/local-release/templates.json`, `.releaserc`, `PUBLISHING.md` and both main/dev procedures together. `main` is stable; `dev` is the dev prerelease channel. Existing version/note/catalogue generation remains authoritative.
- Publish only approved main/dev heads. The local watcher baselines existing heads on first startup, then processes later approved updates automatically. Obtain user approval before promoting unapproved app changes to main; feature branches never publish.
- Verify publisher identity, all required secrets and the unchanged Morphe signing key before publication. API keys come from the private `~/.ci` store; SSH identities and the other existing signing/application secrets are referenced by private configuration. Never commit or log their values.
- Credential tests must use a fixture-only HOME/environment and temporary credential files. Never read the operator's real store in a test, and never use value-dumping assertions for credential comparisons; use fixed-message boolean checks.
- Build/sign once under GitLab source/version authority. Publish the exact canonical signed bytes to Codeberg and GitGud in parallel with separate credentials, immutable refs and per-forge verification. Never select independent versions, rewrite per-forge manifests or force conflicting history/tags.
- Every terminal build/run/worktree job, successful or failed, must execute local storage cleanup. Archive compact diagnostic receipts/logs first, prove owned processes are stopped, then remove only that job's owned workspace, build outputs, scratch files and per-run repository. Never clean an active job or the primary checkout.
- Incomplete or unprovable cleanup is a failure with `cleanupRequired`; do not claim the job finished cleanly. Retain compact retry state and evidence, not a completed failed build directory. Bound retained logs/archives; preserve only documented reusable caches and input artifacts.
- Remove release build storage only after all required publication/discovery checks have completed, or after archiving a terminal failure. Keep per-forge partial outcomes for idempotent recovery.
- Record the exact source SHA, channel, complete verification outcome, publication/discovery and cleanup results. A canceled/skipped cloud pipeline is not a passing check.
