# GitGud mirror

Repository: https://gitgud.io/JohninNL/nl-morphe-patches

All original GitHub remote branches and tags are imported. The default branch is main; main and dev are protected. Build/signing secrets are masked and protected project variables. The GitGud publisher uses its own JohninNL token and the same exact Git author/committer metadata and Morphe signing identity.

`.gitlab-ci.yml` validates source and builds documentation/patch bundles. Codeberg alone creates release versions, commits and tags. Protected GitGud main/dev mirror those exact signed bytes into GitLab generic packages and release asset permalinks, without generating competing source commits or tags. The distribution branch carries independent main/dev JSON manifests; main/dev source branches remain identical to Codeberg. No Codeberg publishing token is copied to GitGud.

The instance's shared runner was stale/offline during migration. CI configuration can be validated, but pipeline execution requires an online runner; no personal workstation runner is installed. The canonical public Morphe source and guide remain on Codeberg. Anonymous GitGud raw-file requests currently encounter a Cloudflare challenge, so a GitGud metadata URL is not advertised as the primary Morphe source.

GitGud CI does not choose versions or alter main/dev/tag history. Codeberg pushes release heads and tags, then triggers the GitGud mirror pipeline explicitly because canonical release commits contain `[skip ci]`. Uploaded packages are compared byte for byte with the signed Codeberg artifact. The distribution metadata branch is separate from source history.
