<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/e64bd6a82ac198aaa855541162c721334c1ef735/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.46.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [e64bd6a82ac1](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/e64bd6a82ac198aaa855541162c721334c1ef735). Check each app's target versions in the user guide before patching.

---

## [1.46.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.45.0...v1.46.0) (2026-11-06)

### 🐛 Bug Fixes

* **ci:** select the current bundle and vendor jar without requiring a clean ([9c558db](https://gitlab.com/johninnld/nl-morphe-patches/commit/9c558db198dc3fcdb21bba28d8035680ab6602d6))
* **release:** mirror only the published canonical bundle ([a2e5251](https://gitlab.com/johninnld/nl-morphe-patches/commit/a2e5251f5a056e3e9632493f41d4aeff80aa29e6))
* **release:** sweep pending workspace cleanup on watch ticks ([762d8d4](https://gitlab.com/johninnld/nl-morphe-patches/commit/762d8d40db1b83cedc207b85ff8ec509c61ae90a))
* **release:** verify discovery against the release commit and retry stale CDN reads ([7eef795](https://gitlab.com/johninnld/nl-morphe-patches/commit/7eef7957925e5471396e15a573e73d0976cb4d22))

### ✨ New Features

* **Gboard:** port six editing and clipboard patches to 18.5.11 ([e64bd6a](https://gitlab.com/johninnld/nl-morphe-patches/commit/e64bd6a82ac198aaa855541162c721334c1ef735))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/21072359acc8f109a7fd835cec7770f12362deda/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.45.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [21072359acc8](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/21072359acc8f109a7fd835cec7770f12362deda). Check each app's target versions in the user guide before patching.

---

## [1.45.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.44.0...v1.45.0) (2026-11-06)

### ✨ New Features

* **EasyPark NL:** enable third-party Android Auto hosts ([2107235](https://gitlab.com/johninnld/nl-morphe-patches/commit/21072359acc8f109a7fd835cec7770f12362deda))
* **EasyPark:** enable third-party Android Auto hosts ([2107235](https://gitlab.com/johninnld/nl-morphe-patches/commit/21072359acc8f109a7fd835cec7770f12362deda))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/9f5c31db276a643c68592571d51e0ff009bee28b/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.44.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [9f5c31db276a](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/9f5c31db276a643c68592571d51e0ff009bee28b). Check each app's target versions in the user guide before patching.

---

## [1.44.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.43.3...v1.44.0) (2026-11-05)

### 🐛 Bug Fixes

* **Flitsmeister:** prevent expired provider sessions restarting ([182689e](https://gitlab.com/johninnld/nl-morphe-patches/commit/182689e6fc2ddd642618c23055c69e84f4b7600f))
* **Flitsmeister:** stop capture-fault snapshot resync loops ([b88566d](https://gitlab.com/johninnld/nl-morphe-patches/commit/b88566d037bb5d727d1b49b2b7a320a92093dc64))
* **General:** deliver native AA routing fixes with experimental external alert limits ([7807790](https://gitlab.com/johninnld/nl-morphe-patches/commit/78077903ab0344ad5d2eb1db30e95aa702ea2081))
* **Google Maps:** preserve native car initialization state ([831866c](https://gitlab.com/johninnld/nl-morphe-patches/commit/831866c89d9461f31b9eaecfabb79b5f6adba337))
* **Google Maps:** preserve native search actions and pan callbacks ([16a00d8](https://gitlab.com/johninnld/nl-morphe-patches/commit/16a00d820fd36578a4fe778ed133b100da3272cc))
* **Google Maps:** prevent expired provider sessions restarting ([182689e](https://gitlab.com/johninnld/nl-morphe-patches/commit/182689e6fc2ddd642618c23055c69e84f4b7600f))
* **Google Maps:** restore native guidance after car reconnect ([5d590e3](https://gitlab.com/johninnld/nl-morphe-patches/commit/5d590e3d2d9875f84e891a269b004ae1a776d6cb))
* **Google Maps:** retain native navigation ownership across car detach ([e822209](https://gitlab.com/johninnld/nl-morphe-patches/commit/e822209fe6b38ccefcee51f63b4113ea9e4f13fa))
* **Google Maps:** retain the active native search editor ([15f2785](https://gitlab.com/johninnld/nl-morphe-patches/commit/15f2785da8c7a36a882512e727e4070dd6132c9f))
* **Google Maps:** reuse native speed and limit displays ([c39cbfa](https://gitlab.com/johninnld/nl-morphe-patches/commit/c39cbfaedbfa980d5db556da53ef9939e95fc56a))
* **Google Maps:** stop capture-fault snapshot resync loops ([b88566d](https://gitlab.com/johninnld/nl-morphe-patches/commit/b88566d037bb5d727d1b49b2b7a320a92093dc64))
* **release:** keep documentation trailers out of patch update badges ([9f5c31d](https://gitlab.com/johninnld/nl-morphe-patches/commit/9f5c31db276a643c68592571d51e0ff009bee28b))
* **Vela Maps:** prevent expired provider sessions restarting ([182689e](https://gitlab.com/johninnld/nl-morphe-patches/commit/182689e6fc2ddd642618c23055c69e84f4b7600f))
* **Vela Maps:** stop capture-fault snapshot resync loops ([b88566d](https://gitlab.com/johninnld/nl-morphe-patches/commit/b88566d037bb5d727d1b49b2b7a320a92093dc64))
* **Waze:** avoid repeated clears for display-only alerts ([2110052](https://gitlab.com/johninnld/nl-morphe-patches/commit/2110052642f4087bc0cc277d245015d6a3f92bfa))
* **Waze:** prevent expired provider sessions restarting ([182689e](https://gitlab.com/johninnld/nl-morphe-patches/commit/182689e6fc2ddd642618c23055c69e84f4b7600f))
* **Waze:** retain background broadcasts when car sessions end ([74d6b24](https://gitlab.com/johninnld/nl-morphe-patches/commit/74d6b24baf5cec5ab7139e2aeab3df76176ff97c))
* **Waze:** scope broadcast keepalive to car teardown ([9f89c95](https://gitlab.com/johninnld/nl-morphe-patches/commit/9f89c9528fc9424f07973502503d0e08978b2b68))
* **Waze:** stop capture-fault snapshot resync loops ([b88566d](https://gitlab.com/johninnld/nl-morphe-patches/commit/b88566d037bb5d727d1b49b2b7a320a92093dc64))

### ✨ New Features

* **Flitsmeister:** stop API-owned providers after missed heartbeats ([c5a7aa9](https://gitlab.com/johninnld/nl-morphe-patches/commit/c5a7aa9eedc64850f260ba66a5424f595524fa31))
* **Google Maps:** own native car UI and restore warning speech ([d6c614f](https://gitlab.com/johninnld/nl-morphe-patches/commit/d6c614fe8a7859ddddd3568d219d2eb00a8f8d65))
* **Google Maps:** relay native road warnings to Maps and Vela ([f6bff5a](https://gitlab.com/johninnld/nl-morphe-patches/commit/f6bff5aa3dde02f851b76088e7462f978978cc33))
* **Google Maps:** stop API-owned providers after missed heartbeats ([c5a7aa9](https://gitlab.com/johninnld/nl-morphe-patches/commit/c5a7aa9eedc64850f260ba66a5424f595524fa31))
* **Vela Maps:** relay native road warnings to Maps and Vela ([f6bff5a](https://gitlab.com/johninnld/nl-morphe-patches/commit/f6bff5aa3dde02f851b76088e7462f978978cc33))
* **Vela Maps:** stop API-owned providers after missed heartbeats ([c5a7aa9](https://gitlab.com/johninnld/nl-morphe-patches/commit/c5a7aa9eedc64850f260ba66a5424f595524fa31))
* **Waze:** relay native road warnings to Maps and Vela ([f6bff5a](https://gitlab.com/johninnld/nl-morphe-patches/commit/f6bff5aa3dde02f851b76088e7462f978978cc33))
* **Waze:** stop API-owned providers after missed heartbeats ([c5a7aa9](https://gitlab.com/johninnld/nl-morphe-patches/commit/c5a7aa9eedc64850f260ba66a5424f595524fa31))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/a111071267b760be0ee063825bd911b000cb10d1/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.43.3.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [a111071267b7](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/a111071267b760be0ee063825bd911b000cb10d1). Check each app's target versions in the user guide before patching.

---

## [1.43.3](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.43.2...v1.43.3) (2026-11-05)

### 🐛 Bug Fixes

* **AD:** present the Nieuws feed like the other news patches ([a111071](https://gitlab.com/johninnld/nl-morphe-patches/commit/a111071267b760be0ee063825bd911b000cb10d1))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/a3b108741807b7f02aa4dc26f984d3ceb6fbb951/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.43.2.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [a3b108741807](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/a3b108741807b7f02aa4dc26f984d3ceb6fbb951). Check each app's target versions in the user guide before patching.

---

## [1.43.2](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.43.1...v1.43.2) (2026-11-05)

### 🐛 Bug Fixes

* **AD:** keep the row builder's declared return type through the styling hook ([a3b1087](https://gitlab.com/johninnld/nl-morphe-patches/commit/a3b108741807b7f02aa4dc26f984d3ceb6fbb951))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/3a5bd57fc759c805f447a5bf579828bef0835468/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.43.1.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [3a5bd57fc759](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/3a5bd57fc759c805f447a5bf579828bef0835468). Check each app's target versions in the user guide before patching.

---

## [1.43.1](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.43.0...v1.43.1) (2026-11-05)

### 🐛 Bug Fixes

* **AD:** render the Nieuws shelf as titled headline-led cards ([3a5bd57](https://gitlab.com/johninnld/nl-morphe-patches/commit/3a5bd57fc759c805f447a5bf579828bef0835468))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/ebd57d1ec04c16716f371933bb53da8c69e9394d/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.43.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [ebd57d1ec04c](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/ebd57d1ec04c16716f371933bb53da8c69e9394d). Check each app's target versions in the user guide before patching.

---

## [1.43.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.42.0...v1.43.0) (2026-11-05)

### 🐛 Bug Fixes

* **AD:** render Nieuws as native headline-led cards with article artwork ([8554500](https://gitlab.com/johninnld/nl-morphe-patches/commit/8554500ce9a22b5ebfa388834c5562d3a59e8e01))

### ✨ New Features

* **Gboard:** add on-device Parakeet voice typing ([ebd57d1](https://gitlab.com/johninnld/nl-morphe-patches/commit/ebd57d1ec04c16716f371933bb53da8c69e9394d))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/19778ddbc04cf63741a9ffe27b6e074a0536d418/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.42.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [19778ddbc04c](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/19778ddbc04cf63741a9ffe27b6e074a0536d418). Check each app's target versions in the user guide before patching.

---

## [1.42.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.41.0...v1.42.0) (2026-11-05)

### ✨ New Features

* **AD:** add the Android Auto news feed ([19778dd](https://gitlab.com/johninnld/nl-morphe-patches/commit/19778ddbc04cf63741a9ffe27b6e074a0536d418))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/1a60424cf5b1b58bd7963400b16cf63d2f759af0/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.41.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [1a60424cf5b1](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/1a60424cf5b1b58bd7963400b16cf63d2f759af0). Check each app's target versions in the user guide before patching.

---

## [1.41.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.40.2...v1.41.0) (2026-11-05)

### ✨ New Features

* **Parool:** add the Android Auto news feed ([ef4bd4b](https://gitlab.com/johninnld/nl-morphe-patches/commit/ef4bd4bb6cfdb924753656d8cc56adea9ba6002d))
* **Trouw:** add the Android Auto news feed ([ef4bd4b](https://gitlab.com/johninnld/nl-morphe-patches/commit/ef4bd4bb6cfdb924753656d8cc56adea9ba6002d))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/2c1a0460b1f82161e3bf9b32645d8e905ad5d04b/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.40.2.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [2c1a0460b1f8](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/2c1a0460b1f82161e3bf9b32645d8e905ad5d04b). Check each app's target versions in the user guide before patching.

---

## [1.40.2](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.40.1...v1.40.2) (2026-11-05)

### 🐛 Bug Fixes

* **ci:** retain bounded workspace cleanup failure reasons ([2c1a046](https://gitlab.com/johninnld/nl-morphe-patches/commit/2c1a0460b1f82161e3bf9b32645d8e905ad5d04b))
* **SoundCloud:** restore car Home and publish Up next ([5632b32](https://gitlab.com/johninnld/nl-morphe-patches/commit/5632b328122c92e5507fcc9e47d62151f3e5b0c9))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/fa55a77890f9778790d0f6b3ec22e3d62d400331/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.40.1.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [fa55a77890f9](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/fa55a77890f9778790d0f6b3ec22e3d62d400331). Check each app's target versions in the user guide before patching.

---

## [1.40.1](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.40.0...v1.40.1) (2026-11-05)

### 🐛 Bug Fixes

* **Volkskrant:** continue the ordered news feed from each article ([fa55a77](https://gitlab.com/johninnld/nl-morphe-patches/commit/fa55a77890f9778790d0f6b3ec22e3d62d400331))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/95962baaf1cbed4fcb489a0df3c86d3545a17e4f/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.40.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [95962baaf1cb](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/95962baaf1cbed4fcb489a0df3c86d3545a17e4f). Check each app's target versions in the user guide before patching.

---

## [1.40.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.39.0...v1.40.0) (2026-11-05)

### 🐛 Bug Fixes

* **ci:** avoid blocking macOS mount scans during release cleanup ([957efa1](https://gitlab.com/johninnld/nl-morphe-patches/commit/957efa1d092811a238149aacbf0a85bd63a52be5))
* **ci:** reuse owned-process proof for Pages cleanup ([95962ba](https://gitlab.com/johninnld/nl-morphe-patches/commit/95962baaf1cbed4fcb489a0df3c86d3545a17e4f))

### ✨ New Features

* **Myspoti:** publish Android Auto Up next queue ([138d983](https://gitlab.com/johninnld/nl-morphe-patches/commit/138d9834f2358af7f0a726d64f93968f6b120a66))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/e4c60ea3fe894cdc4c2609ce7b4cfe471fea32e3/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.39.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [e4c60ea3fe89](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/e4c60ea3fe894cdc4c2609ce7b4cfe471fea32e3). Check each app's target versions in the user guide before patching.

---

## [1.39.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.38.0...v1.39.0) (2026-11-05)

### ✨ New Features

* **Volkskrant:** add the complete Android Auto news feed ([e4c60ea](https://gitlab.com/johninnld/nl-morphe-patches/commit/e4c60ea3fe894cdc4c2609ce7b4cfe471fea32e3))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/17bd68809623f6531c74a530b663535fc37f701d/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.38.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [17bd68809623](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/17bd68809623f6531c74a530b663535fc37f701d). Check each app's target versions in the user guide before patching.

---

## [1.38.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.37.0...v1.38.0) (2026-11-04)

### ✨ New Features

* **Flitsmeister:** relay native Flitsmeister warnings through Maps and Vela ([17bd688](https://gitlab.com/johninnld/nl-morphe-patches/commit/17bd68809623f6531c74a530b663535fc37f701d))
* **Google Maps:** relay native Flitsmeister warnings through Maps and Vela ([17bd688](https://gitlab.com/johninnld/nl-morphe-patches/commit/17bd68809623f6531c74a530b663535fc37f701d))
* **Vela Maps:** relay native Flitsmeister warnings through Maps and Vela ([17bd688](https://gitlab.com/johninnld/nl-morphe-patches/commit/17bd68809623f6531c74a530b663535fc37f701d))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/9fc16cb9858be417a82d7838db61754883e81b3e/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.37.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [9fc16cb9858b](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/9fc16cb9858be417a82d7838db61754883e81b3e). Check each app's target versions in the user guide before patching.

---

## [1.37.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.36.1...v1.37.0) (2026-11-04)

### ✨ New Features

* **Waze:** change the app name and launcher icon together ([bc4dcf0](https://gitlab.com/johninnld/nl-morphe-patches/commit/bc4dcf024aef06c951cf5c477872054bedd3b343))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/692c93c4cec0556d98c93967ebc23ee9b2480045/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.36.1.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [692c93c4cec0](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/692c93c4cec0556d98c93967ebc23ee9b2480045). Check each app's target versions in the user guide before patching.

---

## [1.36.1](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.36.0...v1.36.1) (2026-11-04)

### 🐛 Bug Fixes

* **Google Maps:** expose movable controls through standard Android Auto actions ([692c93c](https://gitlab.com/johninnld/nl-morphe-patches/commit/692c93c4cec0556d98c93967ebc23ee9b2480045))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/eadbb826d0abc3af8c29f6424b2ebf23dd369b34/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.36.0.**

[Source metadata](https://gitlab.com/johninnld/nl-morphe-patches/-/raw/main/patches-bundle.json) · [User guide](https://gitlab.com/johninnld/nl-morphe-patches/-/blob/main/docs/site/en/index.md)

Source: [eadbb826d0ab](https://gitlab.com/johninnld/nl-morphe-patches/-/commit/eadbb826d0abc3af8c29f6424b2ebf23dd369b34). Check each app's target versions in the user guide before patching.

---

## [1.36.0](https://gitlab.com/johninnld/nl-morphe-patches/compare/v1.35.1...v1.36.0) (2026-11-03)

### 🐛 Bug Fixes

* **ci:** add GitGud release mirroring and native build tools ([333872f](https://gitlab.com/johninnld/nl-morphe-patches/commit/333872f62b2dd9abf2de50d2a480bf080995fcd7))
* **ci:** install supported Android SDK packages on Forgejo ([dc18b9c](https://gitlab.com/johninnld/nl-morphe-patches/commit/dc18b9cb12bd6de892cc891b8114341e4d04b078))
* **ci:** split Forgejo release gates into bounded jobs ([bade99a](https://gitlab.com/johninnld/nl-morphe-patches/commit/bade99a08588eec05493615b40369b663e5bc381))
* **ci:** support signed release mirrors on GitLab.com ([14d181b](https://gitlab.com/johninnld/nl-morphe-patches/commit/14d181b5e22fbfba8433bc3f44b091beeef4ff2f))
* **ci:** use Python-compatible GitLab build image ([59a4c21](https://gitlab.com/johninnld/nl-morphe-patches/commit/59a4c21938f385965b305c5b98b5dfa221493fdf))
* **release:** move publishing and source discovery to Codeberg ([36b615a](https://gitlab.com/johninnld/nl-morphe-patches/commit/36b615a695d7929275e5afa7b0c5aef4ead669cd))
* **release:** publish signed Morphe bundles on GitLab ([eadbb82](https://gitlab.com/johninnld/nl-morphe-patches/commit/eadbb826d0abc3af8c29f6424b2ebf23dd369b34))

### ✨ New Features

* **Flitsmeister:** support version 15.0 with restored runtime callbacks ([0acd087](https://gitlab.com/johninnld/nl-morphe-patches/commit/0acd087c13a49de1e12f9f2ec7d92bdca12354f6))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/4c9028dc425ffe7cc9c141a5d263ef6996cdae13/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.35.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [4c9028dc425f](https://codeberg.org/JohninNL/nl-morphe-patches/commit/4c9028dc425ffe7cc9c141a5d263ef6996cdae13). Check each app's target versions in the user guide before patching.

---

## [1.35.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.35.0...v1.35.1) (2026-11-02)

### 🐛 Bug Fixes

* **Google Maps:** keep custom icons visible at small sizes ([4c9028d](https://codeberg.org/JohninNL/nl-morphe-patches/commit/4c9028dc425ffe7cc9c141a5d263ef6996cdae13))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/ea77d2ac29498a959da2168f85527f5367ff9205/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.35.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [ea77d2ac2949](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ea77d2ac29498a959da2168f85527f5367ff9205). Check each app's target versions in the user guide before patching.

---

## [1.35.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.34.0...v1.35.0) (2026-11-02)

### ✨ New Features

* **Google Maps:** add light and dark launcher branding ([ea77d2a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ea77d2ac29498a959da2168f85527f5367ff9205))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/a4d6f09ee9af37f0a57efeddd87ddd7da3670c9d/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.34.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [a4d6f09ee9af](https://codeberg.org/JohninNL/nl-morphe-patches/commit/a4d6f09ee9af37f0a57efeddd87ddd7da3670c9d). Check each app's target versions in the user guide before patching.

---

## [1.34.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.33.3...v1.34.0) (2026-11-02)

### ✨ New Features

* **Myspoti:** add custom app name and six launcher icons ([a4d6f09](https://codeberg.org/JohninNL/nl-morphe-patches/commit/a4d6f09ee9af37f0a57efeddd87ddd7da3670c9d))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/07967a2b26f03d82afe3fcf6c6ca7fbf7336749f/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.33.3.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [07967a2b26f0](https://codeberg.org/JohninNL/nl-morphe-patches/commit/07967a2b26f03d82afe3fcf6c6ca7fbf7336749f). Check each app's target versions in the user guide before patching.

---

## [1.33.3](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.33.2...v1.33.3) (2026-11-02)

### 🐛 Bug Fixes

* **WuPlay Mobile:** keep video full width beneath floating audio panels ([508eaad](https://codeberg.org/JohninNL/nl-morphe-patches/commit/508eaad873dc172e4c334fc4dde6c0e01ecd7ef9))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/63d986f85170e51d511e3cc296880c4b46bcdf69/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.33.2.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [63d986f85170](https://codeberg.org/JohninNL/nl-morphe-patches/commit/63d986f85170e51d511e3cc296880c4b46bcdf69). Check each app's target versions in the user guide before patching.

---

## [1.33.2](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.33.1...v1.33.2) (2026-11-02)

### 🐛 Bug Fixes

* **2dehands:** accept Manager resource API qualifiers ([63d986f](https://codeberg.org/JohninNL/nl-morphe-patches/commit/63d986f85170e51d511e3cc296880c4b46bcdf69))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/efbe2d6c3d64d63a7424753c46233a5ad99ca44d/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.33.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [efbe2d6c3d64](https://codeberg.org/JohninNL/nl-morphe-patches/commit/efbe2d6c3d64d63a7424753c46233a5ad99ca44d). Check each app's target versions in the user guide before patching.

---

## [1.33.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.33.0...v1.33.1) (2026-11-02)

### 🐛 Bug Fixes

* **2dehands:** accept device-specific native and density splits ([efbe2d6](https://codeberg.org/JohninNL/nl-morphe-patches/commit/efbe2d6c3d64d63a7424753c46233a5ad99ca44d))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/002a241d5542c69196cab4173beec64ce80ab048/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.33.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [002a241d5542](https://codeberg.org/JohninNL/nl-morphe-patches/commit/002a241d5542c69196cab4173beec64ce80ab048). Check each app's target versions in the user guide before patching.

---

## [1.33.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.32.0...v1.33.0) (2026-11-02)

### ✨ New Features

* **SoundCloud:** port analytics, consent and local Go controls ([002a241](https://codeberg.org/JohninNL/nl-morphe-patches/commit/002a241d5542c69196cab4173beec64ce80ab048))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/ec8f3f6e43f21d49fa01cfdc95907d8edbcb82dd/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.32.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [ec8f3f6e43f2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ec8f3f6e43f21d49fa01cfdc95907d8edbcb82dd). Check each app's target versions in the user guide before patching.

---

## [1.32.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.31.1...v1.32.0) (2026-11-02)

### ✨ New Features

* **SoundCloud:** show the phone Home in Android Auto ([ec8f3f6](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ec8f3f6e43f21d49fa01cfdc95907d8edbcb82dd))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/46992eee656ccf621bdb1a9685520297738f6dda/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.31.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [46992eee656c](https://codeberg.org/JohninNL/nl-morphe-patches/commit/46992eee656ccf621bdb1a9685520297738f6dda). Check each app's target versions in the user guide before patching.

---

## [1.31.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.31.0...v1.31.1) (2026-11-02)

### 🐛 Bug Fixes

* **General:** name DriveDeck in the trusted music host option ([46992ee](https://codeberg.org/JohninNL/nl-morphe-patches/commit/46992eee656ccf621bdb1a9685520297738f6dda))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/c49d2b4569742d7048b16b014b4309c5f98a7e6a/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.31.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [c49d2b456974](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c49d2b4569742d7048b16b014b4309c5f98a7e6a). Check each app's target versions in the user guide before patching.

---

## [1.31.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.30.1...v1.31.0) (2026-11-02)

### ✨ New Features

* **Flitsmeister:** accept DriveDeck as a car and music host ([c49d2b4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c49d2b4569742d7048b16b014b4309c5f98a7e6a))
* **Google Maps:** accept DriveDeck as a car and music host ([c49d2b4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c49d2b4569742d7048b16b014b4309c5f98a7e6a))
* **Myspoti:** accept DriveDeck as a car and music host ([c49d2b4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c49d2b4569742d7048b16b014b4309c5f98a7e6a))
* **Waze:** accept DriveDeck as a car and music host ([c49d2b4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c49d2b4569742d7048b16b014b4309c5f98a7e6a))
* **YouTube Music:** accept DriveDeck as a car and music host ([c49d2b4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c49d2b4569742d7048b16b014b4309c5f98a7e6a))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/57bc14881b26f8c9b21928b47a186eac931eb6d2/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.30.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [57bc14881b26](https://codeberg.org/JohninNL/nl-morphe-patches/commit/57bc14881b26f8c9b21928b47a186eac931eb6d2). Check each app's target versions in the user guide before patching.

---

## [1.30.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.30.0...v1.30.1) (2026-11-02)

### 🐛 Bug Fixes

* **WuPlay Mobile:** match configured Home and simplify DriveDeck browsing ([f56a768](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f56a76876e889849de4539eeb4f654ebb4775b1b))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/03fed3676fb2991d648d992d95d59e40bc4e819e/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.30.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [03fed3676fb2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/03fed3676fb2991d648d992d95d59e40bc4e819e). Check each app's target versions in the user guide before patching.

---

## [1.30.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.29.0...v1.30.0) (2026-11-02)

### ✨ New Features

* **Flitsmeister:** start navigation providers silently with managed audio ([0df2912](https://codeberg.org/JohninNL/nl-morphe-patches/commit/0df2912abb50d80062182cd28a94ae4a48c708fd))
* **Google Maps:** start navigation providers silently with managed audio ([0df2912](https://codeberg.org/JohninNL/nl-morphe-patches/commit/0df2912abb50d80062182cd28a94ae4a48c708fd))
* **Vela Maps:** start navigation providers silently with managed audio ([0df2912](https://codeberg.org/JohninNL/nl-morphe-patches/commit/0df2912abb50d80062182cd28a94ae4a48c708fd))
* **Waze:** start navigation providers silently with managed audio ([0df2912](https://codeberg.org/JohninNL/nl-morphe-patches/commit/0df2912abb50d80062182cd28a94ae4a48c708fd))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/8d0ff1bb142b4bed09396ddb3f0fa140cbc38dfa/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.29.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [8d0ff1bb142b](https://codeberg.org/JohninNL/nl-morphe-patches/commit/8d0ff1bb142b4bed09396ddb3f0fa140cbc38dfa). Check each app's target versions in the user guide before patching.

---

## [1.29.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.28.4...v1.29.0) (2026-11-02)

### ✨ New Features

* **WuPlay Mobile:** add Home shelves and capped autoplay ([009a765](https://codeberg.org/JohninNL/nl-morphe-patches/commit/009a765aaff2bb7f5c787ab359c30c51f0018617))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/354a820551073f959a36b367a9da1cff5f9a51dc/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.28.4.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [354a82055107](https://codeberg.org/JohninNL/nl-morphe-patches/commit/354a820551073f959a36b367a9da1cff5f9a51dc). Check each app's target versions in the user guide before patching.

---

## [1.28.4](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.28.3...v1.28.4) (2026-11-02)

### 🐛 Bug Fixes

* **Wuplay Android TV:** map configured Home rows and simplify channel setup ([aaecd95](https://codeberg.org/JohninNL/nl-morphe-patches/commit/aaecd95c5a00d008a61d9e29b564c78943c13755))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/3cb2a2d1fa27a92ed25da59567731fb87b59ce00/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.28.3.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [3cb2a2d1fa27](https://codeberg.org/JohninNL/nl-morphe-patches/commit/3cb2a2d1fa27a92ed25da59567731fb87b59ce00). Check each app's target versions in the user guide before patching.

---

## [1.28.3](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.28.2...v1.28.3) (2026-11-02)

### 🐛 Bug Fixes

* **WuPlay Mobile:** accept re-signed Gearslip hosts ([3cb2a2d](https://codeberg.org/JohninNL/nl-morphe-patches/commit/3cb2a2d1fa27a92ed25da59567731fb87b59ce00))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/ac5a2f89a2f9d1d7ef85811fe17d485cd8fca2e5/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.28.2.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [ac5a2f89a2f9](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ac5a2f89a2f9d1d7ef85811fe17d485cd8fca2e5). Check each app's target versions in the user guide before patching.

---

## [1.28.2](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.28.1...v1.28.2) (2026-11-02)

### 🐛 Bug Fixes

* **Gearslip:** enable native navigation in Gearslip and Android Auto ([ac5a2f8](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ac5a2f89a2f9d1d7ef85811fe17d485cd8fca2e5))
* **Google Maps:** enable native navigation in Gearslip and Android Auto ([ac5a2f8](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ac5a2f89a2f9d1d7ef85811fe17d485cd8fca2e5))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/2869b936cd8c0dad3645df3a21485c8f246b855b/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.28.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [2869b936cd8c](https://codeberg.org/JohninNL/nl-morphe-patches/commit/2869b936cd8c0dad3645df3a21485c8f246b855b). Check each app's target versions in the user guide before patching.

---

## [1.28.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.28.0...v1.28.1) (2026-11-02)

### 🐛 Bug Fixes

* **YouTube Music:** include personal Home and Library in the Any-version browser patch ([2869b93](https://codeberg.org/JohninNL/nl-morphe-patches/commit/2869b936cd8c0dad3645df3a21485c8f246b855b))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/b0d9dd619b90c3fef55f23d04a9bf724f2ab1b48/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.28.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [b0d9dd619b90](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b0d9dd619b90c3fef55f23d04a9bf724f2ab1b48). Check each app's target versions in the user guide before patching.

---

## [1.28.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.27.1...v1.28.0) (2026-11-02)

### ✨ New Features

* **WuPlay Mobile:** add native Gearslip video interface ([b0d9dd6](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b0d9dd619b90c3fef55f23d04a9bf724f2ab1b48))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/6705d1e9f2a36a80ee3bf77383b1daa5cd5a99be/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.27.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [6705d1e9f2a3](https://codeberg.org/JohninNL/nl-morphe-patches/commit/6705d1e9f2a36a80ee3bf77383b1daa5cd5a99be). Check each app's target versions in the user guide before patching.

---

## [1.27.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.27.0...v1.27.1) (2026-11-02)

### 🐛 Bug Fixes

* **Myspoti:** allow any structurally compatible app version ([6705d1e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/6705d1e9f2a36a80ee3bf77383b1daa5cd5a99be))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/bfdc032e1ee71fc160fc208d48f6aa96f4568e02/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.27.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [bfdc032e1ee7](https://codeberg.org/JohninNL/nl-morphe-patches/commit/bfdc032e1ee71fc160fc208d48f6aa96f4568e02). Check each app's target versions in the user guide before patching.

---

## [1.27.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.26.1...v1.27.0) (2026-11-02)

### ✨ New Features

* **Myspoti:** enable personal libraries in Gearslip ([bfdc032](https://codeberg.org/JohninNL/nl-morphe-patches/commit/bfdc032e1ee71fc160fc208d48f6aa96f4568e02))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/c50dcd8cdd23df060acba8e214d505b65d452eb6/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.26.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [c50dcd8cdd23](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c50dcd8cdd23df060acba8e214d505b65d452eb6). Check each app's target versions in the user guide before patching.

---

## [1.26.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.26.0...v1.26.1) (2026-11-02)

### 🐛 Bug Fixes

* **Google Maps:** retain the chosen app name with microG support ([c50dcd8](https://codeberg.org/JohninNL/nl-morphe-patches/commit/c50dcd8cdd23df060acba8e214d505b65d452eb6))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/df28ed2c3b64f0bc68edfa57869b63c18bc3d32f/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.26.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [df28ed2c3b64](https://codeberg.org/JohninNL/nl-morphe-patches/commit/df28ed2c3b64f0bc68edfa57869b63c18bc3d32f). Check each app's target versions in the user guide before patching.

---

## [1.26.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.25.0...v1.26.0) (2026-11-02)

### ✨ New Features

* **Gearslip:** enlarge car app text with adjustable display density ([df28ed2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/df28ed2c3b64f0bc68edfa57869b63c18bc3d32f))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/f57abb97c9eb11de5792dc50d66c3bab62d4489a/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.25.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [f57abb97c9eb](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f57abb97c9eb11de5792dc50d66c3bab62d4489a). Check each app's target versions in the user guide before patching.

---

## [1.25.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.24.1...v1.25.0) (2026-11-02)

### ✨ New Features

* **Google Maps:** add privacy controls and Waze/Flitsmeister road data ([f57abb9](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f57abb97c9eb11de5792dc50d66c3bab62d4489a))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/526551952672ca595ac3de4ae0d90d4dc39b62e5/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.24.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [526551952672](https://codeberg.org/JohninNL/nl-morphe-patches/commit/526551952672ca595ac3de4ae0d90d4dc39b62e5). Check each app's target versions in the user guide before patching.

---

## [1.24.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.24.0...v1.24.1) (2026-11-02)

### 🐛 Bug Fixes

* **RainViewer:** accept Android 10 input and enable all defaults ([5265519](https://codeberg.org/JohninNL/nl-morphe-patches/commit/526551952672ca595ac3de4ae0d90d4dc39b62e5))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/17b6366b4771c92594665fbd0f596ea33d058c3c/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.24.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [17b6366b4771](https://codeberg.org/JohninNL/nl-morphe-patches/commit/17b6366b4771c92594665fbd0f596ea33d058c3c). Check each app's target versions in the user guide before patching.

---

## [1.24.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.23.0...v1.24.0) (2026-11-02)

### ✨ New Features

* **RainViewer:** keep weather access and expose local Pro controls ([17b6366](https://codeberg.org/JohninNL/nl-morphe-patches/commit/17b6366b4771c92594665fbd0f596ea33d058c3c))
* **RainViewer:** remove ads and tracking and skip automatic offers ([a7ea2ca](https://codeberg.org/JohninNL/nl-morphe-patches/commit/a7ea2caed876f5f0586ff29021d5b59ec4ccad83))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/cf9acd2fe8597795be9e40409eaf415613ed93e5/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.23.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [cf9acd2fe859](https://codeberg.org/JohninNL/nl-morphe-patches/commit/cf9acd2fe8597795be9e40409eaf415613ed93e5). Check each app's target versions in the user guide before patching.

---

## [1.23.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.22.0...v1.23.0) (2026-11-01)

### 🐛 Bug Fixes

* **Vela Maps:** follow active car routes without the phone service ([cf9acd2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/cf9acd2fe8597795be9e40409eaf415613ed93e5))

### ✨ New Features

* **Flitsmeister:** share road alerts and zone approaches with third-party apps ([27f0205](https://codeberg.org/JohninNL/nl-morphe-patches/commit/27f0205b3ca015055bc007cafdafe61f6035782e))
* **Vela Maps:** integrate external speed and road warnings natively ([8e2a2a2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/8e2a2a22e0d04049602d96ce1a4de35c7cbe0617))
* **Waze:** share road alerts and zone approaches with third-party apps ([27f0205](https://codeberg.org/JohninNL/nl-morphe-patches/commit/27f0205b3ca015055bc007cafdafe61f6035782e))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/e8d866a4c447854d1030c84ab332c00f063aedd5/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.22.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [e8d866a4c447](https://codeberg.org/JohninNL/nl-morphe-patches/commit/e8d866a4c447854d1030c84ab332c00f063aedd5). Check each app's target versions in the user guide before patching.

---

## [1.22.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.21.0...v1.22.0) (2026-11-01)

### ✨ New Features

* **Waze:** add alert, privacy and display controls ([e8d866a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/e8d866a4c447854d1030c84ab332c00f063aedd5))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/dfb458d72dca25332e429af9a352e2469b817aee/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.21.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [dfb458d72dca](https://codeberg.org/JohninNL/nl-morphe-patches/commit/dfb458d72dca25332e429af9a352e2469b817aee). Check each app's target versions in the user guide before patching.

---

## [1.21.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.20.0...v1.21.0) (2026-11-01)

### ✨ New Features

* **Gearslip:** choose drawer apps from phone settings ([dfb458d](https://codeberg.org/JohninNL/nl-morphe-patches/commit/dfb458d72dca25332e429af9a352e2469b817aee))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/1ae705851644e42444d8e45f196d11ffd4c908ff/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.20.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [1ae705851644](https://codeberg.org/JohninNL/nl-morphe-patches/commit/1ae705851644e42444d8e45f196d11ffd4c908ff). Check each app's target versions in the user guide before patching.

---

## [1.20.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.19.2...v1.20.0) (2026-11-01)

### ✨ New Features

* **Waze:** add six optional driving controls ([1ae7058](https://codeberg.org/JohninNL/nl-morphe-patches/commit/1ae705851644e42444d8e45f196d11ffd4c908ff))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/99cd3e255f5f728f5ac8d162f563233682690014/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.19.2.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [99cd3e255f5f](https://codeberg.org/JohninNL/nl-morphe-patches/commit/99cd3e255f5f728f5ac8d162f563233682690014). Check each app's target versions in the user guide before patching.

---

## [1.19.2](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.19.1...v1.19.2) (2026-11-01)

### 🐛 Bug Fixes

* **Waze:** accept patched Gearslip as a car host ([99cd3e2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/99cd3e255f5f728f5ac8d162f563233682690014))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/2c920a152821ca4d54561096ad8419f625680a83/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.19.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [2c920a152821](https://codeberg.org/JohninNL/nl-morphe-patches/commit/2c920a152821ca4d54561096ad8419f625680a83). Check each app's target versions in the user guide before patching.

---

## [1.19.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.19.0...v1.19.1) (2026-11-01)

### 🐛 Bug Fixes

* **YouTube Music:** discover compatible Gearslip browser layouts across versions ([2c920a1](https://codeberg.org/JohninNL/nl-morphe-patches/commit/2c920a152821ca4d54561096ad8419f625680a83))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/1bb6679db1d6161ea28a78228379716bf4f52990/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.19.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [1bb6679db1d6](https://codeberg.org/JohninNL/nl-morphe-patches/commit/1bb6679db1d6161ea28a78228379716bf4f52990). Check each app's target versions in the user guide before patching.

---

## [1.19.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.18.0...v1.19.0) (2026-11-01)

### ✨ New Features

* **Gearslip:** retain music players and filter unwanted drawer apps ([4b1720d](https://codeberg.org/JohninNL/nl-morphe-patches/commit/4b1720d2b95f3b6e9353b97ddd26d0bfb74027e1))
* **YouTube Music:** verify Gearslip callers for music browsing ([1bb6679](https://codeberg.org/JohninNL/nl-morphe-patches/commit/1bb6679db1d6161ea28a78228379716bf4f52990))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/48bea6ad773f8d7bf5814c2db07ece3650894dfb/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.18.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [48bea6ad773f](https://codeberg.org/JohninNL/nl-morphe-patches/commit/48bea6ad773f8d7bf5814c2db07ece3650894dfb). Check each app's target versions in the user guide before patching.

---

## [1.18.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.17.0...v1.18.0) (2026-11-01)

### ✨ New Features

* **Flitsmeister:** broadcast navigation data to third-party apps ([48bea6a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/48bea6ad773f8d7bf5814c2db07ece3650894dfb))
* **Waze:** broadcast navigation data to third-party apps ([48bea6a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/48bea6ad773f8d7bf5814c2db07ece3650894dfb))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/d9b46b3c1df1710afd93f1709ebedbf5f0cad43e/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.17.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [d9b46b3c1df1](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d9b46b3c1df1710afd93f1709ebedbf5f0cad43e). Check each app's target versions in the user guide before patching.

---

## [1.17.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.16.0...v1.17.0) (2026-11-01)

### ✨ New Features

* **Waze:** broadcast background navigation data and support Gearslip ([d9b46b3](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d9b46b3c1df1710afd93f1709ebedbf5f0cad43e))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/828331cdd3b4f7d74c0b1306e2e4134b03b273ec/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.16.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [828331cdd3b4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/828331cdd3b4f7d74c0b1306e2e4134b03b273ec). Check each app's target versions in the user guide before patching.

---

## [1.16.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.15.1...v1.16.0) (2026-11-01)

### 🐛 Bug Fixes

* **2dehands:** complete 2dehands publication and shared Horizon scoping ([828331c](https://codeberg.org/JohninNL/nl-morphe-patches/commit/828331cdd3b4f7d74c0b1306e2e4134b03b273ec))

### ✨ New Features

* **2dehands:** add advertising and privacy controls ([2b57f43](https://codeberg.org/JohninNL/nl-morphe-patches/commit/2b57f434d3f818bb23a976b15cc6b20847d9bc7f))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/0906f8bfd4f4be5d584f82af356c8ac6bfd0c3c4/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.15.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [0906f8bfd4f4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/0906f8bfd4f4be5d584f82af356c8ac6bfd0c3c4). Check each app's target versions in the user guide before patching.

---

## [1.15.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.15.0...v1.15.1) (2026-11-01)

### 🐛 Bug Fixes

* **Flitsmeister:** hide the guest account sign-up card ([0906f8b](https://codeberg.org/JohninNL/nl-morphe-patches/commit/0906f8bfd4f4be5d584f82af356c8ac6bfd0c3c4))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/68b50783fd3d73aaedd4949028dd7269636baad6/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.15.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [68b50783fd3d](https://codeberg.org/JohninNL/nl-morphe-patches/commit/68b50783fd3d73aaedd4949028dd7269636baad6). Check each app's target versions in the user guide before patching.

---

## [1.15.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.14.0...v1.15.0) (2026-11-01)

### ✨ New Features

* **Flitsmeister:** allow onboarding without an account ([68b5078](https://codeberg.org/JohninNL/nl-morphe-patches/commit/68b50783fd3d73aaedd4949028dd7269636baad6))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/643e179fd308a1897b46f6afde176e884f7ab529/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.14.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [643e179fd308](https://codeberg.org/JohninNL/nl-morphe-patches/commit/643e179fd308a1897b46f6afde176e884f7ab529). Check each app's target versions in the user guide before patching.

---

## [1.14.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.13.0...v1.14.0) (2026-11-01)

### ✨ New Features

* **Waze:** add full patch catalog for 5.26.90.901 ([caba974](https://codeberg.org/JohninNL/nl-morphe-patches/commit/caba974ff21504d6411d9e3e1e43da2059eb66ee))
* **Waze:** answer consent silently and close privacy gaps ([643e179](https://codeberg.org/JohninNL/nl-morphe-patches/commit/643e179fd308a1897b46f6afde176e884f7ab529))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/d53c6e40d751d30594068142661b4de5ca9f65e3/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.13.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [d53c6e40d751](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d53c6e40d751d30594068142661b4de5ca9f65e3). Check each app's target versions in the user guide before patching.

---

## [1.13.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.12.0...v1.13.0) (2026-11-01)

### ✨ New Features

* **Wuplay Android TV:** choose Home catalog channels inside settings ([d53c6e4](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d53c6e40d751d30594068142661b4de5ca9f65e3))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/a522a256c65b0c572948006175c26648460084ee/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.12.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [a522a256c65b](https://codeberg.org/JohninNL/nl-morphe-patches/commit/a522a256c65b0c572948006175c26648460084ee). Check each app's target versions in the user guide before patching.

---

## [1.12.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.11.0...v1.12.0) (2026-11-01)

### ✨ New Features

* **Monet Android TV:** show individual Wuplay catalog rows ([a522a25](https://codeberg.org/JohninNL/nl-morphe-patches/commit/a522a256c65b0c572948006175c26648460084ee))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/f6d93736e5ed33070f1c58904c029d3a49cd92fc/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.11.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [f6d93736e5ed](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f6d93736e5ed33070f1c58904c029d3a49cd92fc). Check each app's target versions in the user guide before patching.

---

## [1.11.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.10.0...v1.11.0) (2026-11-01)

### ✨ New Features

* **Wuplay Android TV:** publish selected default-profile catalog channels ([f6d9373](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f6d93736e5ed33070f1c58904c029d3a49cd92fc))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/867e14fe0e449eb6dc7938a2caa6524ea37f3a54/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.10.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [867e14fe0e44](https://codeberg.org/JohninNL/nl-morphe-patches/commit/867e14fe0e449eb6dc7938a2caa6524ea37f3a54). Check each app's target versions in the user guide before patching.

---

## [1.10.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.9.0...v1.10.0) (2026-11-01)

### ✨ New Features

* **AT4K Launcher:** unlock premium on version 1.2 ([d544d2e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d544d2e970cecfdecd1ebe06c4cc3f5c090ddedb))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/b17fcabce88a394c5cd72b6a0a58e125f09c2c7c/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.9.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [b17fcabce88a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b17fcabce88a394c5cd72b6a0a58e125f09c2c7c). Check each app's target versions in the user guide before patching.

---

## [1.9.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.8.0...v1.9.0) (2026-11-01)

### ✨ New Features

* **Projectivy Launcher:** add Unlock Premium for 4.73 ([b17fcab](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b17fcabce88a394c5cd72b6a0a58e125f09c2c7c))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/ed0e21c82cd14a27d7c52191ad5959b3ed5f8096/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.8.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [ed0e21c82cd1](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ed0e21c82cd14a27d7c52191ad5959b3ed5f8096). Check each app's target versions in the user guide before patching.

---

## [1.8.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.7.0...v1.8.0) (2026-11-01)

### ✨ New Features

* **Projectivy Launcher:** disable tracking on 4.73 ([ed0e21c](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ed0e21c82cd14a27d7c52191ad5959b3ed5f8096))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/062679e47e11ad97b7beaba6699367136071d237/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.7.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [062679e47e11](https://codeberg.org/JohninNL/nl-morphe-patches/commit/062679e47e11ad97b7beaba6699367136071d237). Check each app's target versions in the user guide before patching.

---

## [1.7.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.6.0...v1.7.0) (2026-11-01)

### ✨ New Features

* **Waze:** add SoundCloud Feed and mixes, not the personal Library ([062679e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/062679e47e11ad97b7beaba6699367136071d237))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/af8f6ee0ee8a7585c83f3287ace809d066168c67/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.6.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [af8f6ee0ee8a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/af8f6ee0ee8a7585c83f3287ace809d066168c67). Check each app's target versions in the user guide before patching.

---

## [1.6.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.5.0...v1.6.0) (2026-11-01)

### ✨ New Features

* **GVA:** remove newspaper navigation ([af8f6ee](https://codeberg.org/JohninNL/nl-morphe-patches/commit/af8f6ee0ee8a7585c83f3287ace809d066168c67))
* **HBVL:** remove newspaper navigation ([af8f6ee](https://codeberg.org/JohninNL/nl-morphe-patches/commit/af8f6ee0ee8a7585c83f3287ace809d066168c67))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/e321406a19aa15485c8b8b6c047662aa40c53d5e/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.5.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [e321406a19aa](https://codeberg.org/JohninNL/nl-morphe-patches/commit/e321406a19aa15485c8b8b6c047662aa40c53d5e). Check each app's target versions in the user guide before patching.

---

## [1.5.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.4.0...v1.5.0) (2026-11-01)

### ✨ New Features

* **Waze:** show YT Music Morphe Home content, not the personal Library ([e321406](https://codeberg.org/JohninNL/nl-morphe-patches/commit/e321406a19aa15485c8b8b6c047662aa40c53d5e))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/91739dd5b29f27b925e5a4c6133c41db9e5ad7c1/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.4.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [91739dd5b29f](https://codeberg.org/JohninNL/nl-morphe-patches/commit/91739dd5b29f27b925e5a4c6133c41db9e5ad7c1). Check each app's target versions in the user guide before patching.

---

## [1.4.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.3.1...v1.4.0) (2026-10-31)

### ✨ New Features

* **Rollerbank:** remove ads and enable En Route camera alerts ([91739dd](https://codeberg.org/JohninNL/nl-morphe-patches/commit/91739dd5b29f27b925e5a4c6133c41db9e5ad7c1))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/8d8dac5b361f94f293a8a22152928501f305e8ad/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.3.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [8d8dac5b361f](https://codeberg.org/JohninNL/nl-morphe-patches/commit/8d8dac5b361f94f293a8a22152928501f305e8ad). Check each app's target versions in the user guide before patching.

---

## [1.3.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.3.0...v1.3.1) (2026-10-31)

### 🐛 Bug Fixes

* **HLN:** hide empty listening placeholders ([8d8dac5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/8d8dac5b361f94f293a8a22152928501f305e8ad))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/ff9d59cfca1179648bd4e41ffdf138c34028a753/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.3.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [ff9d59cfca11](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ff9d59cfca1179648bd4e41ffdf138c34028a753). Check each app's target versions in the user guide before patching.

---

## [1.3.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.2.2...v1.3.0) (2026-10-31)

### ✨ New Features

* **De Tijd:** hide the Krant shortcut from Meer ([b49182e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b49182e891451a4477a7a959eb2995eb1f06735f))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/93a1c0518f8c54acef3793544b85fcff8c1607c3/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.2.2.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [93a1c0518f8c](https://codeberg.org/JohninNL/nl-morphe-patches/commit/93a1c0518f8c54acef3793544b85fcff8c1607c3). Check each app's target versions in the user guide before patching.

---

## [1.2.2](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.2.1...v1.2.2) (2026-10-31)

### 🐛 Bug Fixes

* **Marktplaats:** restore Notifications with complete APKS input ([93a1c05](https://codeberg.org/JohninNL/nl-morphe-patches/commit/93a1c0518f8c54acef3793544b85fcff8c1607c3))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/961ab4e3f7b026568bd912eb1c87cd414ee162e1/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.2.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [961ab4e3f7b0](https://codeberg.org/JohninNL/nl-morphe-patches/commit/961ab4e3f7b026568bd912eb1c87cd414ee162e1). Check each app's target versions in the user guide before patching.

---

## [1.2.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.2.0...v1.2.1) (2026-10-31)

### 🐛 Bug Fixes

* **ED:** hide empty listening placeholders ([961ab4e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/961ab4e3f7b026568bd912eb1c87cd414ee162e1))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/547199d3dc5a243598e989e0d8be39f8d311b30d/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.2.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [547199d3dc5a](https://codeberg.org/JohninNL/nl-morphe-patches/commit/547199d3dc5a243598e989e0d8be39f8d311b30d). Check each app's target versions in the user guide before patching.

---

## [1.2.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.1.0...v1.2.0) (2026-10-31)

### 🐛 Bug Fixes

* **AD:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **Buienalarm:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **Buienradar:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **ED:** remove embedded consent and vacancy promotions ([ee6c003](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ee6c003e11cdf67560a9fb1d9d8f5ab98510b419))
* **Marktplaats:** retire version 16.17.0 ([aaf7bc7](https://codeberg.org/JohninNL/nl-morphe-patches/commit/aaf7bc780ecb5e7972cd09c32194cd0638ef5f0e))
* **Medium:** include the isolated regex dependency ([93f46b1](https://codeberg.org/JohninNL/nl-morphe-patches/commit/93f46b185cf332cbd6de797a85431be511b0a2b9))
* **NPO Start Android TV:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **NPO Start:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **NU.nl:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **release:** advertise the dev bundle to Morphe sources ([b8c6719](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b8c6719b7d0aadb8b51075fed677f7c3daacaebe))
* **release:** exclude released changes from dev announcements ([fefcb2e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/fefcb2ec0d0e4d654c04e92d7e757051fc5abe59))
* **release:** publish dev bundles through Morphe discovery ([37ed1f6](https://codeberg.org/JohninNL/nl-morphe-patches/commit/37ed1f6f0f3eaff72972aa6823c6758932e29a07))
* **release:** scope Medium dependency changes to the reader ([68adbc2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/68adbc25431caae3b03aa5eec8fb4fb6869521bb))
* **RTL Nieuws:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))

### ✨ New Features

* **ED:** add GVA and ED sibling patch controls ([fe9f035](https://codeberg.org/JohninNL/nl-morphe-patches/commit/fe9f0355bcdbb8739459330be06272aae2b3a931))

### 🚀 Updated App Support

* **NRC:** support 5.12.25 build 627 ([ae2ea28](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ae2ea28064eae228f487ffd8c27da5980af971fc))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/f4641f52b622ab03e3fc9b5aac161f71c5d6352a/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.2.0-dev.2.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [f4641f52b622](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a). Check each app's target versions in the user guide before patching.

---

## [1.2.0-dev.2](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.2.0-dev.1...v1.2.0-dev.2) (2026-10-31)

### 🐛 Bug Fixes

* **AD:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **Buienalarm:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **Buienradar:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **Marktplaats:** retire version 16.17.0 ([aaf7bc7](https://codeberg.org/JohninNL/nl-morphe-patches/commit/aaf7bc780ecb5e7972cd09c32194cd0638ef5f0e))
* **Medium:** include the isolated regex dependency ([93f46b1](https://codeberg.org/JohninNL/nl-morphe-patches/commit/93f46b185cf332cbd6de797a85431be511b0a2b9))
* **NPO Start Android TV:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **NPO Start:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **NU.nl:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))
* **release:** scope Medium dependency changes to the reader ([68adbc2](https://codeberg.org/JohninNL/nl-morphe-patches/commit/68adbc25431caae3b03aa5eec8fb4fb6869521bb))
* **RTL Nieuws:** accept verified public Android APK variants ([f4641f5](https://codeberg.org/JohninNL/nl-morphe-patches/commit/f4641f52b622ab03e3fc9b5aac161f71c5d6352a))

### 🚀 Updated App Support

* **NRC:** support 5.12.25 build 627 ([ae2ea28](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ae2ea28064eae228f487ffd8c27da5980af971fc))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/fefcb2ec0d0e4d654c04e92d7e757051fc5abe59/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.2.0-dev.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [fefcb2ec0d0e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/fefcb2ec0d0e4d654c04e92d7e757051fc5abe59). Check each app's target versions in the user guide before patching.

---

## [1.2.0-dev.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.1.0...v1.2.0-dev.1) (2026-10-31)

### 🐛 Bug Fixes

* **ED:** remove embedded consent and vacancy promotions ([ee6c003](https://codeberg.org/JohninNL/nl-morphe-patches/commit/ee6c003e11cdf67560a9fb1d9d8f5ab98510b419))
* **release:** advertise the dev bundle to Morphe sources ([b8c6719](https://codeberg.org/JohninNL/nl-morphe-patches/commit/b8c6719b7d0aadb8b51075fed677f7c3daacaebe))
* **release:** exclude released changes from dev announcements ([fefcb2e](https://codeberg.org/JohninNL/nl-morphe-patches/commit/fefcb2ec0d0e4d654c04e92d7e757051fc5abe59))
* **release:** publish dev bundles through Morphe discovery ([37ed1f6](https://codeberg.org/JohninNL/nl-morphe-patches/commit/37ed1f6f0f3eaff72972aa6823c6758932e29a07))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/eb2db87b8deec9f04bc3e5cffd648d4c90690095/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.1.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [eb2db87b8dee](https://codeberg.org/JohninNL/nl-morphe-patches/commit/eb2db87b8deec9f04bc3e5cffd648d4c90690095). Check each app's target versions in the user guide before patching.

---

## [1.1.0](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.0.1...v1.1.0) (2026-10-31)

### ✨ New Features

* **GVA:** add approved GVA patch controls ([eb2db87](https://codeberg.org/JohninNL/nl-morphe-patches/commit/eb2db87b8deec9f04bc3e5cffd648d4c90690095))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/5156ac7ba8ff76c4e328b3df94ac7d6aceeffbc4/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.0.1.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [5156ac7ba8ff](https://codeberg.org/JohninNL/nl-morphe-patches/commit/5156ac7ba8ff76c4e328b3df94ac7d6aceeffbc4). Check each app's target versions in the user guide before patching.

---

## [1.0.1](https://codeberg.org/JohninNL/nl-morphe-patches/compare/v1.0.0...v1.0.1) (2026-10-31)

### 🚀 Updated App Support

* **9292:** support additional 9292 and Marktplaats APKM builds ([5156ac7](https://codeberg.org/JohninNL/nl-morphe-patches/commit/5156ac7ba8ff76c4e328b3df94ac7d6aceeffbc4))
* **Marktplaats:** support additional 9292 and Marktplaats APKM builds ([5156ac7](https://codeberg.org/JohninNL/nl-morphe-patches/commit/5156ac7ba8ff76c4e328b3df94ac7d6aceeffbc4))

<!-- dap-release-branding:v1 -->
![Dutch App Patches. Dutch apps. Your choices.](https://raw.githubusercontent.com/johninnl/nl-morphe-patches/d0ddc143a33db9e5a916bb5d674a7a73db0b1879/docs/site/assets/brand/release-banner.png)

**Release · patch bundle v1.0.0.**

[Add to Morphe](https://morphe.software/add-source?github=johninnl/nl-morphe-patches) · [User guide](https://johninnl.codeberg.page/nl-morphe-patches/en/)

Source: [d0ddc143a33d](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879). Check each app's target versions in the user guide before patching.

---

## 1.0.0 (2026-10-31)

### ✨ New Features

* **9292:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **AD:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Bright:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Buienalarm:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Buienradar:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **De Morgen:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **De Tijd:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **DS Nieuws:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Dumpert:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Flitsmeister:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **FTM:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **HBVL:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **HLN:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Humo:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **KIJK Android TV:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **KIJK Android:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Marktplaats:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Medium:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Nieuwsblad:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **NOS Teletekst:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **NPO Start Android TV:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **NPO Start:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **NRC:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **NU.nl:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Omroep Brabant:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Omroep West:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Parool:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Podimo:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **RTL Nieuws:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Trouw:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Tubantia:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **TVgids.nl:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Videoland Android TV:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Videoland Android:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Voetbal International:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Volkskrant:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Weeronline:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))
* **Weerplaza:** launch Dutch App Patches ([d0ddc14](https://codeberg.org/JohninNL/nl-morphe-patches/commit/d0ddc143a33db9e5a916bb5d674a7a73db0b1879))

# Changelog

This repository starts from an unreleased source snapshot. Its releases and
changelog will be generated here by semantic-release. Release history from the
previous repository is not carried over.
