# nilumio – Antworten für die Play Console

Stand 22.10.2026. Grundlage ist der Code auf Branch `nordlicht`, Flavor `appstore` (applicationId `com.held0.nilumio`). Antworten zum Kopieren, Begründungen darunter. Ändert sich die App (neue SDKs, neue Netzwerkziele), diese Datei und `store/website/datenschutz/index.html` mitziehen.

## 0. Vorab-Checkliste

| Punkt | Stand |
|---|---|
| App-Bundle | `store/release/nilumio-<version>-appstore-release.aab` (nicht im Repo) |
| targetSdk | 37 (Pflicht seit 31.08.2026: Smartphone-Apps 36, reine TV-Apps 34; passt) |
| minSdk | 23 (Android 6.0) |
| Signatur | Upload-Schlüssel aus einer lokalen `keystore.properties` außerhalb des Repos (siehe `app/build.gradle.kts`); Play App Signing beim ersten Upload aktivieren |
| Datenschutzerklärung | `store/website/datenschutz/index.html`, muss öffentlich erreichbar sein, z. B. `https://nilumio.com/datenschutz/` |
| Anschrift im Impressum/Datenschutz | **offen** (Platzhalter, siehe Abschnitt 9) |
| TV-Screenshots | **offen** (Abschnitt 7) |
| Demo-Zugang für Prüfer | Vorschlag in Abschnitt 4, **vorher mit nilumio testen** |

## 1. Datensicherheit (Data safety)

### Fragebogen

| Frage | Antwort |
|---|---|
| Erhebt oder teilt deine App erforderliche Nutzerdatentypen? | **Nein** |
| (falls doch Ja verlangt wird) Werden alle Daten bei der Übertragung verschlüsselt? | Nicht pauschal: Die Verbindung zum Jellyfin-Server ist so sicher, wie der Nutzer sie einrichtet (HTTP im Heimnetz ist erlaubt). Daher hier **Nein** angeben. |
| Können Nutzer das Löschen ihrer Daten beantragen? | entfällt bei „keine Daten erhoben“ |

Ergebnis im Store: „Keine Daten erhoben“ und „Keine Daten an Dritte weitergegeben“.

### Begründung (für dich, falls Google nachfragt)

Google zählt als „erhoben“ nur Daten, die die App vom Gerät an den Entwickler oder an Dritte überträgt. Bei nilumio gilt:

- Alle Nutzungsdaten (Anmeldung, Wiedergabefortschritt, Gerätename/-ID, Absturzberichte, App-Protokolle) gehen ausschließlich an den **vom Nutzer selbst eingetragenen Jellyfin-Server**. Der Entwickler betreibt keinen Server und bekommt nichts.
- Absturzberichte (ACRA) werden nur nach Bestätigung im Dialog und nur an den eigenen Jellyfin-Server geschickt (`CrashReportSender` → `clientLogApi.logFile`). Kein ACRA-HTTP-Endpunkt des Entwicklers.
- Keine Analyse-, Werbe- oder Tracking-SDKs (geprüft: kein Firebase, Crashlytics, Sentry, Google Analytics, keine Werbe-ID).
- Sprachsuche nutzt den Android-`SpeechRecognizer` des Systems, die App bekommt nur Text.
- Daten, die der Nutzer selbst an einen von ihm gewählten Dienst schickt (eigener Jellyfin-/Seerr-Server), gelten nicht als Erhebung durch den Entwickler.

**Grenzfälle, die in der Datenschutzerklärung offen genannt sind:**

