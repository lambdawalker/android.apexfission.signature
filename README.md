# Sworn Ink Android signatures

Development Android library for P-256 statement signing with hardware attestation and an Android strong biometric prompt for every operation. Server-selected StrongBox or TEE policy is enforced without silent fallback. JWT issuance and email verification belong to the host.

**No confirmed stable release.** Build from source using [installation instructions](IMPORT.md).

- [Human documentation](sites/README.md): English/Spanish HTML and raw Markdown, generated from the same guides.
- [Agent integration guide](docs/agents/index.md)
- [Compiled host integration](examples/integration/src/main/kotlin/io/apexfission/signature/example/SigningIntegration.kt)
- [Authoritative service protocol](https://github.com/lambdawalker/go.apexfission.aws.signature/blob/feat/sworn-ink-signatures/docs/agents/protocol.md)
- [Device validation and security limitations](docs/agents/testing.md)

```sh
./gradlew :protocol:test :signature:lintDebug :signature:assembleDebug :signature:assembleDebugAndroidTest :examples:integration:assembleDebug
python3 scripts/docs.py verify
```

This is headless infrastructure. The operating system owns the biometric prompt; this project owns no Compose UI or screenshots. Hardware enforcement requires physical device validation. Cryptographic evidence does not establish legal identity or enforceability.
