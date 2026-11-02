# SoundCloud

**Appversie:** 2026.10.28-release · **Build:** 379050 · **Origineel bestand:** APKM

[Gebruik je originele APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige, door de uitgever ondertekende APKM-archief voor precies deze build (Android 12L+), inclusief de apparaatbestanden.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Home in Android Auto** | Aan | Vult de Home-tab in de auto met dezelfde Home-secties als de telefoonapp, gegroepeerd op sectietitel, via het bestaande account van de app. Tracks, mixen en artiesten spelen af; playlists en albums openen. Valt terug op de oorspronkelijke Home van SoundCloud als de telefoon-Home niet laadt. |

## Waarom Home in de auto leeg was

De Home-tab van SoundCloud in de auto leest een oudere ontdekfeed, terwijl de telefoon-Home de nieuwere Home-secties gebruikt. Feed en Bibliotheek werkten al, omdat die je eigen stream en collectie gebruiken. De patch vraagt de telefoonsecties op bij de eigen Home-dienst van de app zodra de auto Home opent. Dit geldt voor Android Auto, Gearslip, DriveDeck en andere mediahosts die SoundCloud doorbladeren.

## Advertentie- en trackingdiensten

Deze compatibiliteitspatch wijzigt alleen de Home-inhoud in de auto. Online diensten blijven, er komt geen account bij en abonnementstoegang verandert niet.

## Voor gebruik

Log op de telefoon in bij SoundCloud. Een gepatchte APK moet opnieuw worden ondertekend, dus de Play Store-installatie kan niet ter plekke worden bijgewerkt; bewaar die tot de gepatchte kopie werkt. De patch is gecontroleerd op exact dit bestand: patchen en opnieuw bouwen lukt, de gepatchte app start en de Home-aanvraag loopt op Android via de eigen dienst van de app. Bladeren en afspelen in de auto met een ingelogd account worden na installatie op een toestel bevestigd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md)
