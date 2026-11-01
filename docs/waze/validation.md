# Waze 5.26.90.901 validation

Validation date: 2026-11-01. Input: supplied APKMirror APKM, package `com.waze`, version code `1030746`, minimum SDK 32. The source catalog contains all 24 Waze patches in upstream commit `32e42e0f757581147307af27196e3aca517d0570` plus the existing two audio patches.

This records the initial 26-patch catalog. The subsequent 27-patch privacy follow-up is documented in the [privacy audit](privacy-audit.md).

## Build and artifact checks

- Original audio bundle: both audio patches applied unchanged with `--validate`, without forced compatibility.
- Complete catalog: all 26 applied together and individually, with no failed patches or failed patch/rebuild steps.
- Nondefault options: speed/unit/sign sizes, custom app name containing an ampersand, disabled black map, font size/background/arrow width, world speed sign, police distance and background timeout applied successfully from the original APKM input.
- Android build and three Robolectric sharing tests passed. The two disable tests first reproduced the imported heartbeat defect before its fix.
- Python regression suite: 393 tests, 39 skipped (unavailable optional fixtures), no failures.
- Godot parser on this APK: all three scenes found, with 2 + 3 + 35 = 40 scalable animation keys.

## Runtime checks

Installed the patched base with original arm64, xxhdpi, English and Dutch splits signed with one local test key on an API 36 arm64 phone emulator. Startup succeeded. Runtime logs confirmed configuration overrides, the patched speedometer binding, ten skin files rewritten, and forty pin scale keys rewritten. A second start wrote zero skin/pin values, confirming changes do not accumulate.

The dedicated emulator also opened the patched side menu and its Customization screen, including its switches and numeric values. Synthetic updates through the patched native-manager handlers exercised the average-speed zone colors and alert bridge. Synthetic camera approaches triggered speed-camera and Flock alerts at 300 m. These are synthetic checks, not road tests.

Both sync callbacks now dispatch native configuration writes through `NativeManager.Post`; final logs show the override method on the native thread. The sharing switch cancels heartbeats and clears cached alerts; Robolectric verifies it also blocks an in-flight update from restarting sharing.

All 15,077 numeric resource IDs were preserved. The patcher normalized 42 case-colliding resource names to ID-based aliases; resources accessed by the extension retain their names. Permission removal, base-only app labels, Firebase performance deactivation and custom size/name output were inspected. The final DEX has no duplicate class definitions; all three injected Waze method references resolve and 2,843 extension invokes have correct argument widths.

[validation.json](validation.json) records artifact hashes and check counts. Raw local evidence remains under `.analysis/waze-526/` and is not shipped with the patch source.

## Limits

These checks establish build compatibility, successful patch application and the runtime behavior explicitly listed above. They do not establish signed-in SoundCloud/YT Music playback, all media shelves, live navigation or road-camera alerts, Android Auto, live companion Maps reception, telemetry traffic suppression, or server-side mood acceptance. Report speed limit retains the upstream unverified label. Camera and zone self-tests use synthetic data and cannot establish road behavior.

The generated release tables and `patches-list.json` remain owned by release automation. The source catalog is documented separately until a bundle containing it is released.
