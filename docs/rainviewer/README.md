# RainViewer

Source-branch support for **RainViewer 8.4.1 / 44084**, package `com.lucky_apps.RainViewer`, Android 12L/API 32 or later. Select the complete original APKM, including its ABI and resource splits. These changes have not been promoted to a stable release.

| Patch | Behavior |
| --- | --- |
| Hide ads | Stops main-map, forecast and sharing banners; post-play, post-premium and sharing interstitials; in-house promotions; chart promo blocks; and the map purchase hint. |
| Disable tracking | Includes Hide ads. Stops app/Firebase analytics, Clarity recording, Crashlytics/Sessions startup, performance metrics, CCT telemetry uploads, publisher session/paywall events and notification-open measurement. Removes advertising/referrer permissions and returns an opted-out advertising ID. |
| Skip consent screens | Includes Disable tracking. Skips the automatic Google UMP request and notice without granting or recording consent. |
| Skip subscription screens | Finishes the policy/purchase onboarding through the app's normal completion path, suppresses recurring subscription offers, automatic startup/web promotions (including cached notices) and Settings offer cards. Preserves the Free/paid plan status and user-initiated purchases. |

All patches include a minimal local Play-licensing compatibility change for re-signed builds. This APK has a licensing wrapper; it does not contain the virtualized callback system seen in some other PairIP apps.

The patches do not unlock premium features. See the [premium-feature audit](premium-audit.md), [tracking audit](tracking-audit.md), and [validation evidence](validation.md).

To reproduce patch application:

```bash
python3 scripts/validate_rainviewer.py ORIGINAL.apkm patches/build/libs/patches-VERSION.mpp morphe-desktop-all.jar --output /tmp/rainviewer-validation
```

After decoding the combined patched APK and the original base APK with apktool:

```bash
python3 tests/verify_rainviewer.py PATCHED_DECODED ORIGINAL_DECODED
```
