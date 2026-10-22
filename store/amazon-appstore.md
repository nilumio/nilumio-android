# nilumio – Amazon Appstore (Fire TV)

Stand 22.10.2026. Grundlage ist Branch `nordlicht`, Flavor `firetv`. Anforderungen nach der Amazon-Doku „Step 3: Add Appstore Details“ (developer.amazon.com/docs/app-submission/appstore-details.html) und Berichten zu Vega OS; vor dem Einreichen kurz gegen die Konsole prüfen, Amazon ändert Details ohne Ankündigung.

## 1. Was der Flavor `firetv` anders macht

| Punkt | `appstore` (Google Play) | `firetv` (Amazon) |
|---|---|---|
| applicationId | `com.held0.nilumio` | `com.held0.nilumio` (gleich) |
| App-Name, Symbol, TV-Banner, Start-Animation | nilumio | nilumio (alles aus `main`, kein eigener Ordner `app/src/firetv`) |
| Leanback-Funktion verlangt | ja | ja |
| Eingebauter Updater (`UPDATING_ENABLED`) | aus | aus |
| Seerr/Filmwünsche (`DISCOVER_ENABLED`) | an | **aus** |
| Berechtigung `REQUEST_INSTALL_PACKAGES` | entfernt (`app/src/appstore/AndroidManifest.xml`) | entfernt (`app/src/firetv/AndroidManifest.xml`, seit 22.10.2026) |

Folgen:
- Der Store-Text für Amazon darf Seerr nicht erwähnen (unten schon berücksichtigt).
- `REQUEST_INSTALL_PACKAGES` wird ohne Updater nicht gebraucht und ist deshalb wie bei `appstore` entfernt, damit Amazon nicht nachfragt.
- Gleiche applicationId wie bei Play ist in Ordnung: Fire-TV-Geräte haben kein Google Play, es gibt keine Überschneidung.

## 2. Bauen

```
./gradlew :app:assembleFiretvRelease
```

Ergebnis in `app/build/outputs/apk/firetv/release/`: je eine APK für `armeabi-v7a`, `arm64-v8a`, `x86_64` und eine Universal-APK. Signiert wird mit demselben lokalen Schlüssel wie `appstore` (`release.signing.config` in `local.properties`).

Hochladen: am einfachsten die **Universal-APK** (läuft auf allen Fire-TV-Modellen). Viele Fire-TV-Geräte laufen trotz 64-Bit-Chip mit 32-Bit-Fire-OS, deshalb mindestens `armeabi-v7a` immer mitliefern, falls einzelne APKs hochgeladen werden.

**Signatur:** Amazon signiert die App beim Veröffentlichen mit einem eigenen, kontobezogenen Zertifikat neu. Den SHA-256-Fingerabdruck zeigt die Konsole nach dem Upload (Abschnitt „Appstore Certificate Hashes“). Diesen Fingerabdruck zusätzlich in `https://nilumio.com/.well-known/assetlinks.json` eintragen, sonst öffnen sich Einladungslinks (`https://nilumio.com/einladung`) bei Amazon-Installationen nicht direkt in der App.

## 3. Konto

- Amazon-Developer-Konto: **kostenlos**, Anmeldung mit einem Amazon-Konto unter developer.amazon.com.
- Steuer- und Auszahlungsangaben (Tax Interview, Bankkonto) braucht es nur für kostenpflichtige Apps oder In-App-Käufe. nilumio ist kostenlos.
- **Achtung Privatsphäre:** Der „Developer Name“ erscheint öffentlich im Store. Als Namen „nilumio“ eintragen. Für die EU fragt Amazon wie Google nach dem Händlerstatus (DSA). Als Nicht-Händler angeben (kostenlose App, keine Einnahmen), dann werden keine Anschrift und Telefonnummer veröffentlicht. Kontakt-E-Mail: `webmaster@nilumio.com`.

## 4. Geräte

- Unterstützt: alle Fire-TV-Geräte mit **Fire OS** (Fire OS 6, 7, 8; Android 7.1 bis 11). minSdk 23 passt.
- **Nicht** unterstützt: Geräte mit **Vega OS** (Linux, keine Android-Apps): Fire TV Stick 4K Select (2025), der neue Fire TV Stick 4K (Oktober 2026) und laut Amazon alle künftigen Fire TV Sticks. Dort gibt es nilumio nur mit einer eigenen Vega-Fassung (React Native, kompletter Neubau). Amazon streamt für ausgewählte Entwickler Android-Apps übergangsweise aus der Cloud, darauf ist kein Anspruch.
- Fire-Tablets: nicht anbieten (Leanback ist Pflicht, die App ist für die Fernbedienung gebaut). Bei der Gerätezuordnung in der Konsole nur „Fire TV“ anhaken.
- Der Amazon Appstore auf normalen Android-Handys wurde im August 2025 eingestellt, er spielt keine Rolle mehr.

Amazon prüft beim Test: Bedienung komplett mit der Fire-TV-Fernbedienung (Steuerkreuz, Auswahl, Zurück, Menü, Wiedergabetasten), Zurück auf der Startseite beendet die App, keine Hinweise auf Google Play, keine Google-Play-Dienste nötig (nilumio nutzt keine).

## 5. Grafiken

Liegen fertig in `store/amazon/`. Alle PNG ohne Transparenz.

