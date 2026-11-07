# Android Auto DHU profiles

1280x480 is the primary real-car viewport. DHU supports standard encoded resolutions; use 1280x720 with 240 total vertical margin and native cropmargins=true, yielding a 1280x480 presented viewport. This is native head-unit configuration, not post-processing of screenshots. Verify the requested margins, resulting view bounds and captured image dimensions during acceptance. DriveDeck uses a native 1280x480 surface. Other comparison profiles: 1280x720 and 800x480, each at 160dpi with touch and rotary controls enabled.

## Current availability (2026-11-07)

The old `native_warnings_ui_api34` registration is historical: its directory lacks
the saved configuration/userdata. Do not launch it or claim native51 is available.
The current goal-owned replacement is `nl_morphe_navigation_api34`, port5590,
under `~/.local/state/nl-morphe-patches/emulator-baselines/navigation-api34/`.
Its registration, full AVD disks/overlays, private system-image directory and
`inputs/privapp-native-aa17.xml` are reusable inputs outside the worktree.
Preserve the complete set, not only `system.img` or a registration file.

An inactive DriveDeck disk baseline was independently cloned with its owner's
permission; source hashes remained unchanged. Its logical disk chains were
normalized and compared before use. No initialized-app claim was inferred from
its old metadata. Preflight135 proved root, verified HTTPS and Frida inventory.
Disk normalization then changed the guest state: current network/Frida readiness
must be checked again before source stimuli. Provisioning136 proved root and
the installed package/role state after that change. Original signed AA17 was provisioned through the
recovered system-slot/unregister/reboot procedure, a guest-derived22-permission
allowlist and normal framework projection-role qualification. A normal update
of the same original APK set then succeeded. All four current patched source
APKs were installed and their device bytes verified before stopping the VM.
Private receipts: `.analysis/navigation-baseline-recovery-135` and
`.analysis/navigation-aa17-provision-136`.

Cold-boot persistence, normal first-run/developer setup, actual Java-hook coverage,
TTS readiness and navigation remain unverified. Do not call this an initialized
acceptance baseline until those have been saved and verified. All source apps
were left stopped; provisioning generated no GPS or warning stimuli.

Use the existing cold-boot recipe with the new AVD name, `-writable-system`,
`-no-snapshot`, `-no-window`, `-gpu host`, four cores and4096MB. Verify original
AA17 version179664044, native role, installed APK hashes and private disk paths
before source-app stimuli. Stop it between batches. The unchanged observer was
compiled from its tracked source using cached Java bridge7.0.13; exact source,
compiled artifact and tool hashes are in `observer-compile.json`/`tool-inputs.json`
under the recovery receipt. Process inventory is not Java hook coverage.

The owner also authorized normal phone checks without Frida. Phone app updates
must still use its existing Morphe signer/data; the engineering APK cache is for
the owned emulator. The newest local MPP is in phone Downloads, but its install
and phone DHU acceptance remain open while the phone handoff is pending.

For phone GPS replay, DHU's documented `location lat long accuracy altitude speed
bearing` command accepts speed in metres per second: 100 km/h is
27.777778 m/s. The profile must enable `location` under `[sensors]` or updates
are ignored. Sending a sensor command is not proof that either receiver or the
FM/Waze producer has accepted that fix. Verify native provider location/speed
before the bounded drive; only GPS may be simulated. Do not inject warnings or
substitute the head unit's separate vehicle-speed sensor for a genuine provider
GPS speed. See the [official DHU sensor guide](https://developer.android.com/training/cars/testing/dhu#sensors).

The remaining sections retain the prior setup and UI recipe as historical
recovery information. Preserve surviving inputs and exported evidence; do not
assume missing private manifests or scripts can be reused.

## Native audio capture candidate

The shipped ARM64 DHU (`4e42c054167d6a09a6cff04bba0b62d88898d39e866f241e7393750d2e23c163`)
contains SDL's `SDL_DISKAUDIOFILE` and `SDL_DISKAUDIOFILEIN` strings. A retained
REA query is in `.analysis/dhu-native-capabilities-115/strings.json`. This is
static capability evidence, not an accepted capture session.

SDL's existing disk audio backend can write output PCM to a file instead of a
speaker. For a future owned DHU session, evaluate the process-scoped settings
`SDL_AUDIODRIVER=disk` and `SDL_DISKAUDIOFILE=<owned-job>/dhu-audio.raw`; leave
the driver's pacing override unset. No observer or runner change is needed.
The [SDL implementation](https://github.com/libsdl-org/SDL/blob/SDL2/src/audio/disk/SDL_diskaudio.c)
documents the raw output and file selection; that current upstream source is
not proof of the SDK binary's exact SDL revision.

Before accepting evidence, confirm that the DHU selected the backend, retain
the negotiated PCM format and determine which output stream owns each file.
Correlate captured speech content with genuine navigation warnings and native
callbacks/logs. A nonempty raw file alone does not prove warning speech, and
this capture setup does not establish production end-to-end audio latency.

## Historical VM root and connectivity tools

These findings belong to the removed runtime and its writable overlays. The
retained private base image alone does not prove `/system/bin/curl`, AA17 or an
initialized app state is present. Current tool inputs and evidence are described
above; do not replace fresh hook/readiness checks with these historical results.

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


## Historical initialized native AA baseline

This section describes the removed native51 runtime. It is recovery evidence,
not a claim that its old userdata, snapshot or compiled artifacts remain present.

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

## Historical single Vela design frame

The one-frame design gate described here was subsequently closed by the owner's
complete run92 approval. Retain this recipe for evidence provenance; do not
repeat the gallery or seek the same approval again.

The owner replaced the19-type design gallery with one Vela generic-pill approval frame. Maps' camera frame80 is already approved. Reuse the Vela engineering APK and initialized native onboarding retained by run84; preserve the older native51 snapshot and rollback APKs. Run84 proves real Vela map/route-preview rendering, not an approved pill or active-guidance acceptance. Verify the retained APK and prefs before claiming a new reusable baseline.

Use the **compiled** observer `.analysis/android-auto-acceptance/quiet-observer/observer.bundle.js`, SHA256 `68a4923351db81aa71e9e40f9ffd4c27f1dcfe4a7e81d8eb8c49baaa384d8120`. The raw ESM source is not a loadable replacement. Stop cached TTS before observation and use the normal AA wake/per-process freezer handling documented above. Require sufficient free disk before boot; run83 encountered an unresponsive guest and a subsequent restore boot was refused with1.3GB free. Completed-job obsolete APKs were audited and removed while keeping native51 and current rollback/input artifacts.

DHU touch coordinates exclude the120px top video margin, while raw screenshots include it. Launcher is `tap 40 435`; native Vela icon in the recorded grid is `tap 540 388`. Allow transitions to settle before taking a separate screenshot. Vela's A5 recent row is `tap 300 138`; the first route's Go is `tap 125 200` (not320). Run84 completed Vela's native welcome/Google choice/system-voice setup. Retain that state rather than repeating onboarding for every picture. Enable the native car speed-display setting for a review that includes the normal speed/limit badge.

Use only the existing archived FM input helper and one camera case for the owner-authorized artificial design event. Configure the receiver after onboarding, ensure both producer/receiver hooks are ready, emit a fresh event, wait for receiver delivery plus1.2s, then capture. Withhold a design claim if distance has expired or the card is absent. Preserve the raw DHU frame; do not compose the UI or describe artificial input as natural provider proof.
