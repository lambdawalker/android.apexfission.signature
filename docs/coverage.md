# Development API coverage and ownership

| Feature | Canonical/Spanish guide | Compiled example | Verification |
| --- | --- | --- | --- |
| Length-prefixed statement, hash, base64 | agents/api.md; es/api.md | SigningIntegration preparation | StatementTest + shared JSON vector |
| Advisory capabilities and server selection | agents/concepts.md; es/concepts.md | prepareRegistration / selected | HardwareKeyTest; device matrix |
| Fresh attested key, alias protection, exact suite | agents/api.md; es/api.md | prepareRegistration | HardwareKeyTest, KeyPolicyTest |
| Recover key and export chain | agents/recipes.md; es/recipes.md | prepareSigning / prepareRegistration | HardwareKeyTest; process recovery manual |
| CryptoObject signing and errors | agents/api.md; es/api.md | authenticateRegistration / authenticateSigning | CancellationTest; physical success/manual matrix |
| Cancellation/lifecycle | agents/concepts.md; es/concepts.md | activity-scoped signer | CancellationTest; rotation/manual matrix |
| Transport and account binding | agents/quickstart.md; es/quickstart.md | SwornInkApi, Account, request fields | Compiled integration; live server not run |
| Installation, migration, limits | ../IMPORT.md; es/IMPORT.md and matching guides | Gradle project dependency | Gradle compilation and docs link/hash checks |

Source defines APIs; tests define observed behavior. docs/agents and IMPORT.md are canonical prose; docs/es translates the same Development scope. Release catalog contains no confirmed release. tools/docs.mjs delegates HTML/raw rendering to shared Astro and checks configured legacy aliases; generated output is ignored. docs/source-manifest.json detects reviewed input drift; docs/es/translations.json detects English changes requiring translation review. These hashes are review reminders, not API compatibility proofs.

No owned Compose/application UI exists. OS prompt behavior requires instrumentation/manual device validation; no fabricated screenshot or preview exists. No claimed successful physical validation until a report records the matrix in agents/testing.md.
