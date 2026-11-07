# Deezer

Use Deezer's car media browser with DriveDeck, Gearslip or another compatible Android Auto host.

**App version:** 9.0.26.3 · **Build:** 9002603 · **Original file:** APKM

[Use your original APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete original archive for this exact version in Morphe Manager, including its architecture, language and display splits.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Allow custom Android Auto hosts** | On | Lets Deezer's car media browser accept a third-party Android Auto host instead of rejecting it through its known-host certificate list. |

## Why the car browser was empty

Deezer's media-browser service validates the caller package and signing certificate, then replaces the browser root with null when the caller is not trusted. The patch keeps the caller lookup and the root construction and skips only that rejection branch. `QUERY_ALL_PACKAGES` is added so Android 11 and newer can resolve an arbitrary host package.

Deezer's account, catalogue and playback services are unchanged.

## Before using

Sign in to Deezer on the phone first. A patched APK is signed with a new certificate and cannot be updated in place from the Play Store.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
