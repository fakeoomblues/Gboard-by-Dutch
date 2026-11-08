# Vela Maps

**Appversie:** 0.5.2621 · **Build:** 46210 · **Origineel bestand:** APK

[Download APK](https://buzzheavier.com/64jger382td4){ .md-button .md-button--primary }

Selecteer de volledige originele APK voor deze exacte versie in Morphe Manager.

Gebruik gepatchte Waze of Flitsmeister als lokale bron voor actuele snelheid, limieten en beschikbare wegwaarschuwingen terwijl Vela je route verzorgt. De bron kan op de achtergrond draaien met de overlay uit.

[Waze of Flitsmeister koppelen aan Google Maps en Vela Maps](../guides/navigation-data.md){ .md-button }

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **External speed controls** | Aan | Voegt De-Waze en Flitsmeister toe als bronnen voor snelheid en wegwaarschuwingen in Vela en Android Auto, met compacte weergave van snelheid, limiet, gedeeltelijk gemiddelde en voorwaarschuwingen. |

## Advertentie- en trackingdiensten

Deze patch voegt geen apart account, advertentiedienst of upload van gebruiksmetingen toe. De gekozen bron blijft zijn eigen kaarten, verkeers-/locatie-uitwisseling en meldingsdiensten gebruiken. Vela behoudt zijn bestaande online- en locatiegedrag.

Wegmeldingen kunnen posities van camera's of incidenten bevatten. De integratie ontvangt die lokaal van de bron op hetzelfde toestel; de apparaatlocatie van Vela wordt niet naar de bron gestuurd.

## Voor gebruik

**Bekende beperking in deze release:** externe waarschuwingskaarten en gesproken meldingen van Waze/Flitsmeister tijdens Android Auto-navigatie zijn nog experimenteel. Werkende navigatie, brongegevens en een geslaagd geparkeerd voorbeeld bewijzen niet dat iedere waarschuwing verschijnt of klinkt. Gebruik de eigen meldingen van de bronapp wanneer u betrouwbare waarschuwingen nodig hebt. Patch de bronapp en ontvangende kaartapp samen opnieuw voor de bijgewerkte verbinding en het afsluitgedrag.

De koppeling met native FM-gebeurtenissen en spraak blijft een ontwikkelfunctie. Flitsmeister 15.0 / 1289 heeft de dekkingscontroles voor modellen en opname doorstaan. Eerdere FM15-ontwikkelversies doorstonden met beide kaartapps gecontroleerde levering, bewegende cameracontroles en trajectcontroles. De huidige kandidaat bevat latere verbeteringen aan tekst, herstel en spraakgereedheid; de uiteindelijke controles van de geïnstalleerde apps, timing en telefoon/auto staan nog open. De categorie-/audiokeuze, het geparkeerde voorbeeld en herstel staan in de [instellingen voor native waarschuwingen](../guides/navigation-data.md#native-waarschuwingsaudio-van-flitsmeister).

Bijgewerkte navigatiegegevenspatches starten de gekozen bron na eenmalige instellingen zonder het bronscherm te openen. Werk voor Flitsmeister ook Maps of Vela bij: met de herstelde overdracht kan een zichtbare kaartapp FM op de achtergrond starten met locatietoegang tijdens appgebruik. Dit is gecontroleerd op een Android 14-emulator; de controle op de Android 16-telefoon staat nog open. Altijd toegestane locatie is geen algemene eis voor deze overdracht. Houd de kaartapp zichtbaar wanneer je FM kiest en controleer de bronstatus. Zie de [installatiegids voor navigatiegegevens](../guides/navigation-data.md).

Patch [een ondersteunde Waze-versie](waze.md) of [Flitsmeister 14.17 of 15.0](flitsmeister.md) met **Broadcast navigation data**, rond de eerste instellingen af en geef de rijdienst eenmaal locatietoegang. Bijgewerkte patches starten de motor wanneer Vela die nodig heeft. De overlay mag uit blijven. De optionele **Receiver package** van de bronpatch kan op `app.vela` worden gezet. Vanaf Android 14 kan Vela de afzender van gedeelde broadcasts via Android controleren; oudere versies bieden die broadcastcontrole niet. Het afzonderlijke kanaal voor native FM-waarschuwingen controleert beide apps via Binder op alle ondersteunde Android-versies.

Open **Snelheid en wegwaarschuwingen** in de navigatie-instellingen van Vela en kies **Vela (eigen gegevens)**, **De-Waze** of **Flitsmeister**. De keuze wordt opgeslagen. Dezelfde keuze staat onder **Navigatieprovider** in de instellingen van Android Auto. **Gesproken wegwaarschuwingen** volgt de stem- en dempinstellingen van Vela; de autodetails hebben een eigen schakelaar.

Op de telefoon toont de compacte kaartweergave met een externe bron gekozen de actuele snelheid naast het snelheidsbord. Bij een actieve trajectcontrole verschijnen daaronder het waargenomen gemiddelde, een voortgangsbalk, percentage en resterende afstand. Het gemiddelde gaat alleen over het waargenomen deel, ook als de bron midden in een traject wordt geopend. Het blijft onbekend totdat genoeg afstand en tijd zijn waargenomen; geschatte voortgang krijgt **≈**. Voorwaarschuwingen gebruiken echte waarnemingen vóór het traject of verse meldingsposities. Beschikbare meldingen omvatten vaste en mobiele flitsers, politie, gevaren, ongevallen, files, afsluitingen en ingeschakelde Flock-camera's van de bron. De dekking hangt af van de bron; geschatte afstanden blijven benaderingen.

Op Android Auto gebruikt de aparte waarschuwingspil het aangeleverde symbool, een normaal gedrukt getal en kleinere eenheden. Vóór een traject toont deze de afstand tot de ingang; binnen het traject het gemiddelde en een onafhankelijke trajectvoortgang. De autopil toont geen waarschuwingsnaam, percentage of bijschrift met resterende afstand; onbekende waarden krijgen **—**. Het actuele snelheids-/limietpaar blijft apart. De nieuwste autopil en de situaties met uitgebreide overlays en andere schermprofielen moeten nog op apparaten worden gecontroleerd.

Ontbrekende of verlopen waarden vallen waar mogelijk terug op de eigen gegevens van Vela. De bronstatus vermeldt die terugval; een onbekende actuele snelheid krijgt **—**. Een bronwissel, het verlaten van een traject of het verlopen van de trajectwaarnemingen verwijdert dat gedeeltelijke gemiddelde. Gegevens van de twee bronnen blijven gescheiden.

Met Flitsmeister blijven Vela's opgeslagen Flock-keuzes voor weergave, spraak en omrijden actief. Algemene FM-gegevens over snelheid, limieten of waarschuwingen vervangen die onafhankelijke functies niet. De bestaande overname door De-Waze geldt alleen zolang de gekozen Waze-bron verse gegevens levert; een niet-geselecteerde bron pauzeert eigen Flock niet.

Eerdere emulatorcontroles omvatten telefooninstellingen, achtergrondontvangst en compacte weergave. Met de oorspronkelijke Android Auto 17-host van Google en de Desktop Head Unit op 1280×480 zijn ook de native kaart, zoeken, routebegeleiding, GPS en navigatiespraak gebruikt. Een A4-GPS-replay met echte FM-gegevens liet trajectvoortgang, het wissen van het traject en afgeronde TTS-meldingen zien. Dat valideert niet de nieuwste numerieke pil, iedere Waze-gebeurtenis, andere schermprofielen of gesproken inhoud in een fysieke auto; die controles staan nog open. Een al gestarte native Flock-verversing kan tijdens de weergave- en waarschuwingspauze aflopen.

[Installeren](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