| Datei | Zweck | Größe | Pflicht |
|---|---|---|---|
| `icon-114.png` | kleines Symbol | 114 × 114 | ja |
| `icon-512.png` | großes Symbol | 512 × 512 | ja |
| `firetv-icon-1280x720.png` | Fire-TV-App-Symbol (Kachel in der App-Reihe) | 1280 × 720 | ja |
| `firetv-background-1920x1080.png` | Hintergrund der Detailansicht | 1920 × 1080 | ja |
| `promo-1024x500-de.png`, `promo-1024x500-en.png` | Werbegrafik | 1024 × 500 | nein |
| Screenshots | 3 bis 10 Stück, 1920 × 1080, nur freie Inhalte | 1920 × 1080 | ja, **offen** |

Die Screenshots können dieselben wie für Google Play sein (siehe `store/play-console.md`, Abschnitt 7). Amazon nimmt die Fire-TV-Screenshots auch für die Website-Ansicht.

## 6. Prüfer-Zugang

Amazon braucht zum Testen einen erreichbaren Jellyfin-Server mit Zugang. Wie bei Google (`store/play-console.md`, Abschnitt 4) einen eigenen Demo-Server mit freien Filmen und einem Demo-Benutzer einrichten und Adresse, Benutzer und Passwort ins Feld „Testing Instructions“ schreiben. Nicht den privaten Server verwenden.

## 7. Inhaltsbewertung und Datenschutz

- Amazon hat einen eigenen Fragebogen. Alle Fragen zu Gewalt, Sex, Drogen, Glücksspiel, Werbung, Standort: **Nein**. Erwartet: „All Ages“ bzw. ab 0. Hinweis an Amazon: Die App ist ein Abspieler und enthält selbst keine Inhalte.
- „Collects user data?“: Nein (Begründung wie `store/play-console.md`, Abschnitt 1).
- Datenschutzerklärung: `https://nilumio.com/datenschutz/`.
- Kategorie: „Video“ bzw. „Movies & TV“.

## 8. Store-Texte

Grenzen bei Amazon: Titel bis 250 Zeichen, Kurzbeschreibung bis 1200, Langbeschreibung bis 4000, 1 bis 10 Produktmerkmale (Aufzählungspunkte). Abgeleitet aus `store/listing.md`, ohne Seerr (im `firetv`-Flavor abgeschaltet).

### Deutsch (de-DE)

**Titel:** nilumio – TV Media Player

**Kurzbeschreibung:** Deine Jellyfin-Mediathek auf dem Fernseher: schnell, übersichtlich, kinoreif.

**Langbeschreibung:**

nilumio bringt deine eigene Film- und Serien-Sammlung auf den Fernseher. Die App verbindet sich mit deinem Jellyfin-Server und zeigt deine Mediathek in einer ruhigen, dunklen Oberfläche mit großen Bildern – gebaut für die Fernbedienung.

Was du brauchst:
nilumio ist ein Abspieler, kein Streamingdienst. Du brauchst einen eigenen Jellyfin-Server (z. B. auf einem NAS) mit deinen eigenen Medien. nilumio enthält keine Filme oder Serien.

Open Source:
nilumio ist freie Software unter der GNU GPL 2.0 und basiert auf dem Open-Source-Projekt Wholphin von damontecres. Den vollständigen Quellcode findest du unter: https://github.com/nilumio/nilumio-android

nilumio ist ein unabhängiges Projekt und steht in keiner Verbindung zum Jellyfin-Projekt.

**Produktmerkmale:**
- Filme, Serien, Musik und Live-TV von deinem eigenen Jellyfin-Server
- Weiterschauen, wo du aufgehört hast – auf allen Geräten
- Vorschaubilder beim Spulen (Trickplay)
- Intro, Rückblick und Abspann automatisch überspringen
- Zwei Videoplayer zur Wahl (ExoPlayer und MPV) für fast jedes Dateiformat
- Untertitel inkl. ASS/SSA, Tonspuren und Kapitel
- Live-TV und Aufnahmen
- Mehrere Profile, auf Wunsch mit PIN

**Suchbegriffe:** Jellyfin, Mediaplayer, Heimkino, NAS, Filme, Serien, Live-TV, Untertitel

### English (en-GB)

**Title:** nilumio – TV Media Player

**Short description:** Your Jellyfin library on the big screen: fast, tidy and cinematic.

**Long description:**

nilumio brings your own film and series collection to your TV. It connects to your Jellyfin server and shows your library in a calm, dark interface with large artwork, designed for the remote control.

What you need:
nilumio is a player, not a streaming service. You need your own Jellyfin server (for example on a NAS) with your own media. nilumio does not contain any films or series.

Open source:
nilumio is free software under the GNU GPL 2.0 and is based on the open-source project Wholphin by damontecres. The full source code is available at: https://github.com/nilumio/nilumio-android

nilumio is an independent project and is not affiliated with the Jellyfin project.

**Product features:**
- Films, series, music and live TV from your own Jellyfin server
- Resume where you left off, on every device
- Preview thumbnails while seeking (trickplay)
- Skip intros, recaps and credits automatically
- Two video players to choose from (ExoPlayer and MPV) for almost any file format
- Subtitles including ASS/SSA, audio tracks and chapters
- Live TV and recordings
- Multiple profiles, optionally PIN-protected

**Keywords:** Jellyfin, media player, home cinema, NAS, films, series, live TV, subtitles

## 9. Offene Punkte vor dem Einreichen

1. Amazon-Developer-Konto anlegen (kostenlos), Developer Name „nilumio“, Nicht-Händler.
2. Screenshots (3 bis 10, 1920 × 1080, freie Inhalte).
3. Demo-Jellyfin-Server für die Prüfer.
4. Nach dem ersten Upload: Amazon-Zertifikat-Fingerabdruck in `assetlinks.json`.
5. Vor dem Upload einmal auf einem echten Fire-TV-Gerät mit Fire OS testen (die Philips-Fassung ist Google TV, nicht Fire OS).
