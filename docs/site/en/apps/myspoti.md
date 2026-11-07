# Myspoti

**App version:** 2.4.0 · **Build:** 1142 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the publisher-signed Myspoti APK. **Custom branding** and **Gearslip personal library** are compatible with any version whose existing browser access structure is uniquely recognized; an unsupported or ambiguous structure stops patching. **Android Auto Up next** accepts 2.3.0 / 1121 and 2.4.0 / 1142. The version above is the reference input. Myspoti is developed by [kota-zo/myspoti](https://github.com/kota-zo/myspoti).

[Set up music players and personal libraries in Gearslip](../guides/gearslip-music.md){ .md-button }

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Custom branding** | Off | Sets a custom app name and selects one of six bundled Spotify-style launcher icons, or keeps the original icon. |
| **Gearslip personal library** | On | Lets the configured Gearslip host browse Myspoti's existing Home, Library and Downloads, use search and select music for playback. |
| **Android Auto Up next** | Off | On 2.4.0, Myspoti already exports the actual current and upcoming playlist/radio queue with metadata and queue selection; this patch makes it available to every Android Auto host, or only to the host packages you list. On 2.3.0 the patch also exports that queue. |

## Advertising and tracking services

These compatibility patches change browser access and the car host decision. They do not remove online services, supply an account or change subscription access.

## Before using

Myspoti 2.4.0 publishes its own queue to the media session, so **Android Auto Up next** no longer duplicates it: the patch removes the original car-host restriction. Leave **Allowed host packages** empty to accept every Android Auto system, including third-party hosts such as DriveDeck and Gearslip. To restrict access, enter one or more Android app package names separated by commas, for example `app.johninnl.drivedeck`; every other app is denied browsing. Include `com.google.android.projection.gearhead` when Google's Android Auto must remain allowed as well. Repatch to change this choice.

**Gearslip personal library** remains a separate trusted-host list and keeps Myspoti's UID ownership checks. The default is the Gearslip and DriveDeck packages, including their `.dev` builds. When both patches are enabled, the Gearslip list is admitted first and the Android Auto choice applies to every other caller. Add another host package only for a build you intentionally use.

On 2.3.0 / 1121, enable **Android Auto Up next** to export up to 50 actual player occurrences, including the current item, and select upcoming rows through Myspoti's existing queue handler. The user confirmed that the missing Up next behavior was fixed on the phone after the signed APK was installed as an update, preserving app data and the existing Spotify branding.

Enable **Custom branding** to use its **App name** field and **App icon** dropdown. The choices are Original, Classic Spotify Green, Elegant dark Spotify, Emerald Spotify on pitch black, Dark emerald Spotify glass, Platinum Spotify on charcoal, and Black Spotify on warm white. An empty name keeps Myspoti. Name and icon can be changed independently; changing them requires patching again. Branding can be selected together with the other two patches from the same bundle. It changes the Android app label and launcher icon; in-app logos, the splash screen, notification artwork and account access stay as provided by Myspoti.

Sign in to Myspoti before testing the car. A patched APK must be signed again. The publisher-signed installation cannot be updated using a different signing key. Keep it while testing a separate copy; do not uninstall it just to try this patch.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
