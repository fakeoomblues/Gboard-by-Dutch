# Gearslip

**App version:** 0.3.31-alpha · **Build:** 13 · **Original file:** APK

[Download original app](https://buzzheavier.com/szlgyqfl0ghx){ .md-button .md-button--primary }

Use the official Gearslip APK. The patches also support 0.3.12-alpha, build 12.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Keep music player entries** | On | Keeps a music app's player available alongside its separate car Settings screen, including YT Music Morphe. |
| **Hide apps from drawer** | On | Adds an **Apps** card to Gearslip's phone settings. Every app is ticked by default; untick one to hide its tile from the car drawer. Apps remain installed. |

## Advertising and tracking services

These patches change the app drawer and player selection. They do not change Gearslip's usage reporting, network services or permissions.

## Before using

Start a track in Gearslip's music screen, return to the map, then use the dock button in the now-playing bar to place the player beside the map. SoundCloud uses Gearslip's existing player. YT Music also needs its separate **Gearslip music browser support** patch.

To choose what the car drawer shows, open the **Apps** card on Gearslip's home screen on your phone. Every app with a car screen or player is ticked. Untick one to hide its tile, and tick it again to bring it back. A music app with a separate Settings tile has a row for each. A tile you pinned to the nav bar stays there, and Gearslip's own tiles such as Settings aren't listed.

The launcher and host-identity checks have passed on Android. Signed-in YT Music browsing and playback still need integration testing. A newly signed patched APK cannot update an official installation signed with another key; preserve the existing installation and its data when testing.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
