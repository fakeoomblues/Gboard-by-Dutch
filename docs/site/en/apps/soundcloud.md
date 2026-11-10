# SoundCloud

**App version:** 2026.11.02-release · **Build:** 379060 · **Original file:** APKM

[Use your original APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete publisher-signed APKM archive for this exact build (Android 9+), including its device files.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Home in Android Auto** | On | Fills the car Home tab with regular cover tiles from the phone's Home, using the app's existing account. Tracks, mixes and artists play; playlists and albums open. Publishes the real playback queue for Up next, with queue item selection. Falls back to SoundCloud's original car Home if the phone Home cannot load. |
| **Disable analytics** | On | Stops SoundCloud's event storage and dispatch through its analytics handler, preserving the worker's own shutdown path. Other tracking services remain active. |
| **Disable consent popup** | On | Skips the OneTrust popup using the app's no-op consent controller. Does not grant consent or disable tracking; select Disable analytics separately. |
| **Enable SoundCloud Go** | On | Enables local Go+ plan controls, hides Go upsells and consumer-plan transition screens, disables client ad requests and display placements, and forces the offline_sync, no_audio_ads and hq_audio switches from the /configuration response on. See below for what those switches can and cannot do. |
| **Add microG-RE support** | Off | Routes SoundCloud's Google services — sign-in and listening notifications over Firebase Cloud Messaging, Cast, the ads identifier and in-app review — through an installed MicroG-RE (MicroG-RE or ReVanced GmsCore) on phones without Google Play services. The sign-in itself is SoundCloud's own web login and needs neither. Leave off on phones with real Play services. |

## Why Home was empty in the car

SoundCloud's car Home tab reads an older discovery feed, while the phone Home uses the newer Home sections. Feed and Library already worked because they use your own stream and collection. The patch asks the phone's GraphQL SDUI Home service for its sections, rather than the separate library-banner endpoint used by the earlier patch, when the car opens Home. It applies to Android Auto, Gearslip, DriveDeck and other media hosts that browse SoundCloud.

Music Home uses an ungrouped cover grid. Tracks inside playlists and albums retain the app's track-list presentation.

## The Go switches, honestly

SoundCloud's app decides locally what to show for its subscription features: the `/configuration` response lists switches such as `offline_sync`, `no_audio_ads` and `hq_audio`, and only the app itself reads them. Enable SoundCloud Go stores all three as enabled and overlays Go+ plan values locally — plus the parts other SoundCloud patch sets leave out: no pending downgrade, no Go upsells or plan-transition screens, and the client's ad requests and display placements disabled outright, so the ad side does not depend on the `no_audio_ads` switch alone.

What the switches cannot do:

- **High-quality audio** shows the setting and lets the app ask for HQ. Whether HQ is delivered is SoundCloud's server decision: the higher-bitrate AAC streams are listed and authorised for a paying Go+ login. A free account keeps the normal streams, or a refusal when the app asks anyway.
- **Offline sync** unhides the download buttons. Go+-only tracks remain 30-second previews, and encrypted offline copies need a real subscription. At most the app can save tracks the account can already stream.
- **No audio ads** does work locally, and the patch's ad-request overrides cover the same ground more broadly.

Local Go+ state never establishes server-side access. Paid streams, downloads and audio quality stay tied to the account.

## Sign in through microG-RE

SoundCloud signs in through its own web page in a browser tab (`secure.soundcloud.com`, returning over the `sc://` deep link), so logging in — with email or the Google button — works on any phone, with or without Google services. What does need Google Play services is everything around the account: notifications ride Firebase Cloud Messaging, which registers through Play services, and the app gates Cast, the ads identifier and in-app review on its Play services check. On a degoogled phone none of that runs. Add microG-RE support points every Play services name in the app and its manifest at MicroG-RE, answers the availability check itself, and tells microG to vouch for stock SoundCloud's package and publisher certificate, so those services work through microG. Turn it off when real Play services is installed.

## Advertising and tracking services

Home in Android Auto changes the car Home content and exports the active player queue to the public media session. Next alone does not publish upcoming titles or artwork. The three additional controls act independently: Disable analytics stops the app's event handler, Disable consent popup skips OneTrust, and Enable SoundCloud Go changes local plan and advertising behavior. They do not remove every tracking service or supply an account. Local Go+ controls do not establish paid-content access on SoundCloud's servers.

## Before using

Sign in to SoundCloud on the phone. A patched APK must be signed again, so the Play Store installation cannot be updated in place; keep it until the patched copy works. All additional controls rebuild independently and together with Home on this exact input. Their local effects and app startup were checked on Android without an account. Signed-in Home, cover tiles and Up next were confirmed on the phone after an in-place upgrade. Downloads and audio quality remain dependent on your account and server access.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
