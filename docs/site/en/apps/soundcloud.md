# SoundCloud

**App version:** 2026.10.28-release · **Build:** 379050 · **Original file:** APKM

[Use your original APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete publisher-signed APKM archive for this exact build (Android 12L+), including its device files.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Home in Android Auto** | On | Fills the car Home tab with regular cover tiles from the phone's Home, using the app's existing account. Tracks, mixes and artists play; playlists and albums open. Publishes the real playback queue for Up next, with queue item selection. Falls back to SoundCloud's original car Home if the phone Home cannot load. |
| **Disable analytics** | On | Stops SoundCloud's event storage and dispatch through its analytics handler. Other tracking services remain active. |
| **Disable consent popup** | On | Skips the OneTrust popup using the app's no-op consent controller. Does not grant consent or disable tracking; select Disable analytics separately. |
| **Enable SoundCloud Go** | On | Enables local Go+ plan controls, hides Go upsells and consumer-plan transition screens, and disables client ad requests and display placements. Paid streams, downloads and audio quality still depend on the account's server access. |

## Why Home was empty in the car

SoundCloud's car Home tab reads an older discovery feed, while the phone Home uses the newer Home sections. Feed and Library already worked because they use your own stream and collection. The patch asks the phone's GraphQL SDUI Home service for its sections, rather than the separate library-banner endpoint used by the earlier patch, when the car opens Home. It applies to Android Auto, Gearslip, DriveDeck and other media hosts that browse SoundCloud.

Music Home uses an ungrouped cover grid. Tracks inside playlists and albums retain the app's track-list presentation.

## Advertising and tracking services

Home in Android Auto changes the car Home content and exports the active player queue to the public media session. Next alone does not publish upcoming titles or artwork. The three additional controls act independently: Disable analytics stops the app's event handler, Disable consent popup skips OneTrust, and Enable SoundCloud Go changes local plan and advertising behavior. They do not remove every tracking service or supply an account. Local Go+ controls do not establish paid-content access on SoundCloud's servers.

## Before using

Sign in to SoundCloud on the phone. A patched APK must be signed again, so the Play Store installation cannot be updated in place; keep it until the patched copy works. All three additional controls rebuild independently and together with Home on this exact input. Their local effects and app startup were checked on Android without an account. Signed-in Home, cover tiles and Up next were confirmed on the phone after an in-place upgrade. Downloads and audio quality remain dependent on your account and server access.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
