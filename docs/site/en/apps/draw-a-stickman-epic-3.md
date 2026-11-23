# Draw a Stickman: EPIC 3

Play the game on the Android Auto car screen and on car hosts such as DriveDeck and Gearslip.

**App version:** 1.13.19873 · **Build:** 19873 · **Original file:** APKS

[Use your original APKS](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete original APKS archive in Morphe Manager, including the base and architecture APKs.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Hide ads** | On | Keeps the ad SDK from starting: initialization completes locally, every ad request reports the SDK's own no-fill error and every show reports not-ready, so no ad network or mediation adapter is contacted. |
| **Disable tracking** | On | Removes the advertising identifier and install-referrer permissions, stops the AppLovin and IronSource launch starters, and turns off Facebook's advertiser identifier and event upload. |

## Before using

This game already declares itself a parked app: its launcher activity carries `android.intent.category.CAR_LAUNCHER` and the app declares the game category, so car hosts that follow Google's documented discovery list it without a manifest patch. Only the ad and tracking patches are offered here.

Rewards that rely on watching an ad are no longer offered, because there is never a filled ad to show. Drawing tools, purchases already made and progress keep working.

Validated with build, patch-session and patched-manifest checks against the original APKS. No car-screen play session has been run.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
