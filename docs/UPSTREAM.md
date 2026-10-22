# Taking over upstream updates

nilumio for Android TV follows [Wholphin](https://github.com/damontecres/Wholphin).
Our work lives on the branch `nordlicht` (published as `main` on GitHub); Wholphin is the
remote `upstream`.

## One-time setup

```sh
git remote add upstream https://github.com/damontecres/Wholphin.git   # already set in the original checkout
git config remote.upstream.tagOpt --no-tags                          # keep Wholphin's tags out of our repo
```

## Regular update

```sh
git switch nordlicht
git status                      # must be clean
git fetch upstream
git log --oneline nordlicht..upstream/main    # what is new upstream
git merge upstream/main         # or a release tag, e.g. git merge v0.6.0
```

Use **merge**, not rebase: the branch is public, and a rebase would rewrite published history.

## Resolving conflicts

Conflicts should only appear in the places nilumio touched. The full list of
modified Wholphin files is in [NILUMIO_CHANGES.md](../NILUMIO_CHANGES.md); the most
likely hot spots are:

| Area | Files | Keep from nilumio |
|---|---|---|
| App identity and signing | `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/debug/res/values/strings.xml` | application ID `com.held0.nilumio`, version code from the commit count, signing block, icons |
| Theme | `ui/theme/Type.kt`, `ui/theme/colors/PurpleThemeColors.kt`, `res/values/themes.xml` | nilumio colours and typography |
| Navigation and home | `ui/nav/*`, `ui/main/HomePage.kt`, `ui/main/settings/HomeSettings*` | *Watch Party* entry, genre rows |
| Texts | `res/values*/strings.xml` | the app name and nilumio strings; take everything else from upstream |
| Docs | `README.md`, `CONTRIBUTING.md` | only the block between the `nilumio:` markers at the top |
| Issue templates | `.github/ISSUE_TEMPLATE/` | ours (upstream templates were removed on purpose) |

Rules of thumb:

- Inside a marked block (`// nilumio`, `// Nordlicht`, `<!-- nilumio -->`) keep ours, everywhere else take upstream.
- Files that only nilumio added (for example `services/syncplay/`, `ui/components/Nordlicht*`, `StartupIntro.kt`) never conflict, but may need adapting when upstream changes an API.
- Translations: take upstream's version, then re-apply the few nilumio strings.
- If a nilumio feature now exists upstream (for example SyncPlay), prefer upstream's version and remove ours.

## After the merge

```sh
./gradlew assembleDebug testDebugUnitTest
python3 docs/tools/nilumio_changes.py list    # refresh the table in NILUMIO_CHANGES.md
python3 docs/tools/nilumio_changes.py stats   # refresh the numbers in docs/OWN_CODE.md
git push origin nordlicht:main                # no --tags, no --force
```

Note the new upstream base commit in `README.md` (section *Origin and licence*).

## GitHub Actions

The workflows in `.github/workflows/` are Wholphin's and expect Wholphin's secrets.
They are left untouched to keep merges simple; disable Actions in the repository
settings of the nilumio repo (or delete the workflows there if you ever want CI of your own).
