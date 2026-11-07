# Myspoti

**Appversie:** 2.4.0 · **Build:** 1142 · **Origineel bestand:** APK

[Gebruik je originele APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer de door de uitgever ondertekende Myspoti-APK. **Custom branding** en **Gearslip personal library** zijn compatibel met elke versie waarvan de bestaande toegangsstructuur van de muziekbrowser eenduidig wordt herkend; bij een onbekende of dubbelzinnige structuur stopt het patchen. **Android Auto Up next** accepteert 2.3.0 / 1121 en 2.4.0 / 1142. De versie hierboven is het referentiebestand. Myspoti wordt ontwikkeld door [kota-zo/myspoti](https://github.com/kota-zo/myspoti).

[Muziekspelers en persoonlijke bibliotheken in Gearslip instellen](../guides/gearslip-music.md){ .md-button }

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Custom branding** | Uit | Stelt een eigen appnaam in en kiest een van zes meegeleverde launchericonen in Spotify-stijl, of behoudt het originele icoon. |
| **Gearslip personal library** | Aan | Laat de ingestelde Gearslip-host de bestaande Home, bibliotheek en downloads van Myspoti bekijken, zoeken en muziek kiezen om af te spelen. |
| **Android Auto Up next** | Uit | Op 2.4.0 exporteert Myspoti de echte huidige en komende wachtrij van afspeellijsten/radio met metadata en selectie al zelf; deze patch maakt die beschikbaar voor elk Android Auto-systeem, of alleen voor de hostpakketten die je opgeeft. Op 2.3.0 exporteert de patch de wachtrij ook. |

## Advertentie- en trackingdiensten

Deze compatibiliteitspatches veranderen de toegang tot de muziekbrowser en de beslissing welke autohost mag verbinden. Ze verwijderen geen onlinediensten, leveren geen account en veranderen geen abonnementsrechten.

## Voor gebruik

Myspoti 2.4.0 publiceert zijn eigen wachtrij naar de mediasessie, dus **Android Auto Up next** dupliceert die niet: de patch haalt de oorspronkelijke beperking voor autohosts weg. Laat **Allowed host packages** leeg om elk Android Auto-systeem toe te laten, inclusief externe hosts zoals DriveDeck en Gearslip. Voer om de toegang te beperken een of meer Android-apppakketten in, gescheiden door komma's, bijvoorbeeld `app.johninnl.drivedeck`; elke andere app mag niet bladeren. Voeg `com.google.android.projection.gearhead` toe wanneer ook Google's Android Auto toegestaan moet blijven. Patch opnieuw om deze keuze te wijzigen.

**Gearslip personal library** blijft een aparte lijst met vertrouwde hosts en behoudt Myspoti's UID-controle. Standaard zijn dat de Gearslip- en DriveDeck-pakketten, inclusief hun `.dev`-versies. Als beide patches aan staan, wordt de Gearslip-lijst eerst toegelaten en geldt de Android Auto-keuze voor elke andere aanroeper. Voeg alleen een ander hostpakket toe voor een versie die je bewust gebruikt.

Schakel op 2.3.0 / 1121 **Android Auto Up next** in om maximaal 50 echte wachtrij-items inclusief het huidige item te publiceren en komende items via Myspoti's bestaande wachtrijhandler te kiezen. De gebruiker heeft bevestigd dat de ontbrekende Up next-weergave op de telefoon is opgelost na installatie van de ondertekende APK als update, met behoud van appgegevens en de bestaande Spotify-naam en het icoon.

Schakel **Custom branding** in om **App name** en de keuzelijst **App icon** te gebruiken. De keuzes zijn Original, Classic Spotify Green, Elegant dark Spotify, Emerald Spotify on pitch black, Dark emerald Spotify glass, Platinum Spotify on charcoal en Black Spotify on warm white. Een lege naam behoudt Myspoti. Naam en icoon zijn onafhankelijk instelbaar; voor een wijziging moet de APK opnieuw worden gepatcht. Branding kan samen met de andere twee patches uit dezelfde bundel worden gekozen. De patch verandert de Android-appnaam en het launchericoon; logo’s in de app, het opstartscherm, notificatieafbeeldingen en accounttoegang blijven zoals Myspoti ze levert.

Log in bij Myspoti voordat je de auto test. Een gepatchte APK moet opnieuw worden ondertekend. De door de uitgever ondertekende installatie kan niet met een andere sleutel worden bijgewerkt. Bewaar deze tijdens het testen van een aparte kopie; verwijder de installatie niet alleen om deze patch te proberen.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md)
