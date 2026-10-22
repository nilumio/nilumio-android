# Übernommener Fremdcode

nilumio ist ein Fork von [Wholphin](https://github.com/damontecres/Wholphin) (GPL-2.0).
Zusätzlich übernommener oder portierter Code aus anderen Projekten:

| Bereich | Herkunft | Lizenz | Dateien in nilumio |
|---|---|---|---|
| SyncPlay: Protokollablauf, Zeitabgleich (TimeSync), Planung der Serverbefehle, Startposition, Bereit-/Puffer-Meldungen (von JavaScript nach Kotlin portiert) | [jellyfin-web](https://github.com/jellyfin/jellyfin-web) `src/plugins/syncPlay/core/Manager.js`, `PlaybackCore.js`, `QueueCore.js`, `timeSync/TimeSync.js`, `timeSync/TimeSyncServer.js`; Seitenaufbau nach `ui/groupSelectionMenu.js` | GPL-2.0 | `app/src/main/java/com/github/damontecres/wholphin/services/syncplay/SyncPlayManager.kt`, `.../services/syncplay/SyncPlayPlayer.kt`, `.../ui/syncplay/SyncPlayPage.kt` |
| SyncPlay: Grundgerüst des Managers (Singleton, Gruppenliste, erstellen/beitreten/verlassen) | [Wholphin-Enhanced](https://github.com/AylaTheTanuki/Wholphin-Enhanced) `services/syncplay/SyncPlayManager.kt` | GPL-2.0 | `.../services/syncplay/SyncPlayManager.kt` |
