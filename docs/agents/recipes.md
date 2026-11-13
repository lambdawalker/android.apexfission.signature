# Recipes — Development

The compiled integration fixture is `examples/integration/src/main/kotlin/io/apexfission/signature/example/SigningIntegration.kt`. It defines the transport boundary explicitly; the host supplies a real access-token provider and HTTP implementation. Build it with `./gradlew :examples:integration:assembleDebug`. It adds no application screen.

Registration: ask for a `register` challenge with `keys.capabilities()`; decode the padded base64 nonce; check expiry and suite; generate a new alias on a worker; persist alias/suite against exact issuer/subject only after server acceptance; build a registration statement using `key.keyId`; sign from the resumed activity; submit `challengeId`, `certificateChain` and `signature`. Export certificates before prompting and preserve the key until server acceptance/rejection is known. A failed registration may leave an unregistered local key; explicitly delete it when abandoning registration.

Signing: reopen the persisted alias with its expected suite on a worker; hash exact document bytes; ask for a `sign` challenge with keyId/documentSHA256; reconstruct all fields using the authenticated issuer/subject; compare server statement byte-for-byte; sign; submit challengeId/document/signature. Retry exact request bytes only; a new or changed document requires a new challenge and biometric operation.

Cancellation: call `signer.cancel()` on the main thread. Backgrounding, rotation or destruction also cancels. Discard any pending network action tied to a failed prompt. A host that navigates immediately after authentication must process the delivered signature before losing its operation state. No key is deleted by cancellation.

Recovery: enrollment changes or secure-lock reset can invalidate a key. Remove its local account binding, request a new registration challenge and create a fresh alias. Never reuse an attestation nonce or silently relabel a StrongBox key as TEE. Keep the old server registration for audit/revocation as required by the host's policy; the library has no server-side deletion API.
