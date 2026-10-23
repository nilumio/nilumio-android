# Changes made by nilumio

nilumio for Android TV is a modified version of [Wholphin](https://github.com/damontecres/Wholphin)
(GPL-2.0). As required by section 2(a) of the GPL-2.0, this file states which files nilumio
has changed and when. All changes were made by the nilumio project (Niklas Held) and are
licensed under the GPL-2.0.

Base: Wholphin `main` at commit `551501fa759d95f7eb601455149ccdf2e8602192` (October 2026).
To see every change in detail: `git diff 551501fa..HEAD` or `git log 551501fa..HEAD`.

## Summary by area

| Area | What changed | Main files |
|---|---|---|
| Name and identity | App name nilumio, application ID `com.held0.nilumio`, version code from the commit count, release signing from a key outside the repo | `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `res/values*/strings.xml`, `app/src/debug/res/values/strings.xml` |
| Look | New launcher icons, TV banner, colours, typography, navigation drawer icons with the nilumio gradient, focus border, progress bars in the (dimmed) logo gradient | `res/mipmap-*`, `ui/theme/*`, `ui/components/Nilumio*.kt`, `ui/nav/NilumioDrawer.kt`, `ui/nav/NavDrawer.kt`, `ui/cards/*` |
| Start-up intro | Short intro video while the app loads | `ui/components/StartupIntro.kt`, `MainActivity.kt`, `res/raw/nilumio_intro.mp4`, `res/drawable-nodpi/nilumio_intro_final.png` |
| Home rows by genre | Genre rows can be added to the home screen | `ui/main/HomePage.kt`, `ui/main/settings/HomeSettings*.kt`, `preferences/AppPreference.kt` |
| Side menu | Shuffle and SyncPlay as regular menu items, "Customise menu" with two lists | `ui/nav/NilumioDrawer.kt`, `ui/preferences/NavMenuEditor.kt`, `ui/preferences/NavDrawerPreference.kt`, `services/NavDrawerService.kt` |
| Player controls "Lichtkante" | Title and meta line, edge-to-edge seek bar, one row with time, playback and track buttons, 30 s skip buttons | `ui/playback/overlay/Lichtkante.kt`, `ui/playback/overlay/*`, `res/drawable/nilumio_player_*`, `res/font/outfit_*` |
| Shuffle | Random film or episode, recently watched items come up rarely | `ui/shuffle/ShufflePage.kt` |
| SyncPlay | Create or join SyncPlay groups, synchronised play, pause and seeking | `services/syncplay/*`, `ui/syncplay/SyncPlayPage.kt`, `ui/nav/*`, `ui/playback/PlaybackPage.kt`, `services/hilt/AppModule.kt` |
| Texts and translations | Name changes, fixed escaped ampersands, British spelling; all nilumio texts in one file per language | `res/values*/strings.xml`, `res/values*/strings_nilumio.xml` |
| Build | More memory for Gradle | `gradle.properties`, `gradle/libs.versions.toml` |
| Store and docs | Play Store texts and graphics, privacy policy, artwork sources, fork documentation | `store/`, `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `THIRD_PARTY_NOTICES.md`, `docs/`, `.github/ISSUE_TEMPLATE/` |

Paths in this table are relative to `app/src/main/java/com/github/damontecres/wholphin/`
or `app/src/main/` where that is obvious.

## All added, modified, removed and renamed files

Generated with `python3 docs/tools/nilumio_changes.py list`. Please regenerate after each change.

| Status | File | Changed |
|---|---|---|
| removed | `.github/ISSUE_TEMPLATE/00-bug.yml` | 2026-10-22 |
| removed | `.github/ISSUE_TEMPLATE/01-playback.yml` | 2026-10-22 |
| added | `.github/ISSUE_TEMPLATE/bug.yml` | 2026-10-22 |
| modified | `.github/ISSUE_TEMPLATE/config.yml` | 2026-10-22 |
| added | `.github/ISSUE_TEMPLATE/idea.yml` | 2026-10-22 |
| modified | `.gitignore` | 2026-10-22 |
| modified | `CONTRIBUTING.md` | 2026-10-22 to 2026-10-23 |
| added | `NILUMIO_CHANGES.md` | 2026-10-22 to 2026-10-23 |
| modified | `README.md` | 2026-10-22 to 2026-10-23 |
| added | `SECURITY.md` | 2026-10-22 |
| added | `THIRD_PARTY_NOTICES.md` | 2026-10-22 to 2026-10-23 |
| modified | `app/build.gradle.kts` | 2026-10-22 |
| modified | `app/src/debug/res/values/strings.xml` | 2026-10-22 |
| added | `app/src/firetv/AndroidManifest.xml` | 2026-10-22 |
| modified | `app/src/main/AndroidManifest.xml` | 2026-10-22 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/MainActivity.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/WholphinApplication.kt` | 2026-10-22 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/preferences/AppPreference.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/services/AppUpgradeHandler.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/services/NavDrawerService.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/services/hilt/AppModule.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/services/syncplay/SyncPlayManager.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/services/syncplay/SyncPlayPlayer.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/BannerCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/ChapterCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/DiscoverItemCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/GenreCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/GridCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/ItemCardImage.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/SeasonCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/StudioCard.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/cards/ViewMoreCard.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/components/Nilumio.kt` | 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/components/NilumioIcons.kt` | 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/components/NilumioMark.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/components/StartupIntro.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/main/HomePage.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/main/settings/HomeSettingsAddRow.kt` | 2026-10-22 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/main/settings/HomeSettingsDestination.kt` | 2026-10-22 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/main/settings/HomeSettingsGenreList.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/main/settings/HomeSettingsPage.kt` | 2026-10-22 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/main/settings/HomeSettingsViewModel.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/nav/Destination.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavDrawer.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/nav/NilumioDrawer.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/NextUpEpisode.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/PlaybackPage.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/overlay/Lichtkante.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/overlay/PlaybackController.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/overlay/PlaybackControls.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/overlay/PlaybackOverlay.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/playback/overlay/SeekBar.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/preferences/NavDrawerPreference.kt` | 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/preferences/NavMenuEditor.kt` | 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/preferences/PreferencesViewModel.kt` | 2026-10-22 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/setup/PinEntry.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/setup/SwitchServerContent.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/setup/SwitchUserContent.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/setup/UserList.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/shuffle/ShufflePage.kt` | 2026-10-23 |
| added | `app/src/main/java/com/github/damontecres/wholphin/ui/syncplay/SyncPlayPage.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/theme/Type.kt` | 2026-10-22 to 2026-10-23 |
| modified | `app/src/main/java/com/github/damontecres/wholphin/ui/theme/colors/PurpleThemeColors.kt` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/res/drawable-nodpi/nilumio_intro_final.png` | 2026-10-22 |
| added | `app/src/main/res/drawable/nilumio_player_audio.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_cc.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_next.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_pause.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_play.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_prev.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_settings.xml` | 2026-10-23 |
| added | `app/src/main/res/drawable/nilumio_player_skip.xml` | 2026-10-23 |
| added | `app/src/main/res/font/outfit_light.ttf` | 2026-10-23 |
| added | `app/src/main/res/font/outfit_regular.ttf` | 2026-10-23 |
| removed | `app/src/main/res/mipmap-anydpi-v26/ic_banner.xml` | 2026-10-22 |
| renamed | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` to `app/src/main/res/mipmap-anydpi-v26/ic_nilumio.xml` | 2026-10-22 |
| renamed | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` to `app/src/main/res/mipmap-anydpi-v26/ic_nilumio_round.xml` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-hdpi/ic_launcher.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-hdpi/ic_launcher_foreground.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` | 2026-10-22 |
| added | `app/src/main/res/mipmap-hdpi/ic_nilumio.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-hdpi/ic_nilumio_foreground.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-hdpi/ic_nilumio_round.png` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-mdpi/ic_launcher.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-mdpi/ic_launcher_foreground.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` | 2026-10-22 |
| added | `app/src/main/res/mipmap-mdpi/ic_nilumio.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-mdpi/ic_nilumio_foreground.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-mdpi/ic_nilumio_round.png` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xhdpi/ic_banner.png` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xhdpi/ic_banner_foreground.png` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xhdpi/ic_nilumio.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xhdpi/ic_nilumio_banner.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xhdpi/ic_nilumio_foreground.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xhdpi/ic_nilumio_round.png` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xxhdpi/ic_nilumio.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xxhdpi/ic_nilumio_foreground.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xxhdpi/ic_nilumio_round.png` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp` | 2026-10-22 |
| removed | `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xxxhdpi/ic_nilumio.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xxxhdpi/ic_nilumio_foreground.png` | 2026-10-22 |
| added | `app/src/main/res/mipmap-xxxhdpi/ic_nilumio_round.png` | 2026-10-22 |
| added | `app/src/main/res/raw/nilumio_intro.mp4` | 2026-10-22 |
| modified | `app/src/main/res/values-cs/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-da/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-de/strings.xml` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/res/values-de/strings_nilumio.xml` | 2026-10-23 |
| modified | `app/src/main/res/values-es/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-et/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-fr/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-hr/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-in/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-it/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-iw/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-nb-rNO/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-nl/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-nn/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-pl/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-pt-rBR/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-pt/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-ru/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-sk/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-sl/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-sq/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-sv/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-th/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-tr/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-uk/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-zh-rCN/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values-zh-rTW/strings.xml` | 2026-10-22 |
| modified | `app/src/main/res/values/ic_launcher_background.xml` | 2026-10-22 |
| modified | `app/src/main/res/values/strings.xml` | 2026-10-22 to 2026-10-23 |
| added | `app/src/main/res/values/strings_nilumio.xml` | 2026-10-23 |
| modified | `app/src/main/res/values/themes.xml` | 2026-10-22 |
| added | `app/src/test/java/com/github/damontecres/wholphin/services/syncplay/SyncPlayTimeTest.kt` | 2026-10-23 |
| added | `app/src/test/java/com/github/damontecres/wholphin/ui/playback/overlay/LichtkanteTimecodeTest.kt` | 2026-10-23 |
| added | `app/src/test/java/com/github/damontecres/wholphin/ui/shuffle/ShuffleWeightTest.kt` | 2026-10-23 |
| added | `docs/OWN_CODE.md` | 2026-10-22 to 2026-10-23 |
| added | `docs/UPSTREAM.md` | 2026-10-22 to 2026-10-23 |
| added | `docs/tools/nilumio_changes.py` | 2026-10-22 |
| modified | `gradle.properties` | 2026-10-22 |
| added | `store/NordlichtArt.java` | 2026-10-22 |
| added | `store/amazon-appstore.md` | 2026-10-22 |
| added | `store/amazon/firetv-background-1920x1080.png` | 2026-10-22 |
| added | `store/amazon/firetv-icon-1280x720.png` | 2026-10-22 |
| added | `store/amazon/icon-114.png` | 2026-10-22 |
| added | `store/amazon/icon-512.png` | 2026-10-22 |
| added | `store/amazon/promo-1024x500-de.png` | 2026-10-22 |
| added | `store/amazon/promo-1024x500-en.png` | 2026-10-22 |
| added | `store/brand/Outfit-OFL.txt` | 2026-10-22 |
| added | `store/brand/banner-1280x720.svg` | 2026-10-22 |
| added | `store/brand/banner-320x180.svg` | 2026-10-22 |
| added | `store/brand/icon-foreground.svg` | 2026-10-22 |
| added | `store/brand/icon-round.svg` | 2026-10-22 |
| added | `store/brand/icon-square.svg` | 2026-10-22 |
| added | `store/brand/icon-store.svg` | 2026-10-22 |
| added | `store/brand/nilumio-wortmarke-dunkel.svg` | 2026-10-22 |
| added | `store/brand/nilumio-wortmarke-hell.svg` | 2026-10-22 |
| added | `store/brand/nilumio-zeichen.svg` | 2026-10-22 |
| added | `store/icon-512.png` | 2026-10-22 |
| added | `store/listing.md` | 2026-10-22 |
| added | `store/play-console.md` | 2026-10-22 |
| added | `store/play/feature-graphic-1024x500-de.png` | 2026-10-22 |
| added | `store/play/feature-graphic-1024x500-en.png` | 2026-10-22 |
| added | `store/play/icon-512.png` | 2026-10-22 |
| added | `store/play/tv-banner-1280x720.png` | 2026-10-22 |
| added | `store/tv-banner-1280x720.png` | 2026-10-22 |
| added | `store/website/datenschutz/index.html` | 2026-10-22 to 2026-10-23 |
