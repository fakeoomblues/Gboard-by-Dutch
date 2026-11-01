# Waze

**App version:** 5.26.90.901 · **Build:** 1030746 · **Original file:** APKM

[Download original app](https://buzzheavier.com/cpwy7pirf997){ .md-button .md-button--primary }

Select the complete original APKM archive for this version in Morphe Manager.

[Install Waze for Android Auto: steps with screenshots](../guides/android-auto.md){ .md-button }

## Where these patches come from

24 of these 29 patches come from [bearinmind patches](https://github.com/bearinmindcat/morphe-patches) by bearinmindcat, where they are published as De-Waze for Waze 5.24.5.0. This project ported them to 5.26.90.901 and maintains that port separately. The other five patches and the extras below come from this project, compared with bearinmind patches v2.8.3.

| Area | Added by this project |
| --- | --- |
| Ads | **Hide ads** also removes sponsored search results on the phone and in the car, the ad token and Adspam cookie sent with searches, and main-menu promotions, including ones Waze already saved. |
| Telemetry | **Remove telemetry** also stops advertising ID collection and hides any saved ID, discards usage events already queued for upload, blocks automatic diagnostic log uploads and turns off Firebase Sessions. You can still send logs yourself. |
| Consent | New: **Skip consent screens** answers privacy and terms prompts without showing them and declines personalized ads. |
| Music | New: **SoundCloud in Waze** and **YT Music Morphe in Waze** add both services to the built-in audio player. |
| Other apps | New: **Broadcast navigation data** shares speed and zone data with other apps, and **Support Gearslip** lets the official Gearslip host run Waze. |
| Alert sharing | **Share alerts with Maps** now stops at once when you switch it off and clears alerts it already shared. |

## Patches

### Added by this project

| Patch | Default | What it does |
| --- | --- | --- |
| **Broadcast navigation data** | On | Shares current speed, the native road speed limit and observed average-speed-zone progress/average with other apps, including while Waze is in the background. No overlay required. |
| **Skip consent screens** | On | Answers privacy and terms consent silently and declines ad personalization without showing a screen. |
| **SoundCloud in Waze** | On | Adds SoundCloud to Waze's built-in audio player. Show list displays the signed-in Feed and playable mixes, not the personal Library. SoundCloud Home recommendations are not included. |
| **Support Gearslip** | On | Adds the official Gearslip release package/certificate to the car-host allowlist while preserving Android Auto validation. |
| **YT Music Morphe in Waze** | On | Adds YT Music Morphe to Waze's built-in audio player. Show list displays supported Home recommendations and mixes, not your personal Library. Available shelves depend on the installed client. |

### From bearinmind patches, extended here

| Patch | Default | What it does |
| --- | --- | --- |
| **Hide ads** | On | Removes sponsored pins, stop ads and ad tracking, plus sponsored search results and main-menu promotions. |
| **Remove telemetry** | On | No crash reports, usage stats or performance data. Also blocks advertising ID collection, queued usage uploads and automatic diagnostic logs. Traffic reporting stays. |
| **Share alerts with Maps** | On | Shows Waze alerts in Ungoogled Maps on this phone. Waze must be signed in and open. Switching it off stops sharing at once. |

### From bearinmind patches, ported to 5.26.90.901

| Patch | Default | What it does |
| --- | --- | --- |
| **Alert distances** | On | How early each alert is announced. |
| **All moods** | Off | Unlocks every mood, beta ones too. |
| **Average speed zone bar** | On | Bar with progress and your average speed in an average speed zone. Off until switched on. |
| **Bigger alert pins** | Off | Bigger alert pins on the map. |
| **Bigger speedometer** | On | Bigger speedometer and speed limit digits. |
| **Change app name** | On | Renames the app, with De-Waze as the default. Look for that name in Android Auto too. |
| **Flock cameras** | On | Chime when a Flock camera is ahead, from OpenStreetMap. No map pins or routing. Off until switched on. |
| **Full black** | On | True black dark mode for AMOLED screens. |
| **Map skin** | On | Bigger map labels, a wider arrow and a warm day background. |
| **Navigation distances** | Off | Android Auto heads-up distances and the traffic bar threshold. |
| **No AI reporting** | Off | Turns off Gemini voice reporting. |
| **No Google Assistant** | Off | Removes Google Assistant from Waze. |
| **Open without device location** | Off | Opens Waze with location off and uses it once it is on. Still needs the location permission. |
| **Remove permissions** | On | Removes contacts, calendar, phone, camera and advertising ID access. |
| **Remove sign-in prompts** | On | Stops Waze's own reminder to register your account. |
| **Report speed limit** | Off | Lets you correct speed limits from the speedometer (unverified). |
| **Speed camera alerts** | On | Chime when a speed camera is ahead, from OpenStreetMap, in any country. No map pins or routing. Off until switched on. |
| **Speed camera sound at any speed** | On | Plays the camera sound even when you are not speeding. |
| **Speed cameras everywhere** | Off | Enables camera and police alerts across countries. Check local rules before enabling. |
| **Speed limit sign** | Off | One speed limit sign style in every country. |
| **Stay awake in the background** | Off | Keeps Waze running without a route so its alerts keep reaching Maps. Uses more battery. |

## Advertising and tracking services

Hide ads and Remove telemetry reduce the supported advertising, crash reporting and usage measurement. Waze still uses online services for maps, routes, live traffic, accounts and reports. These patches do not make navigation anonymous.

## Before using

After patching, open **Customization** in the side menu below **Shut off** to configure the features you applied. Speed camera alerts, Flock cameras and Average speed zone bar start off there even when their patches are selected. Restart Waze after changing map or alert-pin settings.

**Broadcast navigation data** needs Waze’s native engine and background location access, not a foreground phone Activity. It automatically includes the background keep-alive control (180 minutes by default, configurable). Averages cover the observed part of a zone; estimated progress and missing/stale values are identified explicitly. [Broadcast contract](https://github.com/johninnl/nl-morphe-patches/blob/main/docs/navigation-broadcasts.md). **Support Gearslip** verifies the supplied release signing certificate; physical-car rendering is a separate test.

**Skip consent screens** makes privacy and terms choices without showing those screens. It declines personalized ads and accepts required terms. Leave this patch off if you want to read and make those choices yourself. Remove telemetry and Remove permissions reduce the listed data access; essential navigation services remain active.

For Android Auto, follow the [KingInstaller steps with screenshots](../guides/android-auto.md). With **Change app name**, Waze may appear as **De-Waze** in the app list. The two audio patches are not tied to one version; the other patches on this page target 5.26.90.901. Available audio content depends on your music app and account. Live routing, road alerts and every car setup have not been fully tested. Report speed limit remains unverified.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md#waze)
