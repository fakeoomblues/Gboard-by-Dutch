# Gboard

**Appversie:** 18.6.3.1002251336-release-arm64-v8a · **Build:** 176071766 · **Origineel bestand:** APK

[Gebruik je originele APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer de arm64-v8a-APK van Google voor precies deze build (Android 12L+).

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Parakeet voice typing** | Aan | Voegt **Parakeet-spraakinvoer** toe aan Gboard-instellingen → Spraakinvoer. Zodra je daar een model downloadt en het aanzet, zet de microfoontoets spraak op de telefoon zelf om in tekst met NVIDIA Parakeet, in plaats van met de spraakherkenning van Google. Elke zin wordt herkend in de taal waarin je hem uitspreekt, dus Nederlands en Engels werken in hetzelfde dictaat zonder te wisselen. Zolang er geen model klaarstaat, gebruikt Gboard gewoon de spraakinvoer van Google. De gepatchte Gboard krijgt het Parakeet-app-icoon. |
| **Rambler voice typing** | Uit | Voegt Rambler van Google toe aan Gboard-instellingen → Spraakinvoer, als keuze naast Standaard. Kies je **Rambler**, dan stuurt de microfoontoets je spraak naar de servers van Google. Die maken er afgewerkte tekst van en voeren gesproken bewerkingen uit, zoals "verwijder de laatste zin". Standaard blijft de standaardkeuze; met Parakeet voice typing erbij blijft Standaard Parakeet gebruiken. |
| **Change package name** | Aan | Installeert de gepatchte Gboard onder een eigen pakketnaam (standaard app.morphe.android.inputmethod.latin), zodat hij naast de standaard-Gboard staat die op de meeste telefoons is ingebouwd. Je standaard-Gboard, de instellingen en het woordenboek blijven onaangeroerd; kies het gepatchte toetsenbord in de toetsenbordinstellingen van Android. |
| **Custom branding** | Uit | Verandert de appnaam in de launcher, de toetsenbordlijst van Android en de app-informatie, en het app-icoon (standaard Parakeet, het originele Gboard-icoon of vijf andere ontwerpen), zodat je het gepatchte toetsenbord makkelijk onderscheidt van de standaardversie. |
| **Clipboard Enhancements** | Aan | Bepaalt hoe lang gekopieerde fragmenten bewaard blijven, hoeveel er worden bewaard, hoeveel voorbeeldregels een kaart toont en of kaarten een aftelling, aanmaaktijd, volgnummer en kolomaantal tonen. |
| **Web Clipboard** | Aan | Host een klein Web Clipboard-portaal op de telefoon, zodat een desktopbrowser op hetzelfde lokale netwerk clipboardtekst met Gboard kan uitwisselen. Het portaal is beveiligd met een koppelcode en is bereikbaar via een Quick Settings-tegel. |
| **Toolbar Editing Buttons** | Aan | Voegt Alles selecteren, Kopiëren, Knippen en Plakken toe aan het Gboard-menu, zodat je ze naar de bovenste werkbalk kunt slepen en in ondersteunde toetsenbordtalen en editors kunt gebruiken. |
| **Cursor trackpad mode** | Aan | Houd de spatiebalk lang ingedrukt om de cursor nauwkeuriger te verplaatsen met een trackpad en de vergrendelmodus te gebruiken. |
| **Incognito Mode Toggle** | Aan | Voegt een incognito-schakelaar toe aan de Access Point-werkbalk en laat je kiezen of het clipboard en spraakinvoer beschikbaar blijven terwijl incognito actief is. Dit regelt de incognito-status van Gboard zelf; het is geen netwerkbrede anti-trackingfunctie. |
| **Simple Calculator** | Aan | Typ een rekenkundige expressie in een tekstveld en zie een scrollbare vergelijking met het antwoord in de suggestieregel van Gboard, klaar om in te voegen. |

Clipboard Enhancements, Web Clipboard, Toolbar Editing Buttons, Cursor trackpad mode, Incognito Mode Toggle en Simple Calculator zijn overgenomen van het GPLv3-project [Gboard Patches](https://github.com/jasonwu1994/Gboard-patches) en bijgewerkt voor Gboard 18.6.3.

## Een model kiezen

| Model | Download | Het beste voor |
| --- | --- | --- |
| Parakeet Ultra (aanbevolen) | ongeveer 630 MB | Door Moondream verder getrainde Parakeet TDT v3. Nauwkeuriger, vooral bij achtergrondgeluid. |
| Parakeet TDT 0.6B v3 | ongeveer 670 MB | Het oorspronkelijke model van NVIDIA, zoals het sherpa-onnx-project het publiceert. |

Beide modellen verstaan 25 Europese talen, waaronder Nederlands, Engels, Duits, Frans en Spaans, en kiezen per zin zelf de taal. Leestekens en hoofdletters komen er automatisch bij. Bij de eerste download worden ook de spraakruntime en spraakdetectie opgehaald (ongeveer 40 MB). Elk bestand wordt met een vaste controlesom gecontroleerd voordat het wordt gebruikt. Daarna werkt spraakinvoer offline en verlaat je geluid de telefoon niet.

## Dicteren

Tik op de microfoontoets en praat. De tekst verschijnt terwijl je praat en staat vast zodra je pauzeert. Het dictaat stopt na vijf seconden stilte of als je opnieuw op de microfoontoets tikt. Gboard vraagt de eerste keer zoals gewoonlijk om toegang tot de microfoon.

## Rambler voice typing

Rambler is de eigen spraakinvoer van Google met Gemini. Rambler schrijft niet elk woord letterlijk uit, maar maakt van wat je zegt afgewerkte tekst en voert gesproken correcties uit. Google zet Rambler alleen aan op een deel van de Pixel-telefoons; deze patch biedt het ook op andere telefoons aan.

Open na het patchen Gboard-instellingen → Spraakinvoer en kies **Rambler** of **Standaard**. Standaard blijft gekozen tot je dat zelf verandert.

- Rambler heeft een internetverbinding nodig en stuurt je spraak naar Google. Google beperkt hoeveel je per dag kunt dicteren.
- Parakeet regelt alleen de standaard-spraakinvoer. Kan Rambler Google niet bereiken, dan valt Gboard terug op zijn eigen standaardherkenning van Google, niet op Parakeet.
- Of de servers van Google Rambler accepteren op een telefoon buiten hun uitrol, beslist Google. Andere Gboard-mods melden dat het werkt; met deze patches is het nog niet op een telefoon bevestigd.

## Voor gebruik

- Een telefoon met 64-bit ARM is nodig. Houd tijdens het downloaden ongeveer 1,4 GB vrij; het geïnstalleerde model neemt ongeveer 650 MB in beslag.
- Tijdens het dicteren gebruikt het model ongeveer 700 MB geheugen. Na vijf minuten zonder gebruik wordt het weer vrijgegeven, dus de eerste zin na een pauze duurt even iets langer.
- Met Change package name aan is de gepatchte Gboard een aparte app met eigen instellingen: zet hem aan via Android-instellingen → Toetsenbord en kies hem daarna. Zonder die patch kan hij niet worden geïnstalleerd op telefoons waarop Gboard is ingebouwd. Functies die afhangen van de eigen app-handtekening van Google werken in de gepatchte versie mogelijk niet; Parakeet heeft ze niet nodig.
- De tien Gboard-patches zijn gecontroleerd op bouwen en toepassen op deze 18.6.3-build. Toepassen is niet hetzelfde als elke functie op een toestel testen.
- Gecontroleerd op een Android 14-emulator: de instelling, de download en Nederlands, Engels en gemengd Nederlands-Engels dicteren in een tekstveld, plus stilte en achtergrondgeluid zonder losse tekst. Dicteren op een telefoon is nog niet bevestigd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md)
