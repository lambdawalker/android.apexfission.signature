# Human documentation — Development

No confirmed publication exists, so the root opens the visibly labeled Development overview. English and Spanish contain the full consumer guide set. There is no historical archive yet; `docs/releases.json` records this boundary. Do not add releases without confirmed metadata and retained immutable documentation inputs.

```sh
python3 -m pip install -r sites/requirements.txt
python3 scripts/docs.py build
python3 -m http.server 8000 --directory sites/dist
# http://localhost:8000/ or /en/signature/development/index.html
python3 scripts/docs.py verify
```

The renderer uses canonical `docs/agents/*.md`, translations `docs/es/*.md` and `docs/translations.json` hashes. It clears dist, renders HTML plus untouched raw Markdown in matching locale/module/version routes, checks links, and adds `llms.txt` discovery. A stale/missing translation explicitly falls back to the same Development English source. Navigation preserves the guide when changing language. A single module/channel needs no artificial selector. For a project subpath, all generated links are relative.

After deliberately reviewing each Spanish page against English, run `python3 scripts/docs.py accept-translations`, inspect manifest changes, then verify. Never update hashes to hide an untranslated source change. API/input freshness is checked by `docs/source-manifest.json`; after reviewing guides against source, run `python3 scripts/docs.py accept-source`.

The site has no JavaScript runtime dependencies or Node build. Markdown/Pygments are documentation-only pinned Python dependencies. It uses semantic HTML, accessible native navigation, responsive layout and code highlighting. Validation CI uploads the output; it never deploys a site or publishes a package. For hosting, the owner may serve the `sites/dist` artifact on GitHub Pages after separately authorizing/configuring deployment. No custom domain/provider is assumed.

No visual browser inspection or physical-device result is implied by link validation. The OS biometric prompt is not a library-owned screenshot target.
