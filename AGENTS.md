# Contributor instructions

Development consumer contracts start at [docs/agents/index.md](docs/agents/index.md).

- Protocol wire authority is the Go service linked from the consumer guide. Keep the shared vector identical across repositories.
- Never fall back after a suite is selected. Never introduce software signing or device credential authentication.
- API behavior belongs to source and tests. Canonical guides live in docs/agents; Spanish in docs/es. Generated HTML/raw files live in ignored sites/dist.
- Run `./gradlew :protocol:test :signature:testDebugUnitTest :signature:lintDebug :signature:assembleDebug :signature:assembleDebugAndroidTest :examples:integration:assembleDebug` and `python3 scripts/docs.py verify`.
- Edit translations deliberately; after reviewing meaning, run `python3 scripts/docs.py accept-translations`. This updates source hashes and never translates text.
- No confirmed package release exists. Installation is a source dependency. Do not invent Maven coordinates, historical releases or device evidence.
- Headless: only Android BiometricPrompt UI. There are no owned composables, previews or screenshot baselines. Follow docs/agents/testing.md for physical device validation.
- Publication and site deployment are separate authorized operations; validation workflows never publish.
