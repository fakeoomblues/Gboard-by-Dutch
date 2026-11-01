# Gearslip music and drawer patches

These source changes add three independent Morphe controls. Gearslip's upstream source checkout is unchanged; no Gearslip fork is needed.

| Target | Patch | Behavior |
| --- | --- | --- |
| Gearslip 0.3.12-alpha and 0.3.31-alpha | Keep music player entries | Retains a music-player tile when an app also provides a SETTINGS car service. The Settings entry remains separately labelled. |
| Gearslip 0.3.12-alpha and 0.3.31-alpha | Hide apps from drawer | Adds an **Apps** card to Gearslip's phone settings with a checkbox per drawer tile, all ticked by default. Unticking hides that tile from the car drawer without disabling the app. |
| YouTube Music Any (structure-checked) | Gearslip music browser support | Accepts configured Gearslip packages only when the original browser callback UID owns that package. Adds browser eligibility for those hosts while retaining unrelated callers' original checks. |

## Using the panel

Select SoundCloud or YT Music from Gearslip's launcher and start a track. Return to the map, then press the dock icon at the right end of the navigation bar's now-playing pill. Gearslip draws the panel beside the map. Its left/right placement is controlled by Gearslip's existing player setting. The reference screenshot uses Vela and Metrolist; the panel belongs to Gearslip and is separate from Waze's phone player.

SoundCloud already connected and displayed its artwork and controls beside Waze in the official 0.3.31-alpha phone preview. The Waze audio patches are not needed to draw that panel. Like, shuffle, repeat and library content depend on what the music app exposes.

Gearslip's stock drawer has pin/unpin, reorder and close actions, plus fixed exclusions, but no setting for which apps it shows. **Hide apps from drawer** adds one on the phone. Open the **Apps** card under **Car** on Gearslip's home screen. Each row is one drawer tile, so a music app with a separate Settings tile has two rows. Untick a row to hide that tile, and tick it to bring it back. The change takes effect when you leave the screen, and it replaces the earlier patch-time `hiddenPackages` option.

The filter only applies to the drawer. Car messages and a tile already pinned to the nav bar are unaffected. Gearslip's own tiles, such as Settings and Exit, are not listed, so they stay reachable.

## Version compatibility

The YT Music patch declares **Any** version. It finds the original UID-check boundary by its diagnostic and PackageManager lookup, follows its browser call graph, and discovers package/context fields from their use. It has no release table or obfuscated member names. Missing or ambiguous matches abort patching; Any is not a guarantee for every future internal layout. Real builds 9.30.53 and 9.44.52 remain the tested references.

## Caller identity and signing

YT Music's `packageNames` option defaults to `app.seb3thehacker.gearslip`. Add other host package names only when intentionally using those builds. The local validation copy `app.seb3thehacker.gearslip.morphetest` is not trusted by default.

The UID passed through the original MediaBrowser callback is the authority. Reading `Binder.getCallingUid()` later on YT Music's handler thread can identify YT Music itself. The new early host decision is before its server flag and Android Auto-dependent checks; it does not impersonate Google's Android Auto package. Other clients retain their original policy and original eligibility body.

Patched APKs must be signed again. The installed YT Music certificate does not match the checked local Morphe keystore, so no in-place update was attempted and the existing login was preserved. Use the same signing key as the installed copy for an update. A newly signed Gearslip APK likewise cannot update an officially signed installation. The validation copies use separate package names; the official apps were not uninstalled.

## Validation

The bundle built with the release workflow's Android build and existing extension test tasks. Strict patch application and APK rebuild passed on all four targets. On the connected CPH2747:

- Both Gearslip versions passed 9 actual-DEX launcher checks. Original DEX first reproduced the missing player. Adjacent cases cover navigation with incidental audio, media-only, Settings-only, and empty app candidates.
- Both YT Music versions passed emitted-DEX inspection and 9 actual-DEX caller-policy checks each. These execute the real modified classes on Android with real PackageManager UID ownership. A configured installed host is accepted while server dependencies are deliberately unset; a spoofed package under another UID is rejected. The unrelated-client path still reaches the original implementation. The production Java policy also has 15 JVM assertions.

The YT Music tests use isolated policy fixtures containing the actual patched DEX, byte-for-byte, with a minimal manifest and no account providers or exported music service. They establish policy execution, not a live Binder connection to YT Music's full browser, signed-in catalog delivery, playback, or car projection. Those integration checks remain pending a test installation signed with the installed app's key, or a separate fully configured client. The phone's installed account-bearing YT Music was not modified.

Local reports are in `.analysis/gearslip-validation/` and `.analysis/gearslip-ytm-validation/`. Validation harnesses are in `scripts/gearslip/` and `scripts/ytmusic/`. The same launcher, exclusion and both caller-policy probes also passed on a fresh API 36 emulator. Both YT Music policies additionally passed with the official Gearslip host and Android Auto removed for the emulator user. Further testing is restricted to that disposable emulator; signed-in client integration remains unverified.

The full [YT Music caller-validation procedure](../../scripts/ytmusic/README.md) distinguishes structural checks, Android policy execution, and a real browser session.

The Apps card ran on an API 36 emulator with the official 0.3.31-alpha and 0.3.12-alpha builds. Both showed the card and listed every tile. On 0.3.31-alpha, unticking Waze and YouTube Music · Settings removed those tiles from the car preview's drawer, ticking Messages again restored it, and the choices survived a force-stop. This covers the phone preview, not a physical head unit.
