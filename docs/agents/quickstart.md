# Quickstart — Development

Use the source installation in [Installation](../../IMPORT.md). The compiled `SigningIntegration` fixture defines request/response fields, nonce validation, exact statement comparison and document snapshotting. Add a real HTTPS `SwornInkApi` and `AccessTokens` implementation; configure the server's trusted issuer, audience, scopes, app package and signing certificate. No mock identity or email endpoint is included.

In your FragmentActivity, construct `HardwareKeys(applicationContext)` and one `BiometricSigner(this, keys)` in `onCreate`. The following is the complete operation sequence using the compiled fixture. `worker` is a host-owned executor; error rendering, document approval and account persistence are host responsibilities. Start authentication only while the activity is resumed. Methods may throw during preparation or before starting the prompt; route those failures to your host error UI.

```kotlin
import io.apexfission.signature.*
import io.apexfission.signature.protocol.*
import io.apexfission.signature.example.*

// FragmentActivity.onCreate:
val keys = HardwareKeys(applicationContext)
val signer = BiometricSigner(this, keys)
val integration = SigningIntegration(keys, signer, api, tokens, Account(issuer, subject))

// User explicitly requests registration; worker is owned/shut down by host.
worker.execute {
    val prepared = integration.prepareRegistration("account-key-1")
    runOnUiThread {
        integration.authenticateRegistration(prepared, PromptText("Register signing key")) { result ->
            result.onSuccess { request ->
                worker.execute {
                    integration.submitRegistration(request)
                    // After server acceptance persist prepared.key.alias/suite/keyId for issuer+subject.
                }
            }.onFailure { failure -> /* render host error; no submission */ }
        }
    }
}

// After document approval and successful registration, start a separate operation:
worker.execute {
    val prepared = integration.prepareSigning(alias, selectedSuite, approvedDocumentBytes)
    runOnUiThread {
        integration.authenticateSigning(prepared, PromptText("Sign approved document")) { result ->
            result.onSuccess { request -> worker.execute { integration.submitSignature(request) } }
                .onFailure { failure -> /* render host error; no submission */ }
        }
    }
}
// User navigation/cancel: signer.cancel() on the main thread.
// Signer also cancels onStop and closes onDestroy; never retain it across activities.
```

The snippet intentionally leaves host-owned variables (`api`, `tokens`, issuer/subject, worker, stored alias/suite and approved bytes) as integration inputs. The complete compilable fixture source is in `examples/integration/src/main/kotlin/io/apexfission/signature/example/SigningIntegration.kt`; it is not an installable app or a production networking stack. Include that fixture module only if adopting its host transport boundary, otherwise implement the same flow with the core API.

Registration sends `challengeId`, leaf-first base64 DER `certificateChain` and base64 DER ECDSA `signature`. Signing sends `challengeId`, base64 `document` and base64 `signature`. Include the external bearer token on every request. Store/retry exact request bytes after ambiguous network failure; do not replay a challenge with different bytes. The server enforces its five-minute expiry and one-time policy. A successful HTTP response is the server's evidence outcome; prompt success alone is not registration or evidence completion.
