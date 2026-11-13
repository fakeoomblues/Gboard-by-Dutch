# Deezer for Android TV 4.4.2: mapping for a patch port

Investigation date: 2026-11-08. Input:
`deezer.android.tv_4.4.2-6251102_2arch_1dpi_16lang_57782807d712433c5edb3b715307fac2_apkmirror.com.apkm`.

This is the mapping pass, not a port. Every claim is either **seen** (file/line or
string in the artifact) or **inferred** (reasoned from structure, not yet exercised
on a device).

## Build facts (seen)

| Fact | Value |
| --- | --- |
| Package | `deezer.android.tv` |
| Version | 4.4.2 (versionCode `6251102`) |
| Signer SHA-256 | `51a891a6c5d17b4e36a6f46bab675c1cae786a5af06f51d0105dab7ec6a94516` — the same Deezer signer as `deezer.android.app` 9.0.26.3 |
| minSdk / targetSdk | 23 / 35 |
| Split layout | `base.apk` (63 MB, 11 dex) + `split_config.{arm64_v8a,armeabi_v7a,xhdpi,16 langs}` |
| Flavor tag | Kotlin metadata `app_tvOfficialGooglePlayStoreRelease` on the TV classes inspected |
| Launchable | `com.deezer.android.ui.activity.LauncherActivity` (leanback + launcher) |
| Application | `deezer.android.app.DZMidlet` |

`deezer.android.tv` is a **separate flavor of the same Deezer monorepo**, not a
different product: it ships `com.deezer.core.*`, `com.deezer.feature.*`,
`com.deezer.ui.premium.*` and `com.deezer.ui.tabbar.*` next to its own
`com.deezer.android.tv.ui.*` leanback UI.

## Obfuscation shape

The artifact is R8-obfuscated like the phone build, but the mapping differs:

- Entry points declared in the manifest keep their names (`com.deezer.ui.premium.NativePremiumActivity`,
  `com.deezer.feature.artist.ArtistPaywallActivity`, `com.deezer.feature.ads.audio.ui.AudioAdsActivity`, …).
- Shared data models keep names when `@JsonCreator`/`@Metadata` forces them
  (`com.deezer.feature.ads.config.model.AdsConfigDataModel`, `com.deezer.core.data.model.policy.*`).
- The bulk of implementation classes are `defpackage.IIIII…`.
- Field and method names inside those classes are also obfuscated, so phone patch
  targets (e.g. `Lcom/deezer/core/data/model/policy/lIllllIIIIII;`) do not exist verbatim.

Consequence: the existing patches cannot be applied unchanged. They have to be
re-expressed against this mapping. The good news is that the *shapes* (classes,
enums, method signatures, resource ids) are the same.

## Cross-reference: current patches → TV equivalents

### 1. `Remove subscription paywalls`

| Existing hook (9.0.26.3) | TV 4.4.2 | Status |
|---|---|---|
| `NoFreemium` payment gate / `getNoFreemium…` | no `isWaitingForPayment` / `noFreemium` state machine found in the TV dex; only the unused `NoFreemiumData` DTO (`defpackage/lIlIlllllIllIII.java`, toString strings only) | **inferred not applicable** — the TV launcher is a dispatcher (`com.deezer.android.ui.activity.LauncherActivity`), and the authenticated TV UI is the leanback `TabBarActivity`; the phone's startup "waiting for payment" redirect is not visible in this build |
| `NativePremiumTabActivity` / `NativePremiumActivity` / `PremiumTabActivity` | same class names, declared in the manifest (lines 1040, 1106, 1114), intent factories in the obfuscated deep-link table | port the "finish after super.onCreate" pattern verbatim |
| `ArtistPaywallActivity`, `OnboardingPaywallActivity` | same class names, manifest lines 1167, 1170 | port `closeAfterSuperOnCreate` |
| `UserOffersDialogActivity`, `TrialStartActivity`, `FamilyManagementActivity/Picker` | all present in the TV manifest | straight port of the activity-level closes |
| `WebViewDialogActivity` `premiumtab` guard | `com.deezer.android.ui.activity.WebViewDialogActivity` present; TV also has `BridgeWebViewActivity` and `CustomTextPageActivity` | port, plus route "show_offers" / "offersautopromo" (see below) |
| TV-only upsell | `TvInfoMessageActivity` renders `info_subscribe` (`tv_fragment_info_subscribe.xml`, Closes with "Understand"); reachable from deeplink actions `show_offers` and `offersautopromo` (`defpackage/IlIlIllllIIIIlIl.java:3006,4627`) and from the Profile tab's Subscribe button | new TV-specific hook needed |

