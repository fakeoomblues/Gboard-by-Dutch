# Flitsmeister

**Appversie:** 14.17 · **Build:** 1287 · **Origineel bestand:** APKS

[Download originele app](https://buzzheavier.com/87mpz44q4p8h){ .md-button .md-button--primary }

[Flitsmeister installeren voor Android Auto: stappen met screenshots](../guides/android-auto.md){ .md-button }

Selecteer het exacte originele bestand in Morphe Manager. Gebruik bij een gesplitste app het volledige archief.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Broadcast navigation data** | Aan | Deelt actuele snelheid, geldende snelheidslimieten en waargenomen trajectvoortgang/gemiddelde via hetzelfde lokale broadcastcontract als Waze. Geen overlay nodig. |
| **Disable partner services** | Aan | Verwijdert Parkeren, Tol en Carwash en de bijbehorende instellingen. |
| **Disable tracking** | Aan | Stopt de ondersteunde optionele gebruiksstatistieken, advertentie-ID’s en diagnostiek. |
| **Fix PairIP startup** | Aan | Laat de gepatchte app na installatie starten en de ondersteunde functies gebruiken. Laat deze patch aan. |
| **Hide Pro notices** | Aan | Verbergt automatische Pro/Plus-aanbiedingen, badges en Android Auto-upgradeberichten. |
| **Hide ads** | Aan | Verwijdert commerciële locatiepromoties, hun afbeeldingen en reclamemeldingen. Verkeerswaarschuwingen blijven. |
| **Unlock Pro** | Aan | Schakelt lokaal geregelde Pro-opties in. Functies waarvoor accountactivering nodig is blijven afhankelijk van de dienst. |

## Advertentie- en trackingdiensten

De advertentie- en trackingpatches beperken de ondersteunde optionele activiteit van: Firebase, Insider; automatische gebruiks- en locatiemetingen; commerciële locatiegebonden advertenties.

Metingen blokkeren blokkeert niet elke functie van dezelfde aanbieder. Noodzakelijke appdiensten kunnen nog verbinding maken.

## Voor gebruik

**Broadcast navigation data** werkt zolang de rij-/locatiedienst met locatietoegang op de achtergrond draait. De overlay mag uit blijven. Ontvangende apps gebruiken dezelfde actie en velden voor Flitsmeister en Waze; gemiddelden gaan alleen over het waargenomen deel en berekende voortgang wordt als schatting gemarkeerd. [Gedeeld broadcastcontract](https://github.com/johninnl/nl-morphe-patches/blob/main/docs/navigation-broadcasts.md).

Navigatie, live verkeers-/locatie-uitwisseling, accounts en meldingen gebruiken nog steeds online diensten. Unlock Pro activeert geen serverdiensten. Volg voor Android Auto de [KingInstaller-installatiestappen](../guides/android-auto.md).

[Installeren](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
