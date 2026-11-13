# Reolink

Bekijk je Reolink-camera's op het autoscherm van Android Auto en carhosts zoals DriveDeck, met de telefoonindeling van Reolink en een Terug-knop die werkt.

**Appversie:** 4.64.3.20261023 · **Build:** 796 · **Origineel bestand:** APKM

[Gebruik je originele APKM](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer het volledige originele APKM-archief in Morphe Manager, inclusief de basis-, architectuur- en scherm-APK's.

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Parked app for Android Auto** | Aan | Meldt Reolink aan als geparkeerde video-app, zodat Android Auto en hosts die de gedocumenteerde ontdekking volgen hem zelf op het autoscherm tonen. |
| **Custom layout for Android Auto** | Aan | Toont op een autoscherm de telefoonindeling van Reolink over de volle breedte: het overzicht en de camerapagina met de knoppen, Afspelen en Snel antwoorden. Volledig scherm toont nog steeds alleen de video; Terug sluit volledig scherm en gaat daarna naar het overzicht, zonder te draaien. Op de telefoon verandert niets. |
| **Change package name** | Aan | Installeert de gepatchte Reolink naast de Reolink uit de Play Store, die zijn aanmelding, apparaten en meldingen houdt. |
| **Custom branding** | Aan | Noemt de gepatchte kopie "Reolink AA" met een eigen app-icoon. Je kunt ook een andere naam of het originele icoon van Reolink kiezen. |

## Voor gebruik

Een autoscherm is liggend en draait nooit. De camerapagina van Reolink is een staande indeling; liggend toont Reolink alleen de video, waar Terug Android alleen vraagt de telefoon terug te draaien naar staand. Op een autoscherm gebeurt dat nooit, dus Terug verlaat de video nooit. De autoschermindeling geeft elk Reolink-scherm zijn staande indeling over het hele autoscherm en laat de knop voor volledig scherm en Terug de camerapagina wisselen zonder te draaien. Dit geldt op elk scherm behalve dat van de telefoon zelf, welke carhost Reolink ook toont, zoals Android Auto, DriveDeck, AARE of Flywheel: er is geen lijst met toegestane hosts. Hosts die het telefoonscherm spiegelen, zoals Gearslip, tonen Reolink zoals op de telefoon.

De gepatchte kopie is een aparte app met eigen gegevens: meld je er één keer in aan of voeg je camera's toe. Pushmeldingen van Reolink horen bij de originele app en zijn handtekening, dus houd de Reolink uit de Play Store geïnstalleerd voor meldingen.

Android Auto heeft voor een gepatchte app nog steeds Onbekende bronnen in de ontwikkelaarsinstellingen nodig. Gecontroleerd op DriveDeck via Androids desktop head unit: het overzicht, de live camerapagina en de afspeelpagina tonen hun telefoonindeling over het autoscherm met het camerabeeld in de eigen verhoudingen, en Terug gaat terug naar het overzicht, ook na het opnieuw openen van een camera. Het wisselen naar volledig scherm is in die sessie niet vastgelegd.

[Installatie](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaarden](../reference/catalogue.md)
