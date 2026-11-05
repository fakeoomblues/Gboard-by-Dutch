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
A genuine-AA initialized baseline is now retained and verified as described
below. Full all-app acceptance remains incomplete.


## Initialized native AA baseline

The owned AVD retains original AA17 plus its matching splits, native automotive
role, accepted consent/developer settings, and the exact signed engineering
Maps/Waze/Vela/FM APKs. The private manifest is
`.analysis/android-auto-acceptance/reusable-aa17-native51/manifest.json`.
Original system-stub rollback bytes and the guest-derived permission allowlist
are retained beside it. Preserve the configured private image, current AVD
userdata and named `aa17_initialized_native51` snapshot during job cleanup.

The verified reuse path is a **cold boot retaining initialized userdata** with
`-no-snapshot`, followed by the normal AA wake below. Consent and Waze first-run
did not recur; native Waze Search/keyboard/results, route and admitted GPS
worked, and guidance survived DHU reconnects at 1280x480, 800x480 and 1280x720.
Run51's private review receipt records exact hashes, scope and cleanup. This
is not a full voice, mode, DPI, rotary or all-app acceptance claim.

Warm snapshot reload verified APK hashes/settings, but its first Frida attach
failed. Do not label warm runtime replay accepted. A cold boot also exposed
an AA car process waiting in `do_freezer_trap`: root alone does not thaw a
cached Android process. Before starting the existing observer, use ordinary
AA lifecycle entry points, then establish all required hooks before app test
stimuli:

```bash
adb -s emulator-5590 shell am start -W -a com.google.android.projection.gearhead.SETTINGS -p com.google.android.projection.gearhead
adb -s emulator-5590 shell am start-foreground-service -n com.google.android.projection.gearhead/.companion.DeveloperHeadUnitNetworkService
```

The observed wait changed to `futex_wait`; unchanged Frida observation then
attached successfully and retained coverage through native routing and both
reconnects. Do not force-stop AA again between this wake and attachment. Do not
disable the cached-app freezer globally, change SELinux, override native app
results or expand observers to work around startup. The existing observer
accepts only 240, 300 or 900 seconds; retain a separate overall stop deadline.
`-no-window` can select `qemu-system-aarch64-headless`, so verify the owned
AVD/serial and actual QEMU PID instead of assuming the non-headless executable.

At a terminal boundary, detach observation, stop the exact owned server,
remove forwards and token-scoped scratch, restore the initialized baseline's
APKs/settings/GPS and stop the VM. Preserve failed attachment receipts; only
the later fully covered app window supplies accepted behavior. No physical
phone was touched.
