# Spotify

Gebruik de auto-mediabrowser van Spotify met DriveDeck, Gearslip of een andere geschikte Android Auto-host, en verwijder advertentie- en linktrackingoppervlakken. Deze pagina bundelt ook de patches uit de community-set voor Spotify.

**Appversie:** 9.2.0.26 · **Build:** 148373686 · **Origineel bestand:** APKM

[Gebruik je originele APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige originele archief voor deze versie in Morphe Manager, inclusief de architectuur-, taal- en scherm-splits.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Allow custom Android Auto hosts** | Aan | Laat de auto-mediabrowser en de meegeleverde autoservice van Spotify elke externe host zoals DriveDeck of Gearslip accepteren, inclusief de roots Browse, Recently played en Your Library. |
| **Block popup ads** | Aan | Blokkeert volledige promotieschermen bij het openen van de app. |
| **Change package name** | Uit | Wijzigt pakketnaam en appnaam zodat de gepatchte app naast het origineel kan staan. |
| **Disable ad breaks** | Uit | Vraagt de advertentiedienst van Spotify de audio-advertentieslots uit te zetten. Experimenteel: het verzoekpad is voor deze versie hersteld, maar echte weergave met een gratis account is niet vastgesteld. |
| **Fix third party launchers widgets** | Aan | Maakt de Spotify-widgets toevoegbaar in externe launchers. |
| **Hide Premium tab** | Aan | Verwijdert het tabblad Premium uit de onderste navigatie. |
| **Hide ad sections** | Aan | Verwijdert merksecties met advertenties van Home en Zoeken. |
| **Hide playlist ads** | Aan | Verwijdert de ingebouwde advertentieplaatsing uit playlists. |
| **Hide video ads** | Aan | Schakelt de videoadvertentie-plugin van de now-playing-weergave uit. |
| **Skip audio ads** | Aan | Vraagt de speler van Spotify een audioadvertentie over te slaan via zijn eigen volgende-knop, zoekt daarna naar het einde als de speler weigert en dempt pas als laatste redmiddel. Zet "Device broadcast status" in Spotify aan voor betere herkenning. |
| **Sanitize sharing links** | Aan | Verwijdert de trackingparameters `si` en `utm_*` uit gedeelde en gekopieerde links. |

De community-set bevatte ook **Hide ad views**, **Hide context menu upsells** en **Diagnose Spotify client**. Hun doelklassen bestaan niet meer in 9.2.0.26; ze worden hier niet aangeboden in plaats van de hele patchrun te laten mislukken.

## Toegang tot de auto-host

Spotify herkent een autoclient aan de pakketnaam en controleert de Car App Library-host op pakket en ondertekeningscertificaat. Alleen Google's Android Auto-pakket en Spotify's eigen mediatest-app komen door die controles. De patch accepteert elke caller bij de Android Auto-rootkeuze, de gedeelde autoclient-predicate en de drie rootfilters Browse, Recently played en Your Library; geeft de meegeleverde allow-all-validator terug voor Spotify's Car App Library-service; en voegt `QUERY_ALL_PACKAGES` toe zodat Android 11 en nieuwer een willekeurig hostpakket kan opzoeken.

Accounts, catalogus en weergave van Spotify blijven ongewijzigd. De host moet het mediabrowserprotocol zelf ondersteunen.

## Advertentieblokkering

De audioslots zijn `stream` (tussen nummers) en `preroll` (voor het eerste nummer). Spotify zet ze aan via `spotify.ads.esperanto.proto.Settings/updateSlotEnabled`. De extensie stuurt hetzelfde verzoek met `enabled = false` kort nadat het Cosmos-transport bestaat en herhaalt dat, omdat Spotify de slots na account- of netwerkwijzigingen opnieuw aanzet.

De 9.2-build heeft de request-factory als publieke `createRequest`-methode; oudere builds gebruikten de private `access$createRequest`-accessor. De extensie ondersteunt beide, zodat een geslaagde patch niet stilletjes alle advertentieverzoeken kan overslaan.

Een advertentie vervangen door een stream van nul seconden in de native speler zit er niet in. De speler haalt advertenties uit serverdata en de weergave is accountgebonden; zo'n ingreep vraagt een eigen gevalideerde spelerhook.

## Voor gebruik

Meld je op de telefoon aan bij Spotify. Een gepatchte APK heeft een nieuw certificaat en kan niet in-place via de Play Store worden bijgewerkt. Een gratis account krijgt nog serveradvertenties als het slotverzoek niet slaagt; een Premium-account heeft de advertentiepatches niet nodig. In deze build zijn aanmelden, catalogus, weergave en advertentiegedrag niet uitgeoefend; de patchuitvoer is tegen de originele APK gecontroleerd en de app is daaruit herbouwd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
