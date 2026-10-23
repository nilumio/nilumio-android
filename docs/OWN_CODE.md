# nilumio's own work in this repository

An overview for maintainers: how much of nilumio for Android TV is Wholphin and how much
was added or changed by nilumio. Counted with `git diff --numstat` against the upstream
branch point (`git merge-base HEAD upstream/main`), as of 23 October 2026.

## By category

"New own files" are files that do not exist in Wholphin. "Upstream files touched" are
Wholphin files that nilumio modified, renamed or removed; for those, the added and
removed lines are counted. Binary files (images, video) are counted as files only.

| Category | New own files | Lines in new files | Upstream files touched | Lines added there | Lines removed there |
|---|---:|---:|---:|---:|---:|
| code | 14 | 2862 | 37 | 351 | 418 |
| tests | 3 | 158 | 0 | 0 | 0 |
| build and configuration | 9 | 140 | 6 | 47 | 6 |
| UI texts and translations | 2 | 106 | 28 | 129 | 129 |
| images and media (32 binary new, 17 binary changed) | 41 | 9 | 20 | 2 | 11 |
| documentation and store texts | 12 | 1181 | 5 | 85 | 149 |
| **total** | **81** | **4456** | **96** | **614** | **713** |

## In proportion

- Wholphin's app code at the branch point: about **92,300 lines of Kotlin** in `app/src/main/java`.
- nilumio added about **2,950 lines of Kotlin** there (about 2,600 in 12 new files, about 350 in 37 modified Wholphin files) and removed about 420 Wholphin lines. That is roughly **3 %** of the app code.
- Own code lives in own files wherever possible; the Wholphin files mostly only call it (marked with `// nilumio`), which keeps upstream merges small.
- Largest own pieces: SyncPlay (`services/syncplay/`, `ui/syncplay/`, about 950 lines, partly ported from jellyfin-web), player controls "Lichtkante" (`ui/playback/overlay/Lichtkante.kt` and the calls into it, about 490 lines), Shuffle (about 300 lines), side menu with icons and menu editor (about 510 lines), shared theme and logo (`ui/components/Nilumio.kt`, `NilumioMark.kt`, about 200 lines), start-up intro (about 140 lines), genre rows (about 170 lines).
- Unit tests for the pure logic (shuffle weighting, SyncPlay time sync, timecode): 3 files, about 160 lines.
- Everything else is artwork (icons, banner, intro video), store texts, the privacy policy, and this documentation.

## Keeping these numbers current

```sh
python3 docs/tools/nilumio_changes.py stats   # table above
python3 docs/tools/nilumio_changes.py list    # file list for NILUMIO_CHANGES.md
```
