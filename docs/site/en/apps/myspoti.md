# Myspoti

**App version:** 2.3.0 · **Build:** 1121 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the publisher-signed Myspoti APK. Compatibility is **Any** version when the existing browser access structure is uniquely recognized; an unsupported or ambiguous structure stops patching. Actual APK patching and rebuilds have been tested with 2.2.0 / 1116 and 2.3.0 / 1121. The version above is the reference input. Myspoti is developed by [kota-zo/myspoti](https://github.com/kota-zo/myspoti).

[Set up music players and personal libraries in Gearslip](../guides/gearslip-music.md){ .md-button }

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Custom branding** | Off | Sets a custom app name and selects one of six bundled Spotify-style launcher icons, or keeps the original icon. |
| **Gearslip personal library** | On | Lets the configured Gearslip host browse Myspoti's existing Home, Library and Downloads, use search and select music for playback. |
| **Android Auto Up next** | Off | Exports the actual current and upcoming playlist/radio occurrences with metadata and queue selection. Requires the original 2.3.0 / 1121 APK; runtime acceptance is pending. |

## Advertising and tracking services

This compatibility patch changes browser access. It does not remove online services, supply an account or change subscription access.

## Before using

**Android Auto Up next** is a local development addition for 2.3.0 / 1121. Select it explicitly when patching the original APK; it can be combined with the other patches. It publishes a window of up to 50 actual player occurrences, including the current item, and selects upcoming rows through Myspoti's existing queue handler. Desktop verification does not establish phone/car playback acceptance.

Enable **Custom branding** to use its **App name** field and **App icon** dropdown. The choices are Original, Classic Spotify Green, Elegant dark Spotify, Emerald Spotify on pitch black, Dark emerald Spotify glass, Platinum Spotify on charcoal, and Black Spotify on warm white. An empty name keeps Myspoti. Name and icon can be changed independently; changing them requires patching again. Branding can be selected together with **Gearslip personal library** from the same bundle. It changes the Android app label and launcher icon; in-app logos, the splash screen, notification artwork and account access stay as provided by Myspoti.

Sign in to Myspoti. The trusted-host option defaults to the Gearslip and DriveDeck packages, including their `.dev` builds. Add another host package only for a build you intentionally use.

The user confirmed that the patch works with Myspoti 2.2.0 in Gearslip and approved its stable release. The exact publisher 2.3.0 input passes desktop patching. A reported Manager archive-read error was traced to a corrupted phone copy; transfer a fresh original APK if this occurs. Version 2.3.0 has not yet received the same signed-in device acceptance. A patched APK must be signed again. The publisher-signed installation cannot be updated using a different signing key. Keep it while testing a separate copy; do not uninstall it just to try this patch.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
