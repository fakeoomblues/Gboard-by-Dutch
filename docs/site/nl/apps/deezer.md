# Deezer

Gebruik de auto-mediabrowser van Deezer met DriveDeck, Gearslip of een andere geschikte Android Auto-host.

**Appversie:** 9.0.26.3 · **Build:** 9002603 · **Origineel bestand:** APKM

[Gebruik je originele APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige originele archief voor deze versie in Morphe Manager, inclusief de architectuur-, taal- en scherm-splits.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Allow custom Android Auto hosts** | Aan | Laat de auto-mediabrowser van Deezer een externe Android Auto-host accepteren in plaats van hem via de lijst met bekende hostcertificaten te weigeren. |

## Waarom de auto leeg bleef

De mediabrowserservice van Deezer controleert pakket en ondertekeningscertificaat van de caller en zet de browserroot op null als die niet vertrouwd is. De patch behoudt de callerlookup en de rootopbouw en slaat alleen die afwijzingsbranch over. `QUERY_ALL_PACKAGES` wordt toegevoegd zodat Android 11 en nieuwer een willekeurig hostpakket kan vinden.

Account, catalogus en weergave van Deezer blijven ongewijzigd.

## Voor gebruik

Meld je eerst op de telefoon aan bij Deezer. Een gepatchte APK heeft een nieuw certificaat en kan niet in-place via de Play Store worden bijgewerkt.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