### 2. `Remove advertising`

| Existing hook | TV equivalent | Evidence |
|---|---|---|
| `AdsConfigDataModel.getConfig()` / `getSections()` returning empty maps | **same class, same getter names** | `com/deezer/feature/ads/config/model/AdsConfigDataModel.java:85,93` |
| banner section check | `defpackage/IIlIIlIlllIlllI.java:69` — `config.get("banner").getSections().contains(section)` | `config` map key `"banner"` |
| audio/preroll gate | `defpackage/IlIlIIllIlIIIIIl.java` — audio-ad controller holding `TritonAdContent`; gates on `config.get("audio").getStart()` | lines 138, 225, 274, 288 |
| preroll + SmartAdServer placement | `defpackage/IlIIlIllIlIll.java:101,116,141` builds `SASAdPlacement` from `sections["any"].smart` | SmartAdServer SDK present |
| home/feed ad sections | `defpackage/lIIlIIIIIIIIlll.java:305` — `sections.containsKey("home")` | |
| video ad activities | `com.deezer.feature.ads.audio.model.video.VideoAdsActivity` (manifest 1109) | no `VastVideoAdsActivity` in this build |
| Google ads | GAM/AdMob: manifest `com.google.android.gms.ads.APPLICATION_ID = ca-app-pub-3993860719449492~7658480358`, `brick__ad__google` layout, `AdManagerAdRequest` in `IIlIIlIlllIlllI` | seen |
| Triton audio ads | `com/deezer.feature.ads.audio.model.triton.{TritonAdContent,TritonAudioAd}` | seen |
| Substrata? | `libsubstrata-quickjs.so` and `com.segment.analytics.substrata.*` are the Segment JS engine, **not** an ad vendor | corrects a naive string match |

The `AdsConfigDataModel` shape is richer on TV (`profile`, `targeting`, `config`,
`sections` with typed `Config`/`Sections`), but the two getters the phone patch
empties are identical, so the same "no config → no placement" strategy works.

### 3. `Enable full streaming`

This is the cleanest port: the licence model is the same module with the same
bit values.

| Existing hook (9.0.26.3) | TV equivalent | Evidence |
|---|---|---|
| `IIIIlIIIIIIIIlll(FEATURE)Z` feature check | `llIlllIlllIIllI(EnumC0659IIlIIlIIIIIlII)Z` | `com/deezer/core/data/model/policy/IIlIIlIIIIIlII.java:339` |
| `llIIIllIlIllIII(AD_KIND)Z` ad entitlement | `IlIIIlIIlIIIIll(IlIllllIlIIlllll)Z` | `…/IIlIIlIIIIIlII.java:208` |
| `IllIIIlIIII()I` streaming time limit | `lIIllIlllllllI()I` | `…/IIlIIlIIIIIlII.java:332` |
| `IIlIIIIIlllIIIII()Z` licence active | `IllIIlllIIllIIII()Z` | `…/IIlIIlIIIIIlII.java:281` |
| per-track rights helper `IIIlllIIIllIIIl.IIIlIIlIIlllIl(Track, Licence, long)` | `defpackage/llIIlIIIIIlllII.IIIIlIlIIlllIll(IlIIllIIIIlIIII, IIlIIlIIIIIlII, long)` | `defpackage/llIIlIIIIIlllII.java:21` |

