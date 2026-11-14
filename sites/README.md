# Human documentation — Development

Documentation is built by the shared `android-repo-docs` CLI using Astro/Starlight.
The exact source revision is pinned in `.project/docs/config.yaml` and the generated
`docs.sh` / `docs.bat` launchers. Node.js 24 with npm and Rust/Cargo are required;
there are no Python scripts, Python packages or repository-owned site renderer.

The source pin `1da53392b262c7c3129d2bd645dfd7b34ead81e1` is deliberate: tagged CLI
releases through v0.1.28 do not include all compatibility aliases and filesystem
staging behavior required by this migration. The build uses the pinned source,
not the binary version string alone. Do not replace it with a tag until these
site/output contracts pass against that release.

```sh
sh ./docs.sh --version
sh ./docs.sh bootstrap check
sh ./docs.sh check
sh ./docs.sh history verify
node --test tools/docs.test.mjs
node tools/docs.mjs verify
node tools/docs.mjs verify --base /android.apexfission.signature/
sh ./docs.sh serve
```

Use `docs.bat` on Windows. The CLI owns rendering, Markdown preparation, scoped
links, language navigation, search, raw output and build-commit provenance.
`tools/docs.mjs` only checks this project's source-review manifest, reviewed Spanish
hashes, identical code fences, no-release catalog and legacy/output contracts.
It delegates the complete build to the pinned CLI. Unit tests deliberately mutate
isolated source/translation fixtures to ensure unreviewed changes fail before
rendering; they do not duplicate a Markdown or Astro implementation.

Canonical guides live in `docs/agents`, installation at root `IMPORT.md`, and
Spanish in `docs/es`. Installation was relocated because the shared CLI owns its
installation route. Legacy `/<language>/signature/development/<guide>.html` human
URLs and `/raw/<language>/signature/development/<guide>.md` endpoints remain valid.
Both languages retain all ten original guides, plus the Development release
boundary page. `catalog.json` matches the reviewed `docs/releases.json` exactly.

After reviewing Spanish meaning against English, use
`node tools/docs.mjs accept-translations`, inspect the changed hashes and verify.
This command records review; it never translates text. After reviewing documentation
against changed API/example input, use `node tools/docs.mjs accept-source`.
Verification rejects stale review state. A build can demonstrate explicit English
fallback from the same Development checkout; verification requires the reviewed
Spanish pages to be fresh.

The default verifier builds a root-served artifact. `--base` selects a project
prefix, or `ANDROID_REPO_DOCS_BASE` supplies it. CI verifies both `/` and
`/android.apexfission.signature/` and uploads separate complete site artifacts.
A successful project-prefix artifact is ready for a separately authorized Pages
deployment. Validation never deploys a site or publishes a library.

There are no confirmed package publications or release archives. Source integration
remains the installation method. Builds do not invent release history, physical
device results or screenshot evidence; the library owns no UI screenshot target.
