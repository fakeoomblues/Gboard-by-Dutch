# Gearslip

**App version:** 0.3.31-alpha · **Build:** 13 · **Original file:** APK

[Choose an original file](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Use the official Gearslip APK. The patches also support 0.3.12-alpha, build 12.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Keep music player entries** | On | Keeps a music app's player available alongside its separate car Settings screen, including YT Music Morphe. |
| **Hide apps from drawer** | Off | Hides the packages listed in the Packages to hide option. Empty leaves Gearslip's own exclusions unchanged. Apps remain installed. |

## Advertising and tracking services

These patches change the app drawer and player selection. They do not change Gearslip's usage reporting, network services or permissions.

## Before using

Start a track in Gearslip's music screen, return to the map, then use the dock button in the now-playing bar to place the player beside the map. SoundCloud uses Gearslip's existing player. YT Music also needs its separate **Gearslip music browser support** patch.

For the drawer filter, enter comma-separated package names before patching. It hides all discovered entries from each listed package; Gearslip's built-in Settings remains available. Repatch with a shorter list to restore entries. There is no new in-app hide/show menu.

The launcher and host-identity checks have passed on Android. Signed-in YT Music browsing and playback still need integration testing. A newly signed patched APK cannot update an official installation signed with another key; preserve the existing installation and its data when testing.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