Enum values are identical: `MOD(1) OFFLINE(2) CHROMECAST(8) RADIO(16) PREVIEW(32)
DEVICE_LIMITATION(64) QUEUELIST_EDITION(128)` and `DISPLAY(1) AUDIO(2)`
(`…/IIlIIlIIIIIlII.java:66-84`). 81 classes consume the feature enum, including
`com/deezer/feature/player/content/PlayerContentLauncher.java` (playback launch,
per-track rights check at `…:692`), so the single licence object is the correct choke
point — the same reasoning that worked on the phone.

### 4. `Disable tracking`

| Existing hook | TV equivalent | Evidence |
|---|---|---|
| Adjust init | `defpackage/IllllllIIIlIIIl.IIIllllIIlllll(Context)` with token `3l5tgq88n5k9`; invoked from `DZMidlet.onCreate` | `IllllllIIIlIIIl.java:11-23`, `DZMidlet.java:788` |
| Segment delivery | `defpackage/IlllIIIlIllIIIII` builds Segment `Analytics` with write key `x0ZfvbWwBxjNLx46QAZoBEhMYHX8oNPB`, adds Braze destination, then `Braze.INSTANCE.getInstance` | `IlllIIIlIllIIIII.java:31-57`; wired at `DZMidlet.java:761` |
| Braze destination | same class; Braze endpoints `sdk.iad-01.braze.com` in strings | seen |
| Firebase Analytics tracker | `IlllIlIIllIllIlI` wraps `FirebaseAnalytics.getInstance(...)`; constructed in `DZMidlet.onCreate` | `DZMidlet.java:790-793` |
| Firebase Sessions / Crashlytics / Performance / Messaging | `com.google.firebase.sessions.SessionLifecycleService`, `FirebaseMessagingService`, Crashlytics classes | manifest + dex |
| Google Tag Manager | container asset `assets/containers/GTM-NC8442F.json`, `TagManagerService` in manifest | seen |
| Google Analytics (legacy) | `com.google.android.gms.analytics.AnalyticsService`, `CampaignTrackingService/Receiver` | manifest |
| Facebook | `com.facebook.sdk.*` metadata (AutoLogAppEvents=false already, ClientToken `8373724f…`), `FacebookInitProvider` | manifest |
| Qualtrics | `QualtricsSurveyActivity`, `SurveyActivity` deep link only | seen |
| Adjust preinstall + AD_ID permissions | `com.adjust.preinstall.READ_PERMISSION`, two `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_*`, `BIND_GET_INSTALL_REFERRER_SERVICE` | manifest |
| Deezer own telemetry | `com.deezer.core.analyticstracker{rec,segment,braze}provider`, `rec.deezer.com` | out of scope for "tracking" (product telemetry) but note it |

### 5. `Remove premium upsell UI`

The TV UI has no Premium tab: the tab bar is a fixed, code-built list.

| Tab id | Screen (seen in the tab factory) |
|---|---|
| 0 | Home — `channels/home/home-tv`, or `channels/kids` in kids mode |
| 1 | My Music — `tv_dynamic_page_masthead_my_music` |
| 2 | Explore — `channels/explore` |
| 3 | Search — `tv_dynamic_page_masthead_search` |
| 4 | Now playing (equalizer tab) |
| 5 | Profile / user settings (`lIlIlIllIIlI`, `tv_fragment_user_settings.xml`) |

seen in `TabBarActivity.java:139-162`, `defpackage/IIIIlllllIllI.java`,
`defpackage/IIllIIllIIIIIllI.java`, `defpackage/IIIIlllIIlllIlI.java`. Explore is
dropped when the app runs with the 5-entry tab array (`llIIIIllllllI.IlIlIllIIlIllll`).

The TV upsell surfaces are:

- Profile tab: "Subscribe" button (`action_subscribe` in `tv_fragment_user_settings.xml`,
  click handler index 1 in `lIlIlIllIIlI.java:56-72` → `info_subscribe`), plus the
  offer name in `UserProfileHeaderView`.
