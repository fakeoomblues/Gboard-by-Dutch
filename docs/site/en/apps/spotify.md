# Spotify

Use Spotify's car media browser with DriveDeck, Gearslip or another compatible Android Auto host, and remove the app's advertising and link-tracking surfaces. This page also collects the patches that came from the community Spotify patch set.

**App version:** 9.2.0.26 · **Build:** 148373686 · **Original file:** APKM

[Use your original APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete original archive for this exact version in Morphe Manager, including its architecture, language and display splits.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Allow custom Android Auto hosts** | On | Lets Spotify's car media browser and its bundled car service accept a third-party host such as DriveDeck or Gearslip, including the Browse, Recently played and Your Library roots. |
| **Share the car media library with custom hosts** | On | Accepts a custom host at the media-browser admission check and offers it the Android Auto root, so it can browse Home and Library. |
| **Use the car Home and Library feeds** | On | Uses Spotify's real car Home loader and routes Library to the regular car feed instead of empty fallbacks. |
| **Block popup ads** | On | Blocks fullscreen promotions shown when opening the app. |
| **Change package name** | Off | Changes the package name and app name so the patched app can be installed beside the original. |
| **Disable ad breaks** | Off | Asks Spotify's ads service to turn its audio ad slots off. Experimental: the request path is repaired for this version, but it needs a signed-in free account and real playback to confirm. |
| **Fix third party launchers widgets** | On | Allows the Spotify widgets to be added to third-party launchers. |
| **Force play order** | Off | Sends shuffle=false so albums and playlists play in order; a server restriction may still apply. |
| **Hide ad sections** | On | Removes brand ad sections from Home and Search. |
| **Hide download buttons** | On | Hides download buttons on tracks, albums, playlists and podcasts; downloads and entitlements are unchanged. |
| **Hide playlist ads** | On | Removes the embedded ad placement from playlists. |
| **Hide premium context-menu rows** | On | Removes Download and Start a Jam rows from song, album and playlist menus; playback, library and sharing stay. |
| **Hide premium settings sections** | On | Removes Advertising, Download quality, Downloads and streaming, Offline, Storage and DJ sections from Settings. |
| **Hide Premium tab** | On | Removes the Premium tab from the bottom navigation bar. |
| **Hide shuffle controls** | On | Removes the shuffle and Smart Shuffle buttons; Spotify Free still enforces its shuffled queue. |
| **Hide video ads** | On | Disables the video ad plugin of the now-playing view. |
| **Hide video shelves** | On | Drops video and watch sections from the resolved Home feed. |
| **Remove shuffle restriction** | On | Stops the Free shuffle upsell and lets the app use normal play order and shuffle controls. |
| **Sanitize sharing links** | On | Removes `si` and `utm_*` tracking parameters from shared and copied links. |
| **Share the car Up next queue** | On | Publishes the real upcoming tracks to the media session where peek-next would hide the car's Up next list. |
| **Show Premium in the account row** | On | Shows Premium instead of Free in the account and plan rows; a label change only. |
| **Skip audio ads** | On | Asks Spotify's player to skip an audio ad through its own next command, then seeks to its end if the player refuses, and mutes only as a last resort. Turning on Spotify's "Device broadcast status" improves detection. |
| **Use the Premium plan icon** | On | Shows the bundled premium logo on the account/plan row instead of the white Spotify glyph. |

The community set also contained **Hide ad views**, **Hide context menu upsells** and **Diagnose Spotify client**. Their target classes are absent from 9.2.0.26, so they are not offered here rather than failing the whole patch run.

## Car host access

Spotify identifies a car client by package name and checks the Car App Library host by package and signing certificate. Only Google's Android Auto package and Spotify's own media test app pass those checks. The patch:

- accepts every caller in the Android Auto root selection, the shared car-client predicate and the three Browse, Recently played and Your Library root filters;
- returns the bundled allow-all host validator for Spotify's Car App Library service;
- adds `QUERY_ALL_PACKAGES` so Android 11 and newer can resolve an arbitrary host package during that check.

Spotify's accounts, catalogue and playback services are unchanged. The host still has to implement the media-browser protocol; this patch does not add one.

## Ad blocking

The audio ad slots are the player's `stream` (between songs) and `preroll` (before the first song) slots. Spotify enables them through the `spotify.ads.esperanto.proto.Settings/updateSlotEnabled` esperanto call. The extension sends the same request with `enabled = false` shortly after the Cosmos transport exists and repeats it, because Spotify enables the slots again after account or network changes.

The 9.2 build exposes the request factory as a public `createRequest` method; older builds used the private `access$createRequest` accessor. The extension accepts both, so this version cannot patch successfully while silently skipping every ad request.

**Skip audio ads** is the fallback for ads that still reach the player. When the media session reports an ad, it sends Spotify's own next command, the same one a car's Skip button uses. If the player refuses (Spotify marks the ad unskippable), it seeks to the end of the ad; if that is refused too, the stream is muted for the remaining seconds instead of playing the ad aloud. A true zero-length media swap is not included: the ad audio is resolved by Spotify's native player, and the Java layer only carries display metadata.

## Before using

Sign in to Spotify on the phone. A patched APK is signed with a new certificate, so the Play Store installation cannot be updated in place. A free account still receives server-side ads unless the slot request succeeds; a Premium account does not need the ad patches. Signed-in catalogue, playback and ad behavior were not exercised in this build by the project; the patch output was checked against the original APK and the app was rebuilt from it.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
