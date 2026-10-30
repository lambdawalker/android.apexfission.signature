# Inicio rápido — Desarrollo

Use [Instalación](installation.md). El fixture compilado `SigningIntegration` define campos HTTP, validación de nonce, comparación exacta y copia del documento. El host implementa HTTPS `SwornInkApi` y `AccessTokens`; el servidor configura issuer/audience/scopes y paquete/firmante permitidos. No hay identidad simulada ni endpoint de correo.

En `onCreate` de FragmentActivity cree `HardwareKeys(applicationContext)` y una instancia `BiometricSigner(this, keys)`. La secuencia completa usa el fixture compilado. `worker` es un executor del host, que también presenta errores, aprobación del documento y persistencia. Autentique solo con actividad resumed. Preparación e inicio pueden lanzar excepciones; muéstrelas mediante la interfaz de errores del host.

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

Las variables `api`, `tokens`, issuer/subject, worker, alias/suite persistidos y bytes aprobados son entradas del host. El fixture completo compilable está en `examples/integration/src/main/kotlin/io/apexfission/signature/example/SigningIntegration.kt`; no es una aplicación instalable ni una pila de red de producción. Incluya ese módulo solo si adopta su interfaz de transporte; también puede implementar el flujo con la API principal.

Registro envía `challengeId`, `certificateChain` DER base64 con hoja primero y `signature` ECDSA DER base64. Firma envía `challengeId`, `document` base64 y `signature` base64. Cada solicitud lleva token externo. Conserve/reintente exactamente los mismos bytes tras fallo de red ambiguo; no use un desafío con bytes distintos. El servidor impone caducidad de cinco minutos y consumo único. El éxito HTTP refleja el resultado de pruebas del servidor; aprobar el diálogo no completa registro ni conservación de pruebas.