- `brick__subscription` — a dynamic-page upsell brick (`defpackage/lllIIIIIllIlllI.java:21`).
- `brick__conversion_banner` — the conversion banner brick.
- `layout_illustration_comparator_offer*`, `premium_tab_offer_card_item`,
  `settings_item_offer_co_branding` — shared offer layouts.
- Plan name strings: `dz_legacy_premium_text_deezerfree`, `dz_androidtv_text_youreenjoyingplan`,
  `dz_androidtv_text_needoffer…`.
- Onboarding paywall activity (TV manifest).

So the patch becomes: hide `brick_subscription` / conversion banner (data-driven
binder), drop the `info_subscribe` menu entry, rewrite the plan label. This is a
different implementation from the phone's obfuscated composables — do not port the
phone method bodies.

### 6. Car / Android TV client surfaces

The TV APK ships the same media-browser allow-list as the phone app:
`res/xml/allowed_media_browser_callers.xml` (with the `com.google.android.projection.gearhead`
certificate entries) and `com.deezer.feature.mediabrowser.DeezerMediaBrowserService`
(manifest line 1431). The "allow any Android Auto host" patch is therefore applicable
in principle (same resource + same car browser module), but it is not needed for pure
TV use and should only be considered if the TV build is used over Android Auto.

## TV-specific preservation notes

- **`--no-res` decompile omitted resources**; layouts/strings came from `apktool d`.
- **Face**: TV is 10-foot UI. Any UI hook must keep D-pad focus order; hiding a
  brick is safer than hiding a whole section.
- **Purchase flows**: TV has `com.android.billingclient` and `DeezerClubTabActivity`;
  blocking paywalls must not break the account/billing screens for users who do pay.
- **Splits**: patching has to handle an APKM with config splits; `AppTarget` needs
  `ARM64_V8A` (and optionally `ARMEABI_V7A`) entries with versionCode `6251102`.

## Verdict

Portable, in increasing effort:

1. **Full streaming** — high confidence, direct re-expression of the same licence hooks.
2. **Advertising** — high confidence: the gateway ad-config model and both getters are
   identical, and every TV placement (GAM brick, SmartAdServer preroll, Triton audio)
   reads from that config.
3. **Tracking** — high confidence: Adjust, Segment+Braze and Firebase all start from
   `DZMidlet.onCreate`, so a small number of init hooks covers them; manifest metadata
   and permissions are the same family as the phone app.
4. **Paywalls / premium UI** — medium confidence: activities are shared and can be
   closed the same way, but the TV UI's upsell entry points are different (Settings tab
   button, subscription/conversion bricks, `info_subscribe`), so this needs TV-specific
   hooks rather than a port of the phone composables.

## Reproduction

```sh
D="$TMPDIR/re/deezertv"
mkdir -p "$D/apkm" && cd "$D/apkm"
unzip -o deezer.android.tv_*.apkm
jadx -d "$D/jadx" --show-bad-code --no-res --threads-count 8 base.apk
apktool d --no-src -f -o "$D/apktool" base.apk
aapt2 dump xmltree --file AndroidManifest.xml base.apk > "$D/manifest.txt"
aapt2 dump resources base.apk > "$D/res.txt"
```

Anchor checks used above (all run against `$D/jadx/sources`):

```sh
rg -n 'EnumC0659IIlIIlIIIIIlII' com/deezer/core/data/model/policy
rg -n 'getConfig\(\)|getSections\(\)' com/deezer/feature/ads/config/model/AdsConfigDataModel.java
rg -n 'Adjust.initSdk|AndroidAnalyticsKt.Analytics' defpackage
rg -n 'info_subscribe' defpackage/IlIlIllllIIIIlIl.java
```

## Concrete port targets (descriptors for new fingerprints)

These are the class descriptors as verified by `dexdump`. Note that jadx renders
unpackaged smali classes as `defpackage.*`, and for at least one class its display
name differs from the real descriptor (`lllIIIIIllIlllI` in jadx vs
`LlllIIIIIllIlllI;` in dex). Always take the descriptor from the dex.

