# Gboard

**Appversie:** 18.5.11.993608717-release-arm64-v8a · **Build:** 176050094 · **Origineel bestand:** APK

[Gebruik je originele APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer de arm64-v8a-APK van Google voor precies deze build (Android 8.0+).

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Parakeet voice typing** | Aan | Voegt **Parakeet-spraakinvoer** toe aan Gboard-instellingen → Spraakinvoer. Zodra je daar een model downloadt en het aanzet, zet de microfoontoets spraak op de telefoon zelf om in tekst met NVIDIA Parakeet, in plaats van met de spraakherkenning van Google. Elke zin wordt herkend in de taal waarin je hem uitspreekt, dus Nederlands en Engels werken in hetzelfde dictaat zonder te wisselen. Zolang er geen model klaarstaat, gebruikt Gboard gewoon de spraakinvoer van Google. De gepatchte Gboard krijgt het Parakeet-app-icoon. |
| **Change package name** | Aan | Installeert de gepatchte Gboard onder een eigen pakketnaam (standaard app.morphe.android.inputmethod.latin), zodat hij naast de standaard-Gboard staat die op de meeste telefoons is ingebouwd. Je standaard-Gboard, de instellingen en het woordenboek blijven onaangeroerd; kies het gepatchte toetsenbord in de toetsenbordinstellingen van Android. |
| **Custom branding** | Uit | Verandert de appnaam in de launcher, de toetsenbordlijst van Android en de app-informatie, en het app-icoon (standaard Parakeet, het originele Gboard-icoon of vijf andere ontwerpen), zodat je het gepatchte toetsenbord makkelijk onderscheidt van de standaardversie. |

## Een model kiezen

| Model | Download | Het beste voor |
| --- | --- | --- |
| Parakeet Ultra (aanbevolen) | ongeveer 630 MB | Door Moondream verder getrainde Parakeet TDT v3. Nauwkeuriger, vooral bij achtergrondgeluid. |
| Parakeet TDT 0.6B v3 | ongeveer 670 MB | Het oorspronkelijke model van NVIDIA, zoals het sherpa-onnx-project het publiceert. |

Beide modellen verstaan 25 Europese talen, waaronder Nederlands, Engels, Duits, Frans en Spaans, en kiezen per zin zelf de taal. Leestekens en hoofdletters komen er automatisch bij. Bij de eerste download worden ook de spraakruntime en spraakdetectie opgehaald (ongeveer 40 MB). Elk bestand wordt met een vaste controlesom gecontroleerd voordat het wordt gebruikt. Daarna werkt spraakinvoer offline en verlaat je geluid de telefoon niet.

## Dicteren

Tik op de microfoontoets en praat. De tekst verschijnt terwijl je praat en staat vast zodra je pauzeert. Het dictaat stopt na vijf seconden stilte of als je opnieuw op de microfoontoets tikt. Gboard vraagt de eerste keer zoals gewoonlijk om toegang tot de microfoon.

## Voor gebruik

- Een telefoon met 64-bit ARM is nodig. Houd tijdens het downloaden ongeveer 1,4 GB vrij; het geïnstalleerde model neemt ongeveer 650 MB in beslag.
- Tijdens het dicteren gebruikt het model ongeveer 700 MB geheugen. Na vijf minuten zonder gebruik wordt het weer vrijgegeven, dus de eerste zin na een pauze duurt even iets langer.
- Met Change package name aan is de gepatchte Gboard een aparte app met eigen instellingen: zet hem aan via Android-instellingen → Toetsenbord en kies hem daarna. Zonder die patch kan hij niet worden geïnstalleerd op telefoons waarop Gboard is ingebouwd. Functies die afhangen van de eigen app-handtekening van Google werken in de gepatchte versie mogelijk niet; Parakeet heeft ze niet nodig.
- Gecontroleerd op een Android 14-emulator: de instelling, de download en Nederlands, Engels en gemengd Nederlands-Engels dicteren in een tekstveld, plus stilte en achtergrondgeluid zonder losse tekst. Dicteren op een telefoon is nog niet bevestigd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md)
