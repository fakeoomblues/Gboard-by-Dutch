# Google Messages

Open Messages' own car screen on DriveDeck and other Android Auto hosts, from a copy of Messages that installs beside the stock app. **Experimental:** these patches have not yet been confirmed on a phone.

**App version:** messages.android_20261030_00_RC00.phone_samsung_dynamic · **Build:** 327677900 · **Original file:** APKS

[Use your original APKS](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete original APKS archive in Morphe Manager, including the base, arm64, display and language APKs. This is the Samsung phone build of Messages; other Messages builds are not supported.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Allow any Android Auto host** | On | Lets any Android Auto compatible host open Messages' car screen, such as DriveDeck, Gearslip, AARE or Flywheel, whatever its package name or signing key. Stock Messages only accepts Google's own Android Auto. The car screen is also switched on from the start, instead of waiting for a Google settings sync. |
| **Disable screen-share warning** | On | Stops Messages treating a car host's screen as screen sharing, so message text stays visible in notifications and Messages does not warn that someone has access to your screen. This also turns the warning off for real screen sharing in this copy. |
| **Change package name** | On | Installs the patched Messages beside the stock Google Messages that comes with the phone. Every provider and permission Messages declares moves with it, so the two never collide. |
| **Custom branding** | On | Names the patched copy "Messages AA" with its own launcher icon. Another name or Messages' original icon can be chosen instead. |

## Before using

The car screen lists your ten most recent conversations. Opening one lets the host read messages aloud, send a reply and mark the conversation as read; Messages sends the reply itself, the same way as from the phone. Starting a new conversation is up to the host, because Messages' car screen only offers the button.

The patched copy is a separate app with its own data. It only receives and shows texts while it is your default SMS app: set it in Android's default apps settings, or when the patched Messages asks. Switching back to the stock Google Messages works the same way.

The patched copy is signed with your Morphe key, not Google's. Expect RCS chat, Google account sign-in, backups and other Google features to be unreliable or unavailable in it. SMS and MMS are expected to keep working. Keep the stock Google Messages installed if you rely on those features.

Hosts that only read message notifications, such as DriveDeck's own Messages screen, work with stock Google Messages as well and need none of these patches.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
