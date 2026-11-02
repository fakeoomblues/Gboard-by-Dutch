# Meldingen van Waze en Flitsmeister in andere kaartapps

Gebruik Google Maps of Vela Maps voor navigatie terwijl gepatchte Waze of Flitsmeister op de achtergrond snelheid, maximumsnelheden en beschikbare wegmeldingen levert. De apps delen die waarnemingen lokaal op dezelfde telefoon. Zowel de bron als de ontvangende kaartapp moet worden gepatcht.

## Wat de koppeling toevoegt

| Gegeven | Wat de ontvangende kaart kan tonen |
| --- | --- |
| Actuele snelheid en maximumsnelheid | Een compacte snelheidsweergave naast een snelheidsbord, met verse bronwaarden en waar mogelijk terugval op eigen gegevens. |
| Trajectcontroles | Het waargenomen gemiddelde, de voortgang en resterende afstand tijdens een actief traject. Beschikbare waarnemingen vóór de ingang kunnen voorwaarschuwingen geven. |
| Wegmeldingen | Beschikbare vaste en mobiele flitsers, politie, gevaren, ongevallen, files en afsluitingen. Ingeschakelde Waze-cameralagen kunnen hun waarnemingen ook delen. De dekking hangt af van de bron. |

Een gemiddelde gaat alleen over het deel van een traject dat de bron heeft waargenomen. Halverwege starten haalt het eerdere deel niet terug. Geschatte afstanden en voortgang blijven benaderingen; een melding in de buurt ligt niet automatisch op je gekozen route.

Deze navigatiegegevens staan los van advertentie- en gebruiksmetingen. **Remove telemetry** in Waze en **Disable tracking** in Flitsmeister beperken ondersteunde trackingdiensten; **Broadcast navigation data** deelt rijwaarnemingen met je gekozen kaartapp.

## 1. Patch de benodigde apps

Kies één bron en één ontvangende kaartapp. Beide bronnen installeren kan ook; kies daarna in de kaartapp welke je wilt gebruiken.

| App | Ondersteund origineel | Benodigde patch |
| --- | --- | --- |
| [Waze](../apps/waze.md) | 5.26.90.901, volledige APKM | **Broadcast navigation data** |
| [Flitsmeister](../apps/flitsmeister.md) | 14.17, volledige APKS | **Broadcast navigation data**; houd **Fix PairIP startup** aan |
| [Google Maps](../apps/google-maps.md) | 26.43.05.1002020710, volledige APK | **Waze alerts**; kies ook **Native navigation for Gearslip and Android Auto** voor de native auto-interface |
| [Vela Maps](../apps/vela.md) | 0.5.2621, volledige APK | **External speed controls** |

Pas deze patches uit Dutch App Patches toe in Morphe Manager. Kies voor Maps alleen patches uit deze bron; de aangepaste bearinmind-opties zitten er al in. Installeer de resultaten en rond de eerste instellingen af op de telefoon. Gebruik voor een ontvangende navigatie-app in Android Auto de [KingInstaller-handleiding](android-auto.md).

De bronpatch heeft een optionele instelling **Receiver package**. Laat die leeg als beide ondersteunde kaartapps updates mogen ontvangen terwijl ze draaien. Vul voor één ontvanger `app.vela` in voor Vela, of de werkelijke pakketnaam van je gepatchte Maps, meestal `org.ungoogled.android.apps.maps`. Een eigen pakketnaam of de microG-RE-variant vereist zijn eigen naam. Een gerichte pakketnaam levert geen updates aan een andere ontvanger.

## 2. Stel de bron eenmaal in

Open gepatchte Waze of Flitsmeister eenmaal, rond de eerste instellingen af en geef de locatietoegang die de rijdienst nodig heeft. Sta achtergrondlocatie toe wanneer de app daarom vraagt voor dit gebruik. Controleer de batterijbeperkingen als updates stoppen zodra het scherm uitgaat. De overlay mag uit blijven.