1. **TMDB-Bilder:** Ist beim Seerr-Server des Nutzers der Bild-Cache aus, lädt die App Poster direkt von `image.tmdb.org`. TMDB sieht dabei nur die IP-Adresse; es wird nichts gespeichert oder gezielt übertragen. Google wertet die IP-Adresse aus einer normalen Netzwerkanfrage nicht als erhobene Nutzerdaten.
2. **GitHub-Versionshinweise:** Tippt man in den Einstellungen auf „Installierte Version“, holt die App Versionshinweise von `api.github.com/repos/damontecres/Wholphin/releases/tags/v…` (`PreferencesViewModel.fetchReleaseNotes`, nicht durch `UPDATING_ENABLED` abgeschaltet). Das ist auch inhaltlich falsch (Wholphin- statt nilumio-Notizen). **Empfehlung:** im `appstore`-Flavor abschalten oder auf ein eigenes Repo umstellen; danach den Absatz in der Datenschutzerklärung streichen.

## 2. Inhaltsbewertung (IARC-Fragebogen)

- **E-Mail für die Einstufung:** webmaster@nilumio.com
- **Kategorie:** „Alle anderen App-Typen“ (*All Other App Types*). Nicht „Spiel“, nicht „Soziale Netzwerke“.

| Frage | Antwort |
|---|---|
| Gewalt, Blut, Angst/Horror | Nein |
| Sexuelle Inhalte, Nacktheit | Nein |
| Vulgäre Sprache | Nein |
| Kontrollierte Substanzen (Drogen, Alkohol, Tabak) | Nein |
| Glücksspiel, simuliertes Glücksspiel | Nein |
| Können Nutzer miteinander interagieren oder Inhalte austauschen? | Nein |
| Teilt die App den genauen Standort mit anderen Nutzern? | Nein |
| Können Nutzer digitale Waren kaufen? | Nein |
| Bietet die App uneingeschränkten Internetzugang (z. B. Browser)? | Nein |
| Enthält die App Werbung? | Nein |
| Handelt es sich um eine Sammlung/einen Shop für andere Apps? | Nein |

Erwartetes Ergebnis: USK ab 0 / PEGI 3 / IARC 3+. Begründung: Die App selbst enthält keine Inhalte; sie spielt nur Medien aus dem eigenen Server des Nutzers ab (wie ein Video-Player).

## 3. Zielgruppe und Inhalte

| Frage | Antwort |
|---|---|
| Altersgruppen der Zielgruppe | **18 und älter** |
| Könnte die App Kinder ungewollt ansprechen? | Nein |
| Werbung | „Nein, meine App enthält keine Werbung“ |
| App-Kategorie „Nachrichten“ | Nein |
| COVID-19-Kontaktverfolgung | Nein |
| Behörden-App | Nein |
| Finanzfunktionen | Keine |
| Gesundheit | Keine |

Warum 18+: Die App zeigt beliebige Inhalte aus dem Server des Nutzers. Mit 18+ fällt sie nicht unter die Familienrichtlinien, die Altersprüfung übernimmt der Jellyfin-Server (Profile, Altersfreigaben).

## 4. App-Zugriff für Prüfer

Einstellung: **„Alle oder einige Funktionen sind eingeschränkt“** → Anleitung hinzufügen.

### Variante A: öffentlicher Jellyfin-Demo-Server (vorhanden)

Geprüft am 22.10.2026 (eine Abfrage): `https://demo.jellyfin.org/stable` antwortet, Servername „Stable Demo“, Jellyfin-Server **12.2.0**. Anmeldung laut Jellyfin-Projekt: Benutzer `demo`, Passwort leer. Der Server wird regelmäßig zurückgesetzt und enthält wenige, frei verwendbare Testvideos.

**Vor dem Einreichen:** in nilumio auf einem TV-Gerät/Emulator testen, ob Anmeldung und Wiedergabe mit Server 12.2.0 klappen. Nachteil: Wir haben keinen Einfluss auf Verfügbarkeit und Inhalte.

Text für die Play Console (EN):

```
nilumio is a client for a Jellyfin media server and does not contain any media itself.
To review the app, please use the public Jellyfin demo server:

1. Open nilumio on an Android TV device.
2. Choose "Add server" and enter: https://demo.jellyfin.org/stable
3. Username: demo
   Password: (leave empty)
4. Select any film in the library and press play.

The demo server is run by the Jellyfin project and only contains freely licensed test content.
No in-app purchases or accounts with us are required.
```

