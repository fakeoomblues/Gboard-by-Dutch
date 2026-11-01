# RainViewer validation

Target: **8.4.1 / 44084**, Android API 32 minimum. Validation performed on 2026-11-01.

- Original APKM SHA-256: `b38cf76df9235a277b8ec8fba064f67e24820ba2b962b12741986bcfdb89e91b`.
- Original base APK SHA-256: `f0449a4c5deae2273088259243a90bae9dc40842136415d99b5b1469792276c4`.
- Original signer SHA-256: `1ee1224e4ec7aa2214ae60f92f7e53fa2de148b12f7e9fa3e9c97b87844f4bcc`.
- Kotlin/native Android app; full bundle includes ABI/density/language splits. The test build merges the original complete APKM and retains arm64 libraries.

## Verified

- `:patches:buildAndroid :patches:test` passed.
- All four patches applied together and each applied independently with Morphe `--validate`; all five reports contain successful patching and rebuilding steps.
- `tests/verify_rainviewer.py` inspects the actual emitted DEX/resources. It rejects the unpatched input, checks the control boundaries and confirms unchanged entitlement calculations and PremiumWorker methods.
- Python regression suite: **413 tests, 45 skips, no failures**. The skips are existing optional fixture/device checks; they are not RainViewer runtime evidence.
- Patched APK installed successfully on a dedicated Android 16/API 36 arm64 emulator.
- Fresh setup reached the live radar map after location/notification choices and color setup, skipping policy/purchase onboarding and the automatic Google consent notice. The live radar tiles rendered.
- Purchase map hint and Settings offer cards were found during the walkthrough and added to the controls. The updated build keeps plan status `Free` and normal Settings entries without those offers.
- Final signed bundle was rebuilt and reapplied after the cached-startup change; all five application reports passed again. The final APK was installed, its app data cleared on the dedicated emulator, and a fresh full setup again reached the live map without the subscription or UMP pages.
- Playback animated the map and the sharing workflow created a local video without an intervening advertisement.
- The final fresh-start log has no AndroidRuntime, VerifyError, Crashlytics startup or Clarity entries. The error-free process trace observed weather/search/forecast, notification settings and Firebase Installations requests; it did not observe the removed optional sender endpoints in those exercised paths.
- Original comparison screen was inspected across all 19 feature rows. No paid features were unlocked for the audit.

## Evidence locations

Local analysis is retained under the primary checkout's `.analysis/rainviewer-8.4.1/`: original APKs, JADX and smali, patch reports/logs, build/test logs, comparison-screen layouts, emulator screenshots and process request trace. Proprietary decompiled source is not committed.

## Limits

The physical phone supplied a baseline screenshot of the original app. Its installed app/data were not replaced. Runtime acceptance applies to the dedicated emulator and the paths exercised, not every device, account or weather service. Firebase Messaging registration fails on this unsigned/unregistered emulator setup, so push delivery has not been verified. Manual premium purchase flows and paid API entitlement enforcement have not been tested. Stable publication requires the repository's app-approval workflow.
