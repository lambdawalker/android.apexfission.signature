# Human documentation — Development

The shared `android-repo-docs` CLI is pinned to corrective source commit `1da53392b262c7c3129d2bd645dfd7b34ead81e1` in `.project/docs/config.yaml` and the generated root launchers. [Shared tooling PR #37](https://github.com/lambdawalker/gradle.android-repo-setup/pull/37) supplies the migration aliases and filesystem staging fixes absent from the released v0.1.21 asset. Node.js 24 with npm, Python 3.12 and Rust/Cargo are documentation tooling prerequisites. The CLI owns Astro rendering, Markdown rewriting, language navigation, search, raw endpoints and commit provenance. Python has no third-party rendering dependencies.

```sh
./docs.sh bootstrap check
./docs.sh check
python3 -m unittest discover -s scripts -p 'test_*.py'
python3 scripts/docs.py verify
python3 -m http.server 8000 --directory sites/dist
# http://localhost:8000/en/signature/development/
```

`python3 scripts/docs.py verify` checks the deliberately reviewed `docs/source-manifest.json`, Spanish freshness and identical code fences before calling the shared renderer. Shared `site.aliases` configuration adds compatibility redirects for the old `/<language>/signature/development/<guide>.html` URLs, exposes shared raw output to `/raw/<language>/signature/development/<guide>.md`, and preserves `catalog.json` through checked static metadata. The verifier checks exact build-commit provenance. The overview's `index.html` already exists at its old URL. The ten original guides remain available in both languages; the shared renderer also includes a Development-only releases boundary page. CI uploads `sites/dist` as an artifact and never deploys or publishes.

Canonical guides live in `docs/agents`, with installation at root `IMPORT.md`; Spanish lives in `docs/es`, including `IMPORT.md`. Installation was relocated because the shared CLI reserves its installation route for `IMPORT.md`. Translation freshness now lives at the shared CLI's `docs/es/translations.json` path. Existing reviewed hashes were retained except for the two English pages whose installation link paths changed; Spanish received the same link-only edits. The source acceptance manifest is unchanged.

After deliberately reviewing each Spanish page against its English source, run `python3 scripts/docs.py accept-translations`, inspect the manifest changes, then verify. Never refresh hashes to hide an untranslated source change. After reviewing guides against changed API/example inputs, use `python3 scripts/docs.py accept-source`. Missing/stale translations fall back to English from the same Development checkout during a build; verification deliberately rejects stale review state.

The wrapper builds for a root-served artifact by default. Set `ANDROID_REPO_DOCS_BASE=/android.apexfission.signature/` when building for a project subpath. Use the wrapper when producing the final artifact so reviewed source/translation acceptance and the project output contracts are checked alongside shared rendering. `ANDROID_REPO_DOCS_BIN` can select a preinstalled binary built from the pinned corrective source instead of the launcher source build. This is a source pin, not a claim that the fixes are in the released v0.1.21 asset.

There are no confirmed publications or immutable release archives. `docs/releases.json` records that boundary, and the verifier rejects adding confirmed releases without a separate migration. No visual browser inspection or physical-device result is implied. The library owns no UI screenshot target.