| Purpose | Descriptor |
| --- | --- |
| Licence class | `Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII;` |
| Licence feature check | `llIlllIlllIIllI(Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII$IIlIIlIIIIIlII;)Z` |
| Licence ad-kind check | `IlIIIlIIlIIIIll(Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII$IlIllllIlIIlllll;)Z` |
| Streaming time limit | `lIIllIlllllllI()I` |
| Licence active flag | `IllIIlllIIllIIII()Z` |
| Per-track rights verdict | `LllIIlIIIIIlllII;->IIIIlIlIIlllIll(LIlIIllIIIIlIIII;Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII;J)Z` |
| Ad configuration | `Lcom/deezer/feature/ads/config/model/AdsConfigDataModel;->getConfig()Ljava/util/HashMap;` and `getSections()Ljava/util/HashMap;` |
| Banner placement gate | `LIIlIIlIlllIlllI;->IIIIlIlIIlllIll()Z` |
| Adjust init | `LIllllllIIIlIIIl;->IIIllllIIlllll(Landroid/content/Context;)V` |
| Segment + Braze setup | `LIlllIIIlIllIIIII;->IIIllllIIlllll()V`, event path `IIlIIlIIIIIlII(Ljava/lang/String;Ljava/lang/String;ZZ)V` |
| Paywall activities | `Lcom/deezer/feature/artist/ArtistPaywallActivity;`, `Lcom/deezer/feature/onboarding/paywall/OnboardingPaywallActivity;`, `Lcom/deezer/ui/premium/{NativePremiumActivity,NativePremiumTabActivity,PremiumTabActivity};` — all `onCreate(Landroid/os/Bundle;)V` |
| TV subscribe screen | `Lcom/deezer/android/tv/ui/feature/info/TvInfoMessageActivity;->onCreate(Landroid/os/Bundle;)V` |
| Subscription brick binder | `LlllIIIIIllIlllI;->IIlIIlIIIIIlII()I` (returns `R$layout.brick__subscription`) |

All 17 anchors above were checked against the TV dex method table: 17/17 present.
The check is a simple `dexdump` pass over `classes*.dex`, matching owner, name and
prototype; no reliance on the decompiler's renaming.

Not every TV surface should be patched at the activity level: the two premium web
activities are the only ones guaranteed reachable through TV deeplinks, while the
profile "Subscribe" path is a local button and can be removed without touching
activities.

## Suggested batches (after approval)

1. **Licence + ads config** (no UI changes): full streaming hooks, empty ad config,
   banner gate. This is testable headlessly through the gateway and media calls.
2. **Tracking**: Adjust init, Segment/Braze setup, Firebase tracker wiring, manifest
   meta-data and permission removal (mirrors the phone resource patch, but the TV
   manifest has a different permission inventory: no `AD_SERVICES_CONFIG`, and the
   two `AD_ID` entries are present).
3. **TV premium UI**: profile Subscribe button, `info_subscribe` route, subscription
   and conversion bricks, plan label.
4. **Optional**: car-host allow-list if the TV build is used with Android Auto.

## Implementation status (2026-11-08)

Items 1–3 are implemented in `patches/src/main/kotlin/patches/deezertv/`, and item 4
(the Android Auto host) is implemented as a media-browser trust bypass with
`QUERY_ALL_PACKAGES`; see [2026-11-08-deezer-tv-implementation.md](2026-11-08-deezer-tv-implementation.md).
All six patches apply to the real APKM and the patched build runs on an Android TV
emulator through the authentication screen.

## Open questions

- The TV build's preview limit is enforced in the same licence object, but the actual
  media request is obfuscated (`com.deezer.core.media` has only `provider/` readable).
  A runtime check on a free account is required before claiming "full streaming"
  works on TV exactly as on the phone.
- `lIIllIlllllllI()` doubles as the Chromecast licence snapshot value
  (`defpackage/IlIIIIlIIIlIllI.java:31-45`); returning 0 there needs a runtime check
  that cast/remote playback still behaves.
- Free-account `NoFreemium` handling differs; the phone patch's startup-redirect
  workaround has no TV counterpart and should be dropped from the port, not copied.
