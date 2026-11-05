# Android Auto DHU profiles

1280x480 is the primary real-car viewport. DHU supports standard encoded resolutions; use 1280x720 with 240 total vertical margin and native cropmargins=true, yielding a 1280x480 presented viewport. This is native head-unit configuration, not post-processing of screenshots. Verify the requested margins, resulting view bounds and captured image dimensions during acceptance. DriveDeck uses a native 1280x480 surface. Other comparison profiles: 1280x720 and 800x480, each at 160dpi with touch and rotary controls enabled.

## Owned VM root and connectivity tools

The owner authorized rootAVD/avdroot and native curl setup on 2026-11-05.
The registered `native_warnings_ui_api34` AVD now uses the private image at
`.tools/android-root-tools/navigation-api34-image/`; its original config is
retained in the private tooling receipt. Another AVD uses the shared API-34
SDK image, which must remain untouched.

The verified working root path is `adb -s emulator-5590 root`, followed by
`wait-for-device`. Native `/system/bin/curl` survives a cold boot. It comes
from curl-flashable-android commit
`e5d4c9282777540e3ec6f7c42eee87c62c670b42`, ARM64 binary SHA-256
`c78079c0239f0a6c44aa7e9180f97d4c3d175495d1ccf565a8854abd15f68b60`.
The local avdroot v0.1.1 executable is retained under `.tools/android-root-tools/`.
Magisk did not verify after its cold boot; the private ramdisk was restored
and the added manager/environment removed. Do not claim Magisk root is active.

Native curl returned certificate-verified HTTPS responses for Google (204),
Waze's website (200) and YouTube Music's website (200). These prove general
guest HTTPS access, not app API authentication, per-app policy or playback.
YouTube Music's native UI showed its sign-in requirement during the follow-up
diagnostic. Keep Waze's car-login error unattributed until its own failure is
isolated. Do not disable TLS verification to obtain a successful check.

Frida server 17.23.3 was verified running as UID 0; CLI 17.9.11 attached to
YouTube Music with the existing generic observer hooks. Keep the existing
explicit server/forward lifecycle, unload scripts and stop the server at the
end of a session. No new hooks or observers were added for this setup.

Receipts and rollback inputs are in the ignored
`.analysis/android-auto-acceptance/rooted-tooling-setup/` directory. Preserve
the configured private image and tool inputs when cleaning completed jobs.
This is a root/tooling baseline; genuine-AA consent and app acceptance have
not been saved as a reusable initialized baseline.