### Variante B: eigener Demo-Server (empfohlen für Dauerbetrieb)

Damit Prüfungen nicht vom Jellyfin-Demo abhängen:

1. Auf dem eigenen Server einen Jellyfin-Container starten, z. B. unter `demo.nilumio.com` (HTTPS über den bestehenden Reverse Proxy). Domain und Server sind schon vorhanden; falls ein neuer Dienst/eine neue Domain dazukommt, in held0/admin eintragen.
2. Bibliothek „Filme“ mit Blender-Open-Movies füllen (Liste und Lizenzen in Abschnitt 8). Die Dateien direkt von studio.blender.org bzw. download.blender.org holen; Nutzung ist durch CC BY erlaubt, Namensnennung in der Beschreibung der Bibliothek eintragen.
3. Benutzer `review` anlegen: kein Admin, kein Löschen, kein Herunterladen, Transkodierung erlaubt, nur Lesezugriff auf die Demo-Bibliothek, sicheres Passwort (nur in der Play Console hinterlegen, nicht ins Repo).
4. Sitzungen begrenzen (z. B. max. 2 gleichzeitige Streams), Remote-Bitrate begrenzen.
5. Text oben anpassen: Adresse `https://demo.nilumio.com`, Benutzer `review`, Passwort aus der Console.

## 5. Store-Einstellungen

| Feld | Wert |
|---|---|
| App-Name | nilumio – TV Media Player |
| Standardsprache | Deutsch (de-DE), zusätzlich Englisch (en-GB) |
| App oder Spiel | App |
| Kostenlos oder kostenpflichtig | Kostenlos |
| Kategorie | **Video-Player & -Editoren** (*Video Players & Editors*) |
| Tags (max. 5, aus Googles Liste wählen) | Videoplayer, Medienplayer, Streaming, Filme, Fernsehen |
| E-Mail-Adresse | webmaster@nilumio.com |
| Telefonnummer | leer lassen (optional) |
| Website | https://nilumio.com (sobald erreichbar, sonst leer) |
| Datenschutzerklärung | https://nilumio.com/datenschutz/ (Datei: `store/website/datenschutz/index.html`) |

**Händlerstatus (EU-DSA):** Solange die App kostenlos ist und keine Einnahmen erzielt, „Kein Händler“ (*not a trader*) angeben. Als Händler würden Name, Anschrift, Telefon und E-Mail öffentlich im Store angezeigt. Vor jeder Änderung daran erst mit Niklas sprechen.

## 6. Android-TV-Anforderungen

Erledigt (geprüft im Code/Manifest):

- `LEANBACK_LAUNCHER`-Intent-Filter in `MainActivity` vorhanden.
- `android.software.leanback` im Flavor `appstore` auf `required="true"`, die App erscheint damit nur für TV-Geräte. Hinweis: Für Smartphones taucht sie dann nicht auf; das ist gewollt.
- `android.hardware.touchscreen` `required="false"`, ebenso Mikrofon.
- Banner `@mipmap/ic_nilumio_banner` (xhdpi, 320 × 180) im Manifest gesetzt.
- `REQUEST_INSTALL_PACKAGES` wird im Flavor `appstore` entfernt (sonst Ablehnung wegen Richtlinie). Automatische Update-Prüfung ist dort aus.
- Store-Banner 1280 × 720: `store/play/tv-banner-1280x720.png`.

In der Play Console zu tun:

1. **Einstellungen → Erweiterte Einstellungen → Formfaktoren → Android TV hinzufügen** und die TV-Teilnahme bestätigen. TV-Apps werden zusätzlich von Hand geprüft (Bedienung nur mit Steuerkreuz, keine Touch-Pflicht, Banner, Zurück-Taste).
2. Unter **Store-Eintrag → Android TV**: Banner 1280 × 720 und mindestens 1 TV-Screenshot hochladen (1920 × 1080 oder 1280 × 720, 16:9, PNG/JPEG ohne Transparenz).
3. Prüfer achten auf: Fokus immer sichtbar, alle Funktionen mit Fernbedienung erreichbar, Zurück-Taste führt zurück statt die App zu beenden, kein Hochformat.

