# Google Messages

Open het eigen autoscherm van Messages op DriveDeck en andere Android Auto-hosts, vanuit een kopie van Messages die naast de standaardapp wordt geïnstalleerd. **Experimenteel:** deze patches zijn nog niet op een telefoon bevestigd.

**Appversie:** messages.android_20261030_00_RC00.phone_samsung_dynamic · **Build:** 327677900 · **Origineel bestand:** APKS

[Gebruik je originele APKS](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige originele APKS-archief in Morphe Manager, inclusief de basis-, arm64-, scherm- en taal-APK's. Dit is de Samsung-telefoonversie van Messages; andere versies van Messages worden niet ondersteund.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Allow any Android Auto host** | Aan | Laat elke Android Auto-compatibele host het autoscherm van Messages openen, zoals DriveDeck, Gearslip, AARE of Flywheel, ongeacht de pakketnaam of ondertekeningssleutel. Standaard accepteert Messages alleen Googles eigen Android Auto. Het autoscherm staat ook meteen aan, in plaats van te wachten op een synchronisatie van Google-instellingen. |
| **Disable screen-share warning** | Aan | Zorgt dat Messages het scherm van een carhost niet als schermdelen ziet, zodat berichttekst zichtbaar blijft in meldingen en Messages niet waarschuwt dat iemand toegang tot je scherm heeft. Dit zet de waarschuwing in deze kopie ook uit bij echt schermdelen. |
| **Change package name** | Aan | Installeert de gepatchte Messages naast de standaard Google Messages die op de telefoon staat. Elke provider en permissie die Messages declareert, verhuist mee, zodat de twee nooit botsen. |
| **Custom branding** | Aan | Noemt de gepatchte kopie "Messages AA" met een eigen app-icoon. Je kunt ook een andere naam of het originele icoon van Messages kiezen. |

## Voor gebruik

Het autoscherm toont je tien meest recente gesprekken. Als je er een opent, kan de host berichten voorlezen, een antwoord sturen en het gesprek als gelezen markeren; Messages verstuurt het antwoord zelf, net als vanaf de telefoon. Een nieuw gesprek beginnen hangt van de host af, omdat het autoscherm van Messages alleen de knop aanbiedt.

De gepatchte kopie is een aparte app met eigen gegevens. Hij ontvangt en toont sms'jes alleen als hij je standaard-sms-app is: stel dat in bij de standaardapps van Android, of wanneer de gepatchte Messages erom vraagt. Terugschakelen naar de standaard Google Messages gaat op dezelfde manier.

De gepatchte kopie is ondertekend met je Morphe-sleutel, niet met die van Google. Reken erop dat RCS-chat, aanmelden met een Google-account, back-ups en andere Google-functies in deze kopie onbetrouwbaar zijn of ontbreken. Sms en mms blijven naar verwachting werken. Houd de standaard Google Messages geïnstalleerd als je die functies nodig hebt.

Hosts die alleen berichtmeldingen lezen, zoals het eigen Berichten-scherm van DriveDeck, werken ook met de standaard Google Messages en hebben deze patches niet nodig.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaarden](../reference/catalogue.md)
