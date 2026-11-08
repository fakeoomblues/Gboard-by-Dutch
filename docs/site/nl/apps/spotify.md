# Spotify

Gebruik de auto-mediabrowser van Spotify met DriveDeck, Gearslip of een andere geschikte Android Auto-host, en verwijder advertentie- en linktrackingoppervlakken. Deze pagina bundelt ook de patches uit de community-set voor Spotify.

**Appversie:** 9.1.94.1929 · **Build:** 147600575 · **Origineel bestand:** APK

[Gebruik je originele APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer de universele originele APK voor deze versie in Morphe Manager.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Android Auto support** | Aan | Maakt de auto-mediabrowser, Home en Bibliotheek bruikbaar met DriveDeck, Gearslip of een andere compatibele Android Auto-host, inclusief de meegeleverde autoservice van Spotify. |
| **Android Auto Up next** | Uit | Publiceert de echte komende nummers zodat de Up next-lijst in de auto gevuld is; optioneel terwijl het starten van de wachtrij wordt gecontroleerd. |
| **Block popup ads** | Aan | Blokkeert volledige promotieschermen bij het openen van de app. |
| **Disable ad breaks** | Uit | Vraagt de advertentiedienst van Spotify de audio-advertentieslots uit te zetten. Experimenteel: het verzoekpad is voor deze versie hersteld, maar echte weergave met een gratis account is niet vastgesteld. |
| **Enable debuggable** | Uit | Markeert de app als debugbaar zodat diagnosetools er zonder root aan kunnen koppelen. Verandert niets aan hoe de app werkt. |
| **Force play order** | Uit | Stuurt shuffle=false zodat albums en afspeellijsten in volgorde spelen; een serverbeperking kan nog gelden. |
| **Hide ad sections** | Aan | Verwijdert merksecties met advertenties van Home en Zoeken. |
| **Hide download buttons** | Aan | Verbergt downloadknoppen op nummers, albums, afspeellijsten en podcasts; downloads en rechten blijven ongewijzigd. |
| **Hide playlist ads** | Aan | Verwijdert de ingebouwde advertentieplaatsing uit playlists. |
| **Hide premium context-menu rows** | Aan | Verwijdert de rijen Download en Start a Jam uit menu's van nummers, albums en afspeellijsten; afspelen, bibliotheek en delen blijven. |
| **Hide premium settings sections** | Aan | Verwijdert de secties Advertenties, Downloadkwaliteit, Downloads en streamen, Offline, Opslag en DJ uit Instellingen. |
| **Hide Premium tab** | Aan | Verwijdert het tabblad Premium uit de onderste navigatie. |
| **Hide shuffle controls** | Aan | Verwijdert de knoppen Shuffle en Smart Shuffle; Spotify Free dwingt nog steeds een geschudde wachtrij af. |
| **Hide video ads** | Aan | Schakelt de videoadvertentie-plugin van de now-playing-weergave uit. |
| **Hide video shelves (safe)** | Aan | Verwijdert video- en kijksecties uit de opgeloste Home-feed. |
| **Remove shuffle restriction** | Aan | Stopt de shuffle-upsell voor Free en laat de app normale afspeelvolgorde en shuffleknoppen gebruiken. |
| **Sanitize sharing links** | Aan | Verwijdert de trackingparameters `si` en `utm_*` uit gedeelde en gekopieerde links. |
| **Show Premium in the account row** | Aan | Toont Premium in plaats van Free in de account- en planrijen; alleen het label verandert. |
| **Skip audio ads** | Aan | Vraagt de speler van Spotify een audioadvertentie over te slaan via zijn eigen volgende-knop, zoekt daarna naar het einde als de speler weigert en dempt pas als laatste redmiddel. Zet "Device broadcast status" in Spotify aan voor betere herkenning. |

De community-set bevatte ook **Hide ad views**, **Hide context menu upsells** en **Diagnose Spotify client**. Hun doelklassen bestaan niet in 9.1.94.1929. Ook de oudere **Fix third party launchers widgets** en **Use the Premium plan icon** zijn voor deze versie verwijderd: de widget-permissiecontrole en de planlogo-renderer bestaan niet meer in 9.1.94.1929. Ze worden niet aangeboden in plaats van de hele patchrun te laten mislukken. **Change package name** wordt niet meer aangeboden: Spotify's inlogcontrole (reCAPTCHA) accepteert alleen de oorspronkelijke pakketnaam, waardoor een hernoemde app niet kan inloggen of een account kan aanmaken.

## Toegang tot de auto-host

Spotify herkent een autoclient aan de pakketnaam en controleert de Car App Library-host op pakket en ondertekeningscertificaat. Alleen Google's Android Auto-pakket en Spotify's eigen mediatest-app komen door die controles. De patch:

- accepteert elke caller bij de Android Auto-rootkeuze, de autoclient-predicate, de mediabrowser-toelating en de drie rootfilters Browse, Recently played en Your Library;
- accepteert elke host in de meegeleverde Car App Library-validator;
- gebruikt de echte Home-feed in plaats van de beperkte terugvaloptie voor een eigen host;
- voegt `QUERY_ALL_PACKAGES` toe zodat Android 11 en nieuwer een willekeurig hostpakket kan opzoeken.

Accounts, catalogus en weergave van Spotify blijven ongewijzigd. De host moet het mediabrowserprotocol zelf ondersteunen.

## Advertentieblokkering

De audioslots zijn `stream` (tussen nummers) en `preroll` (voor het eerste nummer). Spotify zet ze aan via `spotify.ads.esperanto.proto.Settings/updateSlotEnabled`. De extensie stuurt hetzelfde verzoek met `enabled = false` kort nadat het Cosmos-transport bestaat en herhaalt dat, omdat Spotify de slots na account- of netwerkwijzigingen opnieuw aanzet.

De extensie ondersteunt beide request-fabrieken die Spotify gebruikt: de private `access$createRequest`-accessor en de publieke `createRequest`-methode. Een geslaagde patch kan daardoor niet stilletjes alle advertentieverzoeken overslaan.

Een advertentie vervangen door een stream van nul seconden in de native speler zit er niet in. De speler haalt advertenties uit serverdata en de weergave is accountgebonden; zo'n ingreep vraagt een eigen gevalideerde spelerhook.

## Voor gebruik

Meld je op de telefoon aan bij Spotify. Een gepatchte APK heeft een nieuw certificaat en kan niet in-place via de Play Store worden bijgewerkt. Een gratis account krijgt nog serveradvertenties als het slotverzoek niet slaagt; een Premium-account heeft de advertentiepatches niet nodig. In deze build zijn aanmelden, catalogus, weergave en advertentiegedrag niet uitgeoefend; de patchuitvoer is tegen de originele APK gecontroleerd en de app is daaruit herbouwd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
