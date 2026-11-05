# SoundCloud

**Appversie:** 2026.10.28-release · **Build:** 379050 · **Origineel bestand:** APKM

[Gebruik je originele APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige, door de uitgever ondertekende APKM-archief voor precies deze build (Android 12L+), inclusief de apparaatbestanden.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Home in Android Auto** | Aan | Vult de Home-tab in de auto met gewone covertegels uit de telefoon-Home, via het bestaande account van de app. Tracks, mixen en artiesten spelen af; playlists en albums openen. Publiceert de echte afspeelwachtrij voor Up next, met selectie van wachtrij-items. Valt terug op de oorspronkelijke Home van SoundCloud als de telefoon-Home niet laadt. |
| **Disable analytics** | Aan | Stopt het opslaan en versturen van gebeurtenissen via de eigen analytics-handler van SoundCloud. Andere trackingdiensten blijven actief. |
| **Disable consent popup** | Aan | Slaat de OneTrust-pop-up over met de bestaande lege toestemmingscontroller. Geeft geen toestemming en schakelt tracking niet uit; selecteer Disable analytics apart. |
| **Enable SoundCloud Go** | Aan | Activeert lokale Go+-bediening, verbergt Go-aanbiedingen en schermen voor wijzigingen van het luisterabonnement, en schakelt advertentieverzoeken en weergaveplekken uit. Betaalde streams, downloads en audiokwaliteit blijven afhankelijk van servertoegang voor je account. |

## Waarom Home in de auto leeg was

De Home-tab van SoundCloud in de auto leest een oudere ontdekfeed, terwijl de telefoon-Home de nieuwere Home-secties gebruikt. Feed en Bibliotheek werkten al, omdat die je eigen stream en collectie gebruiken. De patch vraagt de telefoonsecties op bij de GraphQL-SDUI-Home-dienst van de app, in plaats van het afzonderlijke bibliotheekbanner-endpoint van de eerdere patch, zodra de auto Home opent. Dit geldt voor Android Auto, Gearslip, DriveDeck en andere mediahosts die SoundCloud doorbladeren.

Muziek-Home gebruikt een coverraster zonder sectiegroepen. Tracks binnen playlists en albums behouden de tracklijst van de app.

## Advertentie- en trackingdiensten

Home in Android Auto wijzigt de Home-inhoud in de auto en publiceert de actieve afspeelwachtrij in de mediasessie. Alleen een werkende knop Volgende levert nog geen titels of afbeeldingen voor Up next. De drie extra opties werken onafhankelijk: Disable analytics stopt de eigen event-handler, Disable consent popup slaat OneTrust over en Enable SoundCloud Go wijzigt lokaal abonnementsgedrag en advertenties. Niet alle trackingdiensten worden verwijderd en er komt geen account bij. Lokale Go+-bediening bewijst geen toegang tot betaalde inhoud op de servers van SoundCloud.

## Voor gebruik

Log op de telefoon in bij SoundCloud. Een gepatchte APK moet opnieuw worden ondertekend, dus de Play Store-installatie kan niet ter plekke worden bijgewerkt; bewaar die tot de gepatchte kopie werkt. Alle drie extra opties bouwen afzonderlijk en samen met Home op exact dit bestand. Hun lokale effecten en het starten van de app zijn op Android zonder account gecontroleerd. Home, covertegels en Up next zijn met een ingelogd account op de telefoon bevestigd na een update ter plekke. Downloads en audiokwaliteit blijven afhankelijk van je account en servertoegang.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md)
