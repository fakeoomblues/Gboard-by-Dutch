# Deezer for Android TV: patch implementation

Implements the mapping in [2026-11-08-deezer-tv-mapping.md](2026-11-08-deezer-tv-mapping.md)
for `deezer.android.tv` 4.4.2 (build `6251102`) as five patches in
`patches/src/main/kotlin/patches/deezertv/`.

Target: package `deezer.android.tv`, version `4.4.2`, versionCode `6251102`,
minSdk 23, APKM, signer `51a891a6c5d17b4e36a6f46bab675c1cae786a5af06f51d0105dab7ec6a94516`.

## Files

| File | Contents |
| --- | --- |
| `Compatibility.kt` | `deezerTv` compatibility (TV form factor, APKM, arm64 + armv7 targets) and `originalInput` |
| `Bytecode.kt` | signature-based `exact()`, `superOnCreateIndex()`, `closeAfterSuperOnCreate()`, `redirectAfterSuperOnCreate()` using only p-registers |
| `FullStreamingPatch.kt` | licence feature/ad/time/active verdicts and the per-track rights helper |
| `RemoveAdsPatch.kt` | empty ad configuration, banner gate, audio-ad launcher guard, video-ad activity close |
| `DisableTrackingPatch.kt` | Adjust/Segment/Braze initializers and event paths, manifest flags, permissions, providers |
| `RemovePremiumUiPatch.kt` | profile Subscribe button, subscription/conversion bricks, `info_subscribe` route |
| `RemovePaywallsPatch.kt` | premium activities, onboarding/artist paywall, trial/offers dialogs, `premiumtab` WebView |
| `CarHostPatch.kt` | media-browser trust verdict bypass + `QUERY_ALL_PACKAGES` |

## Hooks (dex descriptors)

Full streaming (`Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII;`):

- `llIlllIlllIIllI($IIlIIlIIIIIlII)Z` keeps every feature bit except `PREVIEW` (0x20)
  and `DEVICE_LIMITATION` (0x40).
- `IlIIIlIIlIIIIll($IlIllllIlIIlllll)Z` returns false (no ad entitlement).
- `lIIllIlllllllI()I` returns 0 (no streaming limit).
- `IllIIlllIIllIIII()Z` returns true (licence active).
- `LllIIlIIIIIlllII;->IIIIlIlIIlllIll(LIlIIllIIIIlIIII;Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII;J)Z`
  returns true (per-track rights).

Advertising:

- `Lcom/deezer/feature/ads/config/model/AdsConfigDataModel;->getConfig()/getSections()`
  return empty `HashMap`s.
- `LIIlIIlIlllIlllI;->IIIIlIlIIlllIll()Z` returns false (Google banner gate).
- `LIIlIIllIIIlIlII;->IIIllllIIlllll(Landroid/content/Intent;)V` returns before
  writing the `ad` extra; `AudioAdsActivity` then takes its existing no-ad path.
- `VideoAdsActivity.onCreate` closes after `super.onCreate`.

Tracking:

- `LIllllllIIIlIIIl;->IIIllllIIlllll(Landroid/content/Context;)V` (Adjust) returns.
- `LIlllIIIlIllIIIII;->IIIllllIIlllll()V` (Segment setup + Braze destination) returns.
- `LIlllIIIlIllIIIII;->IlIllllIlIIlllll(LIlIlIlIlIllIIIIl;)V` (events/screens) returns.
- `LIlllIIIlIllIIIII;->IIlIIlIIIIIlII(String,String,ZZ)V` (identify) returns.
- Manifest: Firebase Analytics/Crashlytics/Performance/Sessions and Google Analytics
  flags off, Facebook auto-events and ad-ID collection off, six advertising/attribution
  permissions removed, Adjust and Facebook init providers disabled.

Premium/paywalls:

- `tv_fragment_user_settings.xml`: `@id/action_subscribe` hidden and zero-height.
- `brick__subscription.xml` and `brick__conversion_banner.xml`: hidden and zero-height.
- `TvInfoMessageActivity.onCreate` finishes when `contentId == "info_subscribe"`.
- `NativePremiumActivity.onCreate` redirects to `TabBarActivity` (task-root safety).
- `PremiumTabActivity`, `NativePremiumTabActivity`, `ArtistPaywallActivity`,
  `OnboardingPaywallActivity`, `UserOffersDialogActivity`, `TrialStartActivity` close
  after `super.onCreate`.
- `WebViewDialogActivity.onCreate` closes when its `url` contains `premiumtab`.

Car host:

- `LIllllllIIlIlIll;->IlIllllIlIIlllll(ILandroid/os/Bundle;Ljava/lang/String;)LllIIIIll$IIIllllIIlllll;`
  keeps the media-session browse tree and skips the trust branch: the
  `if-eqz trust, :null` before `const/4 root, 0` is replaced with `nop`, so the root
  is built for any caller.
- `androidAutoHostVisibility` from `patches.shared` adds `QUERY_ALL_PACKAGES` so the
  service can resolve a host that Android 11 hides.

## Plan label: investigated, no TV surface

