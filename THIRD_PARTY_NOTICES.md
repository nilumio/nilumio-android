# Third-party notices

nilumio for Android TV is a fork of **Wholphin** and is distributed under the
[GNU General Public License v2.0](LICENSE). This file lists the code, fonts and
other material that nilumio took from other projects, in addition to Wholphin itself.
The files nilumio added or changed are listed in [NILUMIO_CHANGES.md](NILUMIO_CHANGES.md).

## Base project

| Project | Licence | Use in nilumio |
|---|---|---|
| [Wholphin](https://github.com/damontecres/Wholphin) by damontecres and contributors | GPL-2.0 | The whole app. Wholphin's files keep their licence; nilumio's changes to them are also GPL-2.0. Translations come from Wholphin's [Weblate project](https://translate.codeberg.org/engage/wholphin/). |

## Code taken or ported into nilumio

| Area | Origin | Licence | Files in nilumio |
|---|---|---|---|
| SyncPlay protocol flow, time sync, scheduling of server commands, start position, ready and buffering reports (ported from JavaScript to Kotlin) | [jellyfin-web](https://github.com/jellyfin/jellyfin-web): `src/plugins/syncPlay/core/Manager.js`, `PlaybackCore.js`, `QueueCore.js`, `timeSync/TimeSync.js`, `timeSync/TimeSyncServer.js`; page layout after `ui/groupSelectionMenu.js` | GPL-2.0 | `app/src/main/java/com/github/damontecres/wholphin/services/syncplay/SyncPlayManager.kt`, `.../services/syncplay/SyncPlayPlayer.kt`, `.../ui/syncplay/SyncPlayPage.kt` |
| SyncPlay manager skeleton (singleton, group list, create, join and leave) | [Wholphin-Enhanced](https://github.com/AylaTheTanuki/Wholphin-Enhanced): `services/syncplay/SyncPlayManager.kt` | GPL-2.0 | `.../services/syncplay/SyncPlayManager.kt` |

## Libraries added by nilumio

| Library | Licence | Use |
|---|---|---|

All other libraries are inherited from Wholphin and declared in `gradle/libs.versions.toml`.
The app lists their licences on its *License information* screen in the settings; the licence texts of the native
components (libmpv, libass, dav1d, libunibreak and others) are in `app/config/licenses/`.

## Fonts and artwork

| Material | Origin | Licence | Where |
|---|---|---|---|
| Outfit typeface | [Outfit Fonts](https://github.com/Outfitio/Outfit-Fonts), The Outfit Project Authors | SIL Open Font License 1.1 (`store/brand/Outfit-OFL.txt`) | Wordmark in `store/brand/*.svg`; static 300/400 instances in `app/src/main/res/font/outfit_*.ttf` (player controls) |
| nilumio logo, icons, banners, intro video and sound | made for nilumio | All rights reserved, not licensed for use in other apps | `store/brand/`, `store/play/`, `app/src/main/res/mipmap-*/ic_nilumio*`, `app/src/main/res/raw/nilumio_intro.mp4`, `app/src/main/res/drawable-nodpi/nilumio_intro_final.png` |

## Trademarks

Jellyfin is a trademark of the Jellyfin project. Wholphin is the name of the upstream
project. nilumio is not affiliated with or endorsed by either.
