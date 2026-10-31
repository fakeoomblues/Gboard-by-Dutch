# Contributing

Prepare changes for `main`. Use conventional commit messages such as
`feat(app): ...`, `fix(app): ...` and `docs: ...`. Release automation publishes
production bundles from `main`. All current app targets belong to that release.
Do not manually publish versions or edit generated release metadata.

Keep each patch restricted to its declared app versions and form factor. Fail
when a required match is absent or ambiguous. Keep independent controls
independent and preserve functionality outside their documented scope.

## Build and verify

Use JDK 21 and the Android SDK. Run `python3 scripts/build.py buildAndroid` to
use the Gradle wrapper with an optional local `.pat` for GitHub Packages. The
build requires `SHARED_MAPS_API_KEY`; Bright article builds also use
`BRIGHT_ARTICLE_SIGNING_KEY`. Store credentials in the environment or the
ignored files used by the build, never in source control.

Run `python3 -m unittest discover -s tests` and
`node --test tests/test_release_branding.cjs tests/test_preview_release.cjs`.
For changed apps, use the corresponding verifiers and target validation in
`scripts/validate_targets.py`. `scripts/validate.py` covers Buienalarm only.
Report compilation, patch application, static verification and device checks
separately. A successful build does not prove runtime behaviour.

Install `docs/site-requirements.txt`, run `python3 scripts/generate_docs_catalog.py`,
`python3 scripts/check_markdown_links.py`, `python3 -m mkdocs build --strict`
and `python3 scripts/check_docs_site.py` to validate documentation.

## Write for users

Keep Dutch and English app pages aligned. Describe each patch's visible effect,
default, supported versions and useful limitations. Named advertising and
tracking services may be described in detail. Leave implementation mechanics,
reverse-engineering instructions and technical measurement records out of the
user guide. Keep technical verification in developer source and tests.

Regenerate the public catalogue and README summary after changing patch
metadata. The source metadata may contain developer details; public summaries
must use the app guides' end-user explanations.

## Repository setup

Follow the mandatory identity rules in [AGENTS.md](AGENTS.md). Activate
`johninnl` with `gh auth switch --user johninnl` before working. Every author,
committer and release publisher must use that identity.

Configure GitHub Packages access and the build secrets before enabling
releases. Set `JOHNINNL_RELEASE_TOKEN` to a token owned by `johninnl`; checkout
and publishing require it. Use `MORPHE_SIGNING_KEY` for bundle signing and
retain its public key. Keep the map and Bright build inputs in their Actions
secrets. Enable GitHub Pages with Actions. This repository uses a single
production branch, `main`.

The first release creates `patches-bundle.json`; no download manifest is
supplied for an artifact that has not been published. Start `main` with the
prepared source snapshot, including the complete current patch set. Do not
import old branches or tags.

Open issues and pull requests at
https://github.com/johninnl/nl-morphe-patches. Include the app version, selected
patches and reproducible steps. Keep original APKs, credentials, signing keys,
private app data and downloaded tools out of Git.
