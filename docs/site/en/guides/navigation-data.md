# Waze and Flitsmeister alerts in other map apps

Keep Google Maps or Vela Maps as your navigation app while patched Waze or Flitsmeister supplies speed, road limits and available road warnings in the background. The apps exchange those observations locally on the same phone. You need to patch both the provider and the map app that receives its data.

## What the integration adds

| Data | What the receiving map can show |
| --- | --- |
| Current speed and road limit | A compact speed display beside a speed-limit sign, using fresh provider values and native fallback where available. |
| Average-speed zones | The observed average, progress and remaining distance while a zone is active. Available pre-entry observations can give advance warnings. |
| Road reports | Available static/mobile cameras, police, hazards, crashes, traffic jams and closures. Enabled Waze camera layers can also share their observations. Coverage depends on the provider. |

An average covers only the part of a zone observed since the provider started tracking it. Starting halfway through a zone cannot recover the earlier part. Estimated distances and progress remain approximate; a nearby report is not automatically on your chosen route.

This navigation feed is separate from the providers' advertising and usage tracking. **Remove telemetry** in Waze and **Disable tracking** in Flitsmeister control supported tracking services; **Broadcast navigation data** shares driving observations with your selected map app.

## 1. Patch the apps you need

Choose one provider and one receiving map app. You can install both providers, then select which one to use in the receiving app.

| App | Supported original | Patch required for this setup |
| --- | --- | --- |
| [Waze](../apps/waze.md) | 5.26.90.901, complete APKM | **Broadcast navigation data** |
| [Flitsmeister](../apps/flitsmeister.md) | 14.17, complete APKS | **Broadcast navigation data**; keep **Fix PairIP startup** enabled |
| [Google Maps](../apps/google-maps.md) | 26.43.05.1002020710, complete APK | **Waze alerts**; also select **Native navigation for Gearslip and Android Auto** for the native car interface |
| [Vela Maps](../apps/vela.md) | 0.5.2621, complete APK | **External speed controls** |

Apply these patches from Dutch App Patches in Morphe Manager. For Maps, select its patches from this source only; the adapted bearinmind controls are already included. Install the resulting apps and complete first-run setup on the phone. For a receiving navigation app used in Android Auto, follow the [KingInstaller installation guide](android-auto.md).

The provider patch has an optional **Receiver package** setting. Leave it blank if you want either supported map app to receive updates while it runs. To target one app, enter `app.vela` for Vela, or the actual package of your patched Maps copy, normally `org.ungoogled.android.apps.maps`. A custom package or the microG-RE variant needs its own package name. One targeted package will not deliver updates to a different receiver.

## 2. Keep the provider running

Open the patched Waze or Flitsmeister once, finish onboarding and grant the location access its driving service needs. Allow background location when requested for this use. The provider must keep its navigation engine or driving/location service running while the other map app is in front. Its overlay can stay off.

Waze's broadcast patch includes the background keep-alive control, initially 180 minutes and configurable. Flitsmeister publishes while its driving/location service runs. Closing or force-stopping the provider stops fresh observations. If updates disappear after the screen turns off, check the provider's background battery restrictions on your phone.

## 3. Choose the source in your map app

### Google Maps / Ungoogled Maps

1. Open the account menu, then **Waze & external data source**.
2. Open **Navigation data source** and choose **De-Waze · navigation data** or **Flitsmeister · navigation data**.
3. Enable **Compact speed and average-speed control** if you want the speed, limit and active-zone details together.
4. Open **Road alert types** and select the categories you want. Only police reports start selected.
5. Configure **Road alerts on the map** and **Chime for a road alert ahead** separately. Only reports with known positions become map pins.
6. Check **Source now** for the provider status. Tap it to request an available snapshot; this cannot start a stopped provider or make an old observation fresh.

**Off · Google Maps** restores Maps' own data. The alternative **Waze · Share alerts with Maps** uses the older Waze alert bridge: Waze needs **Share alerts with Maps**, must be signed in and must remain running. That mode does not provide the extended speed/limit/zone feed. Select the navigation-data option for this guide.

Maps pauses its own Flock pins and chimes while the selected modern feed supplies usable fresh data. Saved Flock preferences remain, and Flock route avoidance keeps its separate camera dataset.

### Vela Maps

1. Open navigation settings, then **Speed and road alerts**.
2. Choose **De-Waze** or **Flitsmeister**. **Vela (native data)** restores the native source.
3. Configure **Spoken road warnings** if wanted. It follows Vela's voice and mute settings.
4. In the car settings, **Navigation provider** selects the same saved source. **Show zone and warning details** controls the additional car details.

The compact display shows speed beside the limit. An active zone adds the observed average, progress and remaining distance; estimated progress is marked with ≈. The average stays unknown until enough distance and time have been observed.

With an external source selected, Vela pauses its own Flock display and alerts while either supported provider supplies fresh usable data. The pause can continue if the selected source drops out while the other still publishes. Saved preferences resume when neither feed is usable, or when you select **Vela (native data)**.

## 4. Check reception before relying on it

While parked, open the provider and then the receiving map app. Check that the chosen source is listed and that it reports fresh data when observations are available. Leave the provider in the background and confirm reception continues. A stationary speed of zero is valid; a missing speed or limit must remain unknown or use the map's native fallback.

Changing provider, leaving a zone or losing fresh zone observations clears the partial average. The two providers' values are kept separate. Android 14 and newer let the receiving app verify which installed provider sent an update; older versions lack that Android sender verification.

| Problem | Check |
| --- | --- |
| Source stays unavailable | Both apps were patched, the provider was opened, its driving/location service is active and the correct source is selected. |
| One receiver gets data but another does not | Remove or correct the provider patch's optional **Receiver package**, then repatch the provider if you change that option. |
| Speed works but no average appears | A fresh active-zone observation is required, followed by enough observed time and distance. A camera report alone is not an active zone. |
| No pins or warning sound in Maps | Select the report types and enable the map/chime switches. The provider must have fresh reports, with positions for pins and a usable approach distance for warnings. |
| Data stops when the screen locks | Check background location, the provider service and phone battery restrictions. Reopen a stopped provider. |

Google Maps' consumer display and fallback were checked with controlled provider replays. Vela's background reception, phone/car display and advance speech were checked on an Android 16 emulator with Gearslip. Native Maps navigation has separate Gearslip and official Android Auto Desktop Head Unit checks. These results do not establish every combination of provider, phone, physical car display or live-road accuracy.

For developers, the [shared navigation broadcast contract](https://github.com/johninnl/nl-morphe-patches/blob/main/docs/navigation-broadcasts.md) documents the data and freshness rules. Other receiving apps need an implementation of that contract; installing the provider patch alone does not add support to an arbitrary app.
