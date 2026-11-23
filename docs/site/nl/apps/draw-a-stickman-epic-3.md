# Draw a Stickman: EPIC 3

Speel het spel op het Android Auto-autoscherm en op carhosts zoals DriveDeck en Gearslip.

**Appversie:** 1.13.19873 · **Build:** 19873 · **Origineel bestand:** APKS

[Gebruik je originele APKS](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige originele APKS-archief in Morphe Manager, inclusief de basis- en architectuur-APK's.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Hide ads** | Aan | Houdt de advertentie-SDK tegen: de initialisatie rondt lokaal af, elk advertentieverzoek meldt de eigen no-fill-fout van de SDK en elke weergave meldt not-ready, zodat er geen advertentienetwerk of mediation-adapter wordt benaderd. |
| **Disable tracking** | Aan | Verwijdert de machtigingen voor advertentie-ID en install-referrer, stopt de opstartstarters van AppLovin en IronSource en schakelt de advertentie-ID en gebeurtenisupload van Facebook uit. |

## Voor gebruik

Dit spel declareert zichzelf al als geparkte app: de launcher-activity draagt `android.intent.category.CAR_LAUNCHER` en de app declareert de gamecategorie, dus carhosts die Googles gedocumenteerde detectie volgen tonen het spel zonder manifestpatch. Alleen de advertentie- en trackingpatches worden hier aangeboden.

Beloningen die een advertentie vereisen worden niet meer aangeboden, omdat er nooit een gevulde advertentie is. Tekenfuncties, eerdere aankopen en voortgang blijven werken.

Gecontroleerd met build-, patchsessie- en manifestcontroles tegen de originele APKS. Er is geen playsessie op het autoscherm uitgevoerd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardinstellingen](../reference/catalogue.md)
