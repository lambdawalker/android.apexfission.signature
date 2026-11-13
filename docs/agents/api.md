# Kotlin API — Development

Imports: `io.apexfission.signature.*` and `io.apexfission.signature.protocol.*`. Public declarations are the contract; no reflection or serializer schema is used.

| Declaration | Contract |
| --- | --- |
| `Suite.ES256_STRONGBOX`, `Suite.ES256_TEE` | P-256/SHA-256; enum name is wire value; explicit hardware policy |
| `Operation.REGISTER`, `Operation.SIGN` | `wire` is `register` or `sign` |
| `Statement(operation, suite, challengeId, nonce, issuer, subject, keyId, documentSHA256 = "")` | 32-byte nonce copied on construction; nonempty identity/challenge; lowercase 64-character hex digests; register requires empty document digest, sign requires digest |
| `Statement.encode(): ByteArray` | Fresh exact length-prefixed bytes; no normalization of identity strings |
| `sha256(bytes): String` | Lowercase SHA-256 hex; SPKI fingerprint or document hash depending on input |
| `wireBase64(bytes): String` | Standard padded base64 without line breaks |
| `HardwareKeys(context)` | Uses application context; Android Keystore ownership stays with app UID |
| `capabilities(): List<Suite>` | Advisory supported order; empty without strong biometrics; StrongBox first when feature advertised; TEE candidate still requires generation check |
| `generate(alias, suite, attestationNonce): HardwareKey` | Worker thread; fresh alias (1..80 ASCII letters/numbers/dot/underscore/hyphen); 32-byte nonce; blocks during keygen; throws on missing strong biometric, unavailable/mismatched hardware, bad inputs or platform errors; failed generated key is deleted |
| `open(alias, suite): HardwareKey` | Worker thread; recover persisted alias with explicit expected suite; checks hardware/auth policy and computes fingerprint |
| `certificateChain(key): List<ByteArray>` | Worker thread; rechecks key binding and returns leaf-first DER; may fail on missing/incomplete chain; server validates trust |
| `delete(alias)` | Worker thread; idempotently delete only prefixed app-owned alias; irreversible local key deletion; never called by cancellation |
| `HardwareKey.alias`, `.suite`, `.keyId` | Read-only handle; internal constructor; persist alias/account/suite, reopen rather than serialize handle |
| `BiometricSigner(activity, keys)` | Main thread, construct once per activity; lifecycle observer cancels at stop and closes at destruction |
| `sign(key, statement, prompt, callback)` | Main thread, resumed activity; exact keyId/suite match required; signature initialized before CryptoObject authentication; one operation; callback once with `Result<ByteArray>`; setup failures delivered synchronously |
| `cancel()` | Main thread; idempotent; completes pending operation with `CancellationException` and cancels OS prompt |
| `close()` | Main thread; idempotent cancel and detach lifecycle observer; closed signer cannot be reused |

`PromptText(title, subtitle = null, cancel = "Cancel")` configures OS prompt labels. No custom UI is owned. Nonempty title/cancel text required. Use localized strings in the host. No device credential fallback is configured.

`HardwareUnavailableException` means requested hardware/auth capability cannot be satisfied. `InvalidatedKeyException` means permanent invalidation and requires a newly challenged key. `AuthenticationException(code, message)` preserves Android biometric error codes (lockout, user cancel, hardware errors, etc). Lifecycle/explicit cancellation uses `java.util.concurrent.CancellationException`. Other provider/security errors remain failures; no error downgrades to software or another suite. Bad caller arguments throw `IllegalArgumentException`; main-thread misuse throws `IllegalStateException`. Nonmatching biometric attempts do not complete the callback; terminal OS errors do.

Per-use KeyInfo duration accepts only -1 or 0 and rejects positive windows. The KeyInfo API documents -1, while Keystore2 initializes the value to 0 when the hardware authTimeout tag is absent. Generation always requests `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`; server attestation remains authoritative about tag absence. See [AOSP KeyInfo](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/keystore/java/android/security/keystore/KeyInfo.java) and [Keystore2 extraction](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/keystore/java/android/security/keystore2/AndroidKeyStoreSecretKeyFactorySpi.java).
