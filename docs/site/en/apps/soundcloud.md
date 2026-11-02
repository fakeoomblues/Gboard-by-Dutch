# SoundCloud

**App version:** 2026.10.28-release · **Build:** 379050 · **Original file:** APKM

[Use your original APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete publisher-signed APKM archive for this exact build (Android 12L+), including its device files.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Home in Android Auto** | On | Fills the car Home tab with the same Home sections as the phone app, grouped by section title, using the app's existing account. Tracks, mixes and artists play; playlists and albums open. Falls back to SoundCloud's original car Home if the phone Home cannot load. |

## Why Home was empty in the car

SoundCloud's car Home tab reads an older discovery feed, while the phone Home uses the newer Home sections. Feed and Library already worked because they use your own stream and collection. The patch asks the app's own Home service for the phone sections when the car opens Home. It applies to Android Auto, Gearslip, DriveDeck and other media hosts that browse SoundCloud.

## Advertising and tracking services

This compatibility patch changes only the car Home content. It does not remove online services, supply an account or change subscription access.

## Before using

Sign in to SoundCloud on the phone. A patched APK must be signed again, so the Play Store installation cannot be updated in place; keep it until the patched copy works. The patch was verified on the exact input: patching and rebuilding succeed, the patched app starts, and the Home request runs through the app's own service on Android. Signed-in car browsing and playback are confirmed on a device after installation.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