Grafiken in `store/play/`:

| Datei | Zweck | Größe |
|---|---|---|
| `icon-512.png` | App-Symbol | 512 × 512 |
| `feature-graphic-1024x500-de.png` | Vorstellungsgrafik (de-DE) | 1024 × 500 |
| `feature-graphic-1024x500-en.png` | Vorstellungsgrafik (en-GB) | 1024 × 500 |
| `tv-banner-1280x720.png` | Android-TV-Banner | 1280 × 720 |

## 7. TV-Screenshots

- Format 1920 × 1080, PNG, 4 bis 8 Stück: Startseite, Film-Details, Serien-Staffeln, Wiedergabe mit Trickplay-Vorschau, Einstellungen/Profile, Seerr-Anfragen (optional).
- **Nur freie Inhalte:** Blender-Open-Movies (Abschnitt 8) und frei erfundene Demo-Titel mit eigenen Covern in den nilumio-Farben (#8CFFC4, #6CCDE4, #8079FF). Keine echten Kinofilme oder Serien, keine Senderlogos, kein Blender-Logo.
- Aufnehmen per `adb exec-out screencap -p > shot.png` auf einem 1080p-TV-Emulator (Android TV 14, 1920 × 1080).
- Servername und Benutzername auf den Bildern neutral halten (z. B. „Demo“), keine privaten Daten.

## 8. Namensnennung für Blender-Open-Movies (Textbaustein)

Die Filme stehen unter Creative-Commons-Namensnennung (CC BY). Die Lizenzversion je Film vor der Verwendung auf studio.blender.org nachsehen, die Angaben unten entsprechen dem Stand der Projektseiten.

Für die Langbeschreibung (unten anhängen) bzw. die Website:

```
Bildschirmfotos zeigen freie Filme der Blender Foundation (studio.blender.org), lizenziert unter Creative Commons Namensnennung (CC BY): Big Buck Bunny (CC BY 3.0), Elephants Dream (CC BY 2.5), Sintel (CC BY 3.0), Tears of Steel (CC BY 3.0), Cosmos Laundromat (CC BY 4.0), Spring (CC BY 4.0), Agent 327: Operation Barbershop (CC BY 4.0), Sprite Fright (CC BY 4.0), Charge (CC BY 4.0), Wing It! (CC BY 4.0). © Blender Foundation | studio.blender.org. Übrige Titel und Cover sind frei erfunden.
```

```
Screenshots show free films by the Blender Foundation (studio.blender.org), licensed under Creative Commons Attribution (CC BY): Big Buck Bunny (CC BY 3.0), Elephants Dream (CC BY 2.5), Sintel (CC BY 3.0), Tears of Steel (CC BY 3.0), Cosmos Laundromat (CC BY 4.0), Spring (CC BY 4.0), Agent 327: Operation Barbershop (CC BY 4.0), Sprite Fright (CC BY 4.0), Charge (CC BY 4.0), Wing It! (CC BY 4.0). © Blender Foundation | studio.blender.org. All other titles and artwork are fictional.
```

Nur die Filme nennen, die tatsächlich auf den Screenshots zu sehen sind. Lizenz-Links: https://creativecommons.org/licenses/by/2.5/ · https://creativecommons.org/licenses/by/3.0/ · https://creativecommons.org/licenses/by/4.0/

## 9. Offene Punkte für Niklas

1. **Anschrift:** Datenschutzerklärung (und ein Impressum auf nilumio.com) brauchen in Deutschland eine ladungsfähige Anschrift (§ 5 DDG, Art. 13 DSGVO). Die Privatadresse soll nicht öffentlich werden. Datensparsame Wege: Impressums-/c/o-Service mit ladungsfähiger Adresse, später eine Firma (UG/GmbH) mit Geschäftsadresse. Bis dahin steht ein gelb markierter Platzhalter in der Seite. Abschluss eines solchen Dienstes dann in held0/admin eintragen.
2. **Google-Play-Entwicklerkonto:** Bei einem privaten Konto zeigt Google die Adresse nur bei Händlerstatus an. Kontaktangaben im Store nur webmaster@nilumio.com.
3. **Demo-Server:** Variante A testen oder Variante B aufsetzen.
4. **Screenshots** nach Abschnitt 7.
5. **GitHub-Link** zum Quellcode in `store/listing.md` nachtragen (GPL-2.0 verlangt Zugang zum Quellcode).
6. **Privacy-Seite veröffentlichen** unter einer festen URL, die in der Console eingetragen wird.

## 10. Geschlossener Test (Pflicht für neue private Entwicklerkonten)

Regel: Private Konten, die nach dem 13.11.2023 erstellt wurden, müssen vor der Produktionsfreigabe einen **geschlossenen Test mit mindestens 12 Testern** durchführen, die **14 Tage am Stück** angemeldet bleiben. Danach „Produktionszugriff beantragen“ (Fragen zum Test beantworten).

Ablauf:

1. **Testen → Geschlossene Tests → Track erstellen** (z. B. „Freunde“), App-Bundle hochladen, Länder auswählen (Deutschland, Österreich, Schweiz, ggf. weitere).
2. **Tester festlegen**, zwei Wege:
   - **Google-Gruppe** (bequemer): unter groups.google.com eine Gruppe anlegen, z. B. `nilumio-test@googlegroups.com`, Beitritt „jeder mit Link“ oder auf Einladung. In der Play Console die Gruppen-Adresse eintragen. Neue Tester treten einfach der Gruppe bei.
   - **E-Mail-Liste:** in der Console eine Liste mit den Google-Konten der Tester anlegen (Gmail-Adresse, die auf dem Fernseher angemeldet ist).
3. **Opt-in-Link** aus der Console kopieren und an die Tester schicken. Jeder muss ihn öffnen und „Tester werden“ bestätigen, dann nilumio im Play Store auf dem **Fernseher** installieren (Suche oder Link am Handy → „Auf Gerät installieren“ → TV wählen).
4. Mindestens **12 Tester** müssen **14 Tage durchgehend** im Test bleiben. Besser 15 bis 20 einladen, falls jemand abspringt. Die Tage zählen erst ab dem Opt-in.
5. Während der 14 Tage: kleine Updates hochladen und Rückmeldungen einsammeln (Google fragt beim Produktionsantrag danach, was im Test verbessert wurde).
6. Nach 14 Tagen: **Dashboard → Produktionszugriff beantragen**, Fragebogen ausfüllen (wie getestet, Rückmeldungen, Änderungen). Prüfung dauert meist bis zu 7 Tage.

Textvorschlag für die Einladung:

```
Hallo! Ich bringe meine Android-TV-App nilumio in den Play Store und brauche dafür 14 Tage lang Testerinnen und Tester. nilumio spielt Filme und Serien von einem eigenen Jellyfin-Server ab. Ohne Server kannst du trotzdem helfen: einfach dabeibleiben, ein paar Mal öffnen und gern den Demo-Server ausprobieren.
1. Link öffnen (mit dem Google-Konto deines Fernsehers): <Opt-in-Link>
2. „Tester werden“ tippen
3. nilumio im Play Store auf dem Fernseher installieren
Bitte 14 Tage nicht austreten. Danke!
```

```
Hi! I'm bringing my Android TV app nilumio to Google Play and need testers for 14 days. nilumio plays films and series from your own Jellyfin server. Even without a server you can help: just stay opted in, open it now and then and try the demo server.
1. Open this link with the Google account used on your TV: <opt-in link>
2. Tap "Become a tester"
3. Install nilumio from the Play Store on your TV
Please stay opted in for 14 days. Thank you!
```
