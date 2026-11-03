# GitGud mirror

Repository: https://gitgud.io/JohninNL/nl-morphe-patches

All original GitHub remote branches and tags are imported. The default branch is main; main and dev are protected. Build/signing secrets are masked and protected project variables. The GitGud publisher uses its own JohninNL token and the same exact Git author/committer metadata and Morphe signing identity.

`.gitlab-ci.yml` validates source and builds documentation/patch bundles. Codeberg alone creates release versions, commits and tags. Protected GitGud main/dev mirror those exact signed bytes into GitLab generic packages and release asset permalinks, without generating competing source commits or tags. The distribution branch carries independent main/dev JSON manifests; main/dev source branches remain identical to Codeberg. No Codeberg publishing token is copied to GitGud.

The instance's shared runner was stale/offline during migration. CI configuration can be validated, but pipeline execution requires an online runner; no personal workstation runner is installed. The canonical public Morphe source and guide remain on Codeberg. Anonymous GitGud raw-file requests currently encounter a Cloudflare challenge, so a GitGud metadata URL is not advertised as the primary Morphe source.

GitGud CI does not choose versions or alter main/dev/tag history. Codeberg pushes release heads and tags, then triggers the GitGud mirror pipeline explicitly because canonical release commits contain `[skip ci]`. Uploaded packages are compared byte for byte with the signed Codeberg artifact. The distribution metadata branch is separate from source history.

## GitLab.com

The same pipeline supports https://gitlab.com/johninnld/nl-morphe-patches.
GitLab.com selects its own destination automatically from `CI_SERVER_HOST`;
local commands use `GITLAB_DESTINATION=gitlab`. Its publisher must authenticate
as `johninnld`, using `~/.gitlab` locally or the protected, masked
`JOHNINNL_GITLAB_TOKEN` variable in CI. The GitGud token is kept separate.

Import authoritative main/dev heads and tags, protect the source branches and
trigger a pipeline explicitly after importing a release commit with `[skip ci]`.
The pipeline validates and builds the source, then publishes the existing signed
Codeberg bundle to GitLab's generic package registry and release asset link.
The `distribution` branch holds channel metadata with GitLab download URLs.
No competing source version, release tag or signing identity is generated.
