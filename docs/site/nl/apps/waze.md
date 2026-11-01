# Waze

**Appversie:** 5.26.90.901 · **Build:** 1030746 · **Origineel bestand:** APKM

[Download originele app](https://buzzheavier.com/cpwy7pirf997){ .md-button .md-button--primary }

Gebruik het volledige originele APKM-archief voor deze versie in Morphe Manager.

[Waze installeren voor Android Auto: stappen met screenshots](../guides/android-auto.md){ .md-button }

## Waar deze patches vandaan komen

24 van deze 29 patches komen uit [bearinmind patches](https://github.com/bearinmindcat/morphe-patches) van bearinmindcat, waar ze als De-Waze voor Waze 5.24.5.0 worden uitgebracht. Dit project heeft ze overgezet naar 5.26.90.901 en onderhoudt die versie zelf. De andere vijf patches en de extra's hieronder komen van dit project, vergeleken met bearinmind patches v2.8.3.

| Onderdeel | Toegevoegd door dit project |
| --- | --- |
| Advertenties | **Hide ads** verwijdert ook gesponsorde zoekresultaten op de telefoon en in de auto, het advertentietoken en de Adspam-cookie bij zoekopdrachten en promoties in het hoofdmenu, ook als Waze die al had opgeslagen. |
| Telemetrie | **Remove telemetry** stopt ook het ophalen van de advertentie-ID en verbergt een opgeslagen ID, gooit gebruiksgegevens weg die al klaarstonden om te versturen, blokkeert automatische diagnoselogs en zet Firebase Sessions uit. Je kunt logs nog steeds zelf versturen. |
| Toestemming | Nieuw: **Skip consent screens** beantwoordt privacy- en voorwaardenvragen zonder scherm en weigert gepersonaliseerde advertenties. |
| Muziek | Nieuw: **SoundCloud in Waze** en **YT Music Morphe in Waze** voegen beide diensten toe aan de ingebouwde audiospeler. |
| Andere apps | Nieuw: **Broadcast navigation data** deelt snelheids- en trajectgegevens met andere apps en met **Support Gearslip** kan de officiële Gearslip-host Waze draaien. |
| Waarschuwingen delen | **Share alerts with Maps** stopt nu direct als je het uitzet en wist waarschuwingen die al gedeeld waren. |

## Patches

### Toegevoegd door dit project

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Broadcast navigation data** | Aan | Deelt de actuele snelheid, native snelheidslimiet en waargenomen voortgang/gemiddelde snelheid in een trajectcontrole met andere apps, ook als Waze op de achtergrond draait. Geen overlay nodig. |
| **Skip consent screens** | Aan | Rondt privacy- en voorwaardenkeuzes zonder scherm af en weigert gepersonaliseerde advertenties. Lees de toelichting hieronder. |
| **SoundCloud in Waze** | Aan | Voegt SoundCloud toe aan de ingebouwde audiospeler. Show list toont de ingelogde Feed en afspeelbare mixes, niet je persoonlijke bibliotheek. |
| **Support Gearslip** | Aan | Voegt het pakket en ondertekeningscertificaat van de officiële Gearslip-release toe aan de hostlijst en behoudt de Android Auto-validatie. |
| **YT Music Morphe in Waze** | Aan | Voegt YT Music Morphe toe aan de ingebouwde audiospeler. Show list toont ondersteunde Home-aanbevelingen en mixes, niet je persoonlijke bibliotheek. |

### Uit bearinmind patches, hier uitgebreid

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Hide ads** | Aan | Verwijdert gesponsorde kaartpictogrammen, advertenties bij stilstand en bijbehorende advertentietracking, plus gesponsorde zoekresultaten en promoties in het hoofdmenu. |
| **Remove telemetry** | Aan | Stopt de ondersteunde crashrapporten, gebruiksstatistieken en prestatiegegevens, en blokkeert ook het ophalen van de advertentie-ID, wachtende gebruiksgegevens en automatische diagnoselogs. Verkeersmeldingen blijven werken. |
| **Share alerts with Maps** | Aan | Deelt Waze-waarschuwingen met Ungoogled Maps op dezelfde telefoon. Waze moet open zijn en je moet ingelogd zijn. Zet je dit uit, dan stopt het delen direct. |

### Uit bearinmind patches, overgezet naar 5.26.90.901

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Alert distances** | Aan | Stelt in hoe vroeg een waarschuwing wordt uitgesproken. |
| **All moods** | Uit | Maakt alle stemmingen beschikbaar, ook bètastemmingen. |
| **Average speed zone bar** | Aan | Toont voortgang en je gemiddelde snelheid bij trajectcontrole. Zet dit na het patchen aan in Customization. |
| **Bigger alert pins** | Uit | Vergroot waarschuwingspictogrammen op de kaart. |
| **Bigger speedometer** | Aan | Vergroot de cijfers van de snelheidsmeter en maximumsnelheid. |
| **Change app name** | Aan | Verandert de appnaam, standaard in De-Waze. Die naam zie je ook in Android Auto. |
| **Flock cameras** | Aan | Geeft een geluid bij een naderende Flock-camera uit OpenStreetMap. Voegt geen kaartpictogrammen of routes toe. Zet dit aan in Customization. |
| **Full black** | Aan | Maakt de donkere weergave zwart voor AMOLED-schermen. |
| **Map skin** | Aan | Vergroot kaartlabels en de navigatiepijl en geeft de dagkaart een warme achtergrond. |
| **Navigation distances** | Uit | Past aankondigingsafstanden in Android Auto en de drempel voor de verkeersbalk aan. |
| **No AI reporting** | Uit | Zet spraakmeldingen via Gemini uit. |
| **No Google Assistant** | Uit | Verwijdert Google Assistant uit Waze. |
| **Open without device location** | Uit | Laat Waze openen als locatie uitstaat. Navigatie heeft nog steeds locatietoegang nodig. |
| **Remove permissions** | Aan | Verwijdert toegang tot contacten, agenda, telefoon, camera en advertentie-ID. |
| **Remove sign-in prompts** | Aan | Verbergt Wazes herinnering om je account te registreren. |
| **Report speed limit** | Uit | Maakt het aanpassen van de maximumsnelheid via de snelheidsmeter beschikbaar. De werking is niet bevestigd. |
| **Speed camera alerts** | Aan | Geeft een geluid bij een naderende flitspaal uit OpenStreetMap. Voegt geen kaartpictogrammen of routes toe. Zet dit aan in Customization. |
| **Speed camera sound at any speed** | Aan | Laat het flitspaalgeluid ook horen wanneer je niet te hard rijdt. |
| **Speed cameras everywhere** | Uit | Maakt flitspaal- en politiewaarschuwingen in elk land beschikbaar. Controleer voor vertrek de plaatselijke regels. |
| **Speed limit sign** | Uit | Gebruikt in elk land dezelfde stijl voor het snelheidslimietbord. |
| **Stay awake in the background** | Uit | Houdt Waze zonder actieve route op de achtergrond open om waarschuwingen met Maps te delen. Dit gebruikt meer batterij. |

## Advertentie- en trackingdiensten

Hide ads en Remove telemetry beperken de ondersteunde advertenties, crashrapporten en gebruiksmetingen. Waze gebruikt nog steeds online diensten voor kaarten, routes, live verkeer, accounts en meldingen. Deze patches maken navigatie niet anoniem.

## Voor gebruik

Open na het patchen **Customization** in het zijmenu onder **Shut off**. Daar stel je de toegepaste functies in. Speed camera alerts, Flock cameras en Average speed zone bar staan daar eerst uit, ook als je de patches hebt geselecteerd. Herstart Waze na wijzigingen aan de kaartstijl of waarschuwingspictogrammen.

**Broadcast navigation data** gebruikt Wazes native engine en locatietoegang op de achtergrond, zonder zichtbare telefoonactiviteit. De achtergrondoptie wordt automatisch meegenomen (standaard 180 minuten, instelbaar). Gemiddelden gelden voor het waargenomen deel van een trajectcontrole; geschatte voortgang en ontbrekende/verouderde waarden worden apart aangeduid. [Broadcastprotocol](https://github.com/johninnl/nl-morphe-patches/blob/main/docs/navigation-broadcasts.md). **Support Gearslip** vertrouwt het gecontroleerde releasecertificaat; weergave in een echte auto moet apart worden getest.

**Skip consent screens** maakt privacy- en voorwaardenkeuzes zonder die schermen te tonen. Gepersonaliseerde advertenties worden geweigerd; verplichte voorwaarden worden geaccepteerd. Laat deze patch uit als je de keuzes zelf wilt lezen en maken. Remove telemetry en Remove permissions beperken de genoemde gegevens; noodzakelijke navigatiediensten blijven actief.

Voor Android Auto volg je de [KingInstaller-stappen met screenshots](../guides/android-auto.md). Door **Change app name** kan Waze als **De-Waze** in de applijst staan. De twee audiopatches zijn niet aan één versie gebonden, maar de overige patches op deze pagina richten zich op 5.26.90.901. Beschikbare audio-inhoud hangt af van je muziekapp en account. Routebegeleiding, waarschuwingen op de weg en elke auto-opstelling zijn hiermee niet volledig getest. Report speed limit is nog onbevestigd.

[Installeren](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md#waze)
