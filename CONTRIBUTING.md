<!-- nilumio: start of fork section -->
# Contributing to nilumio

Thank you for helping. A few rules keep this fork easy to maintain:

1. **Upstream first.** If a bug or idea also concerns Wholphin itself, please report it to [Wholphin](https://github.com/damontecres/Wholphin) first. Fixes that land there reach nilumio with the next upstream merge.
2. **Small changes.** One topic per pull request, with a short description of what you tested and on which device.
3. **Own code separate.** New nilumio features go into new files where possible. When a Wholphin file has to change, keep the change small (ideally just a call into an own file) and mark it with a `// nilumio` comment, so upstream merges stay simple (see [docs/UPSTREAM.md](docs/UPSTREAM.md)).
4. **Record changes.** If you add or modify a file, regenerate [NILUMIO_CHANGES.md](NILUMIO_CHANGES.md) with `python3 docs/tools/nilumio_changes.py list > /tmp/list.md` and update the table. Code from other projects goes into [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) with its licence.
5. **Licence.** By submitting a pull request you agree that your contribution is licensed under the [GPL-2.0](LICENSE).
6. **No secrets.** Never commit keystores, passwords, `local.properties`, personal paths or e-mail addresses.
7. **Texts.** English texts use British spelling (colour, favourite, licence).

Wholphin's own contribution guide follows below; its code of conduct and AI/LLM policy apply here too.

---
<!-- nilumio: end of fork section. Original Wholphin CONTRIBUTING.md follows. -->

# Contributing to Wholphin

We appreciate your interest in contributing to Wholphin!

## Code of Conduct

We follow Jellyfin's example by adhering to their [Code of Conduct](https://jellyfin.org/docs/general/community-standards/#code-of-conduct).

## How to contribute

You can contribute in several ways:
* File bug reports
* Request new features
* Propose code changes via pull request
* Submit translations to https://translate.codeberg.org/engage/wholphin/

In all cases, be sure to fully explain the issue.

### Code change proposals

You acknowledge that, if applicable, submitting and subsequent acceptance of any pull request you, the code contributor of the pull request, agree and acknowledge that the code will be licensed under [GPLv2](LICENSE).

Please be thoughtful when contributing code and consider the long term maintenance/support aspect for any new features.

See the [developer's guide](./DEVELOPMENT.md) for more information.


#### Policy for AI or LLMs

Wholphin has adopted the [Jellyfin LLM/"AI" Development Policy](https://jellyfin.org/docs/general/contributing/llm-policies/). AI/LLM assisted code contributions are permitted under the policy, but the author must undertand and be able to explain the code changes in their own words.

At a minimum, you must disclose the usage any of AI/LLM assistance and you must write the pull request description _without_ AI assistance.