The TV profile header (`UserProfileHeaderView.setData`) renders
`DeezerProfile.getProfileTypeDescriptionText` — the profile type ("main"/"family
member"/"kids"), not a subscription plan. The only plan strings in the TV build are
`dz_deezerplans_title_deezerpremium_mobile` and
`dz_androidtv_text_needoffer…`, both used exclusively by the `info_subscribe` page
(`IllIllllIlllIlI`) that this patch now closes. There is therefore no free-plan label
to rewrite; the phone's "show free as Premium" hook has no TV equivalent.

## Verification (2026-11-08)

Build and compile:

```
./gradlew :patches:compileKotlin   # ok
./gradlew :patches:build --rerun-tasks -x test   # ok
./gradlew :patches:test            # ok (exit 0)
```

Real patch run (Morphe CLI 2.0.0, Java 27):

```
morphe patch -p patches/build/libs/patches-1.55.0.mpp \
  -o $TMPDIR/re/deezertv/patched.apk --unsigned <original .apkm>
```

Output:

```
INFO: Applying 6 patches...
INFO: Applied: Allow any Android Auto host (TV)
INFO: Applied: Disable tracking (TV)
INFO: Applied: Enable full streaming (TV)
INFO: Applied: Remove advertising (TV)
INFO: Applied: Remove premium upsell UI (TV)
INFO: Applied: Remove subscription paywalls (TV)
INFO: Patching took 2.2 seconds
```

Artifact checks (`aapt2` + `dexdump` on the patched APK):

- Classes rewritten into one new dex, exactly the intended classes (family activities
  untouched).
- `AdsConfigDataModel.getConfig()` / `getSections()` now construct and return an empty
  `HashMap`.
- Licence feature method reads the enum bit, rejects 0x20 and 0x40, returns true
  otherwise; time limit returns 0; active returns 1; ad-kind returns 0.
- `VideoAdsActivity.onCreate`: `invoke-super` then `finish()` then `return-void`.
- `TvInfoMessageActivity.onCreate`: `contentId == "info_subscribe"` → `finish()`
  (guarded for a null extra so a `smart_page_id` launch keeps working).
- `NativePremiumActivity.onCreate`: super, then `Intent(TabBarActivity)` with
  `0x14000000`, `startActivity`, `finish`.
- Adjust `IIIllllIIlllll` starts with `return-void`; Segment setup keeps the Analytics
  instance (removing it crashed) but its Braze init call and Braze destination
  registration are `nop`, every event/identify/`$default` entry returns early, and
  `SettingsKt.fetchSettings` returns null.
- Manifest: the six ad/attribution permissions are gone; `firebase_analytics_collection_enabled=false`,
  `firebase_sessions_enabled=false`, Crashlytics/Performance flags present; both init
  providers have `android:enabled=false`; `QUERY_ALL_PACKAGES` added.
- Car admission: the trust branch at `0137` is now `nop` and the root is built from the
  media-session tree for any package.
- Layouts: `action_subscribe` has `visibility=gone` and `layout_height=0dp`; both bricks
  have `visibility=gone` and `0dp` height.

### Runtime (Android TV API 36 emulator)

Created a `tv_1080p` / `system-images;android-36;android-tv;arm64-v8a` AVD, installed
the patched APK signed with a scratch key, and launched
`deezer.android.tv/com.deezer.android.ui.activity.LauncherActivity`.

```
topResumedActivity=deezer.android.tv/...authentication.AuthenticationActivity
FATAL EXCEPTION count: 0
Adjust not initialized: 5 log lines
media session: deezer.android.tv/DeezerJukebox/12
```

Screenshots: `$TMPDIR/re/deezertv/tv-final.png` (landing screen with "Log in") and
`tv-login.png` (login form). This proves the patched dex loads, the leanback UI renders
and Adjust never initializes.

Runtime found and fixed two real defects:

1. No-op'ing Segment's `setup()` left `IlIIIIIllIl`'s `lateinit` Analytics empty and
   crashed at startup (`UninitializedPropertyAccessException`). Setup is kept; only its
   inputs (events, identify, Braze destination, Braze init) are disabled.
2. Removing the Crashlytics registrar crashed `DZMidlet` with "FirebaseCrashlytics
   component is not present". Registrars stay; collection is disabled by flags only.

Tracking after the final pass:

```
fatal=0  braze_enable=0  braze_net=0  segment=0
topResumedActivity=...authentication.AuthenticationActivity
```

- Braze never enables (`Setting SDK to enabled` count 0) and never enables outbound
  requests; the destination is not registered and the identity tracker is inert.
- Segment performs no `api.segment.io` request (count 0); all event entry points are
  inert.
- An earlier attempt to skip Deezer's whole Braze initializer crashed with
  `media router service must only be accessed on the application's main thread`; only
  `enableSdk` is patched now, which keeps the object graph intact.

Car-browser probe:

- A client APK (`com.morphe.tvprobe`, androidx.media 1.7.0, not in the allow-list) binds
  to `DeezerMediaBrowserService`. Without `<queries>` the bind fails (Android 11 package
  visibility); with it the bind succeeds and the service logs
  `No root for client com.morphe.tvprobe`, so the client gets `onConnectionFailed`.
  The trust branch is bypassed in the dex, but the media-session tree yields no root for
  a logged-out app. A meaningful root test needs a signed-in session.

Login-gated screens (profile tab, playback, ads) were not exercised: no Deezer
credentials are available in this environment.

## Not ported

- **Family management**: `FamilyManagementActivity` / `FamilyPickerActivity` are left
  intact so a paying family admin can still manage profiles.

## Remaining gate

Playback, ad-free playback and the post-login leanback screens need a signed-in TV
session. A Deezer test account (or a valid ARL) is required for that acceptance pass;
everything up to and including the authentication screen is verified.