Met de bijgewerkte patches aan beide kanten start de gekozen rij-engine automatisch zodra je Maps/Vela opent of **De-Waze**/**Flitsmeister** kiest. Het scherm van de bronapp opent niet. Android toont de normale doorlopende melding voor de locatiedienst. Tijdens deze beheerde sessie blijft de bron gedempt: Maps/Vela geeft de gesproken waarschuwingen en geluiden. Opgeslagen geluidsinstellingen en het telefoonvolume blijven ongewijzigd; normaal brongeluid keert terug nadat de deelsessie is verlopen. De kaartapp controleert de eigen motorstatus van de bron tijdens zichtbaar gebruik en actieve navigatie, herstelt de verbinding na een gestopt proces en probeert een gestopte motor opnieuw te starten met tussenpozen. Ontbrekende GPS- of wegwaarnemingen veroorzaken geen herstart en worden niet als verse gegevens behandeld.

De bronstatus maakt onderscheid tussen starten, een actieve motor, ontbrekende locatietoegang en een ontbrekende start-API. Patch beide apps opnieuw als de API ontbreekt; oudere bronpatches kunnen nog steeds gegevens leveren nadat je de bron handmatig opent. Blokkeert Android een herstart op de achtergrond, dan meldt de kaartapp dit zonder de bron te openen. Bij terugkeer naar de kaart volgt een nieuwe stille poging. De koppeling gebruikt geen root of Shizuku.

Kiezen voor eigen gegevens of het beëindigen van de ontvangende sessie verbreekt de startverbinding. De extra deeldienst verloopt na 45 seconden zonder ontvanger; zelfstandig draaiende bronnavigatie blijft ongemoeid. Waze behoudt zijn bestaande instelbare inactiviteitstime-out, aanvankelijk 180 minuten.

## 3. Kies de bron in je kaartapp

### Google Maps / Ungoogled Maps

1. Open het accountmenu en daarna **Waze & external data source**.
2. Open **Navigation data source** en kies **De-Waze · navigation data** of **Flitsmeister · navigation data**.
3. Zet **Compact speed and average-speed control** aan als je snelheid, limiet en actieve trajectgegevens samen wilt zien.
4. Kies de gewenste categorieën onder **Road alert types**. Alleen politie staat standaard geselecteerd.
5. Stel **Road alerts on the map** en **Chime for a road alert ahead** apart in. Alleen meldingen met bekende posities krijgen kaartspelden.
6. Controleer de bronstatus bij **Source now**. Tik erop om een beschikbare momentopname op te vragen. Automatisch starten gebruikt de aparte bron-API; een momentopname maakt oude waarnemingen nooit vers.

**Off · Google Maps** herstelt de eigen gegevens van Maps. Het alternatief **Waze · Share alerts with Maps** gebruikt de oudere Waze-meldingenkoppeling: Waze moet **Share alerts with Maps** bevatten, aangemeld zijn en blijven draaien. Die modus levert niet de uitgebreide snelheid-, limiet- en trajectgegevens. Kies voor deze handleiding de optie voor navigatiegegevens.

Maps pauzeert zijn eigen Flock-spelden en geluiden zolang de gekozen moderne bron bruikbare verse gegevens levert. Je Flock-voorkeuren blijven bewaard en Flock-routevermijding houdt zijn aparte cameragegevens.

### Vela Maps

1. Open de navigatie-instellingen en daarna **Snelheid en wegwaarschuwingen**.
2. Kies **De-Waze** of **Flitsmeister**. **Vela (eigen gegevens)** herstelt de eigen bron.
3. Stel zo gewenst **Gesproken wegwaarschuwingen** in. Deze optie volgt de stem- en dempinstellingen van Vela.
4. In de auto-instellingen kiest **Navigatieprovider** dezelfde opgeslagen bron. **Traject- en waarschuwingsdetails tonen** regelt de extra autodetails.

De compacte weergave toont snelheid naast de limiet. Een actief traject voegt het waargenomen gemiddelde, de voortgang en resterende afstand toe; geschatte voortgang krijgt ≈. Het gemiddelde blijft onbekend totdat voldoende afstand en tijd zijn waargenomen.

Met een externe bron gekozen pauzeert Vela zijn eigen Flock-weergave en waarschuwingen zolang een van beide ondersteunde bronnen bruikbare verse gegevens levert. De pauze kan doorgaan als de gekozen bron wegvalt terwijl de andere nog verstuurt. Opgeslagen voorkeuren hervatten zodra beide bronnen onbruikbaar zijn, of als je **Vela (eigen gegevens)** kiest.

## 4. Controleer de ontvangst

Open na de eenmalige broninstellingen, terwijl je geparkeerd staat, de ontvangende kaartapp met een externe bron geselecteerd. Controleer of de gekozen bron wordt vermeld en verse gegevens meldt wanneer er waarnemingen zijn. Laat de bron op de achtergrond en controleer of de ontvangst doorgaat. Stilstand met snelheid nul is geldig; een ontbrekende snelheid of limiet moet onbekend blijven of terugvallen op de eigen kaartgegevens.

Een bronwissel, het verlaten van een traject of verlopen trajectwaarnemingen verwijdert het gedeeltelijke gemiddelde. De waarden van beide bronnen blijven gescheiden. Vanaf Android 14 kan de ontvanger via Android controleren welke geïnstalleerde bron een update verstuurde; oudere versies hebben die afzendercontrole niet.

| Probleem | Controleer |
| --- | --- |
| Bron blijft onbeschikbaar | Beide apps zijn gepatcht, de bron is geopend, zijn rij-/locatiedienst is actief en de juiste bron is gekozen. |
| Eén ontvanger krijgt gegevens, een andere niet | Verwijder of corrigeer de optionele **Receiver package** van de bronpatch en patch de bron opnieuw als je die optie wijzigt. |
| Snelheid werkt, maar geen gemiddelde | Er is een verse waarneming van een actief traject nodig, gevolgd door voldoende tijd en afstand. Alleen een cameramelding is geen actief traject. |
| Geen spelden of geluid in Maps | Kies de meldingstypen en zet de kaart-/geluidsschakelaars aan. De bron moet verse meldingen hebben, met posities voor spelden en een bruikbare naderingsafstand voor waarschuwingen. |
| Gegevens stoppen bij schermvergrendeling | Controleer achtergrondlocatie, de brondienst en batterijbeperkingen. Open een gestopte bron opnieuw. |

De weergave en terugval in Google Maps zijn gecontroleerd met gestuurde bronherhalingen. Ontvangst op de achtergrond, telefoon-/autoweergave en gesproken voorwaarschuwingen in Vela zijn gecontroleerd op een Android 16-emulator met Gearslip. Native Maps-navigatie heeft aparte controles in Gearslip en de officiële Android Auto Desktop Head Unit. Deze resultaten bewijzen niet iedere combinatie van bron, telefoon, fysiek autoscherm of nauwkeurigheid op de weg.

Voor ontwikkelaars beschrijft het [gedeelde navigatieprotocol](https://github.com/johninnl/nl-morphe-patches/blob/main/docs/navigation-broadcasts.md) de gegevens en geldigheidsregels. Andere ontvangende apps moeten dat protocol ondersteunen; alleen de bronpatch installeren voegt geen ondersteuning toe aan een willekeurige app.

Stil koud starten is ook gecontroleerd voor alle vier combinaties van Maps/Vela en Waze/Flitsmeister op een Android 16-emulator, met de aangeleverde originelen en de eigen motorstatus van de bron. Dit bewijst niet alle energieregels van fabrikanten of gedrag in een fysieke auto.
