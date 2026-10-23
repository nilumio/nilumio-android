# nilumio's own work in this repository

An overview for maintainers: how much of nilumio for Android TV is Wholphin and how much
was added or changed by nilumio. Counted with `git diff --numstat` against the upstream
branch point (`git merge-base HEAD upstream/main`), as of 22 October 2026.

## By category

"New own files" are files that do not exist in Wholphin. "Upstream files touched" are
Wholphin files that nilumio modified, renamed or removed; for those, the added and
removed lines are counted. Binary files (images, video) are counted as files only.

| Category | New own files | Lines in new files | Upstream files touched | Lines added there | Lines removed there |
|---|---:|---:|---:|---:|---:|
| code | 12 | 1710 | 20 | 230 | 89 |
| tests | 1 | 43 | 1 | 1 | 0 |
| build and configuration | 0 | 0 | 7 | 62 | 6 |
| UI texts and translations | 2 | 34 | 28 | 127 | 93 |
| images and media (24 binary new, 17 binary changed) | 33 | 9 | 20 | 2 | 11 |
| website | 2 | 153 | 0 | 0 | 0 |
| documentation and store texts | 10 | 971 | 5 | 86 | 149 |
| **total** | **60** | **2920** | **81** | **508** | **348** |

## In proportion

- Wholphin's app code at the branch point: about **92,300 lines of Kotlin** in `app/src/main/java`.
- nilumio added about **1,680 lines of Kotlin** there (1,450 in 10 new files, about 230 in 20 modified Wholphin files) and removed 89 Wholphin lines. That is roughly **2 %** of the app code.
- Largest own pieces: SyncPlay (*SyncPlay*, about 610 lines, partly ported from jellyfin-web), drawer icons and start-up intro (about 390 lines), genre rows (about 80 lines).
- Everything else is artwork (icons, banner, intro video), store texts, the privacy policy, and this documentation.

## Keeping these numbers current

```sh
python3 docs/tools/nilumio_changes.py stats   # table above
python3 docs/tools/nilumio_changes.py list    # file list for NILUMIO_CHANGES.md
```
