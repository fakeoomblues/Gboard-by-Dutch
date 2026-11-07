# Spotify

Use Spotify's car media browser with DriveDeck, Gearslip or another compatible Android Auto host, and remove the app's advertising and link-tracking surfaces. This page also collects the patches that came from the community Spotify patch set.

**App version:** 9.1.94.1929 · **Build:** 147600575 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the universal original APK for this exact version in Morphe Manager.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Android Auto support** | On | Makes the car media browser, Home and Library work with DriveDeck, Gearslip or another compatible Android Auto host, including Spotify's bundled car service. |
| **Android Auto Up next** | Off | Publishes the real upcoming tracks so the car's Up next list is populated; opt-in while queue startup is verified. |
| **Block popup ads** | On | Blocks fullscreen promotions shown when opening the app. |
| **Change package name** | Off | Changes the package name and app name so the patched app can be installed beside the original. |
| **Disable ad breaks** | Off | Asks Spotify's ads service to turn its audio ad slots off. Experimental: the request path is repaired for this version, but it needs a signed-in free account and real playback to confirm. |
| **Force play order** | Off | Sends shuffle=false so albums and playlists play in order; a server restriction may still apply. |
| **Hide ad sections** | On | Removes brand ad sections from Home and Search. |
| **Hide download buttons** | On | Hides download buttons on tracks, albums, playlists and podcasts; downloads and entitlements are unchanged. |
| **Hide playlist ads** | On | Removes the embedded ad placement from playlists. |
| **Hide premium context-menu rows** | On | Removes Download and Start a Jam rows from song, album and playlist menus; playback, library and sharing stay. |
| **Hide premium settings sections** | On | Removes Advertising, Download quality, Downloads and streaming, Offline, Storage and DJ sections from Settings. |
| **Hide Premium tab** | On | Removes the Premium tab from the bottom navigation bar. |
| **Hide shuffle controls** | On | Removes the shuffle and Smart Shuffle buttons; Spotify Free still enforces its shuffled queue. |
| **Hide video ads** | On | Disables the video ad plugin of the now-playing view. |
| **Hide video shelves (safe)** | On | Drops video and watch sections from the resolved Home feed. |
| **Remove shuffle restriction** | On | Stops the Free shuffle upsell and lets the app use normal play order and shuffle controls. |
| **Sanitize sharing links** | On | Removes `si` and `utm_*` tracking parameters from shared and copied links. |
| **Show Premium in the account row** | On | Shows Premium instead of Free in the account and plan rows; a label change only. |
| **Skip audio ads** | On | Asks Spotify's player to skip an audio ad through its own next command, then seeks to its end if the player refuses, and mutes only as a last resort. Turning on Spotify's "Device broadcast status" improves detection. |

The community set also contained **Hide ad views**, **Hide context menu upsells** and **Diagnose Spotify client**. Their target classes are absent from 9.1.94.1929. The older **Fix third party launchers widgets** and **Use the Premium plan icon** controls were dropped for this version as well: the widget permission check and the plan logo renderer no longer exist in 9.1.94.1929. They are not offered rather than failing the whole patch run.

## Car host access

Spotify identifies a car client by package name and checks the Car App Library host by package and signing certificate. Only Google's Android Auto package and Spotify's own media test app pass those checks. The patch:

- accepts every caller in the Android Auto root selection, the car-client predicate, the media-browser admission check and the three Browse, Recently played and Your Library root filters;
- accepts any host in the bundled Car App Library host validator;
- uses the real Home feed instead of the gated fallback for a custom host;
- adds `QUERY_ALL_PACKAGES` so Android 11 and newer can resolve an arbitrary host package during that check.

Spotify's accounts, catalogue and playback services are unchanged. The host still has to implement the media-browser protocol; this patch does not add one.

## Ad blocking

The audio ad slots are the player's `stream` (between songs) and `preroll` (before the first song) slots. Spotify enables them through the `spotify.ads.esperanto.proto.Settings/updateSlotEnabled` esperanto call. The extension sends the same request with `enabled = false` shortly after the Cosmos transport exists and repeats it, because Spotify enables the slots again after account or network changes.

The extension accepts both request factories Spotify has used: the private `access$createRequest` accessor and the public `createRequest` method. Either way, this version cannot patch successfully while silently skipping every ad request.

**Skip audio ads** is the fallback for ads that still reach the player. When the media session reports an ad, it sends Spotify's own next command, the same one a car's Skip button uses. If the player refuses (Spotify marks the ad unskippable), it seeks to the end of the ad; if that is refused too, the stream is muted for the remaining seconds instead of playing the ad aloud. A true zero-length media swap is not included: the ad audio is resolved by Spotify's native player, and the Java layer only carries display metadata.

## Before using

Sign in to Spotify on the phone. A patched APK is signed with a new certificate, so the Play Store installation cannot be updated in place. A free account still receives server-side ads unless the slot request succeeds; a Premium account does not need the ad patches. Signed-in catalogue, playback and ad behavior were not exercised in this build by the project; the patch output was checked against the original APK and the app was rebuilt from it.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
