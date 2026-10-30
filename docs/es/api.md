# API Kotlin — Desarrollo

Importaciones: `io.apexfission.signature.*` y `io.apexfission.signature.protocol.*`. Las declaraciones públicas definen el contrato; no hay modelos de serialización ni reflexión.

| Declaración | Contrato |
| --- | --- |
| `Suite.ES256_STRONGBOX`, `Suite.ES256_TEE` | P-256/SHA-256; nombre enum como valor de protocolo; política explícita |
| `Operation.REGISTER`, `Operation.SIGN` | `wire`: `register` o `sign` |
| `Statement(operation, suite, challengeId, nonce, issuer, subject, keyId, documentSHA256 = "")` | Copia nonce de 32 bytes; identidad/desafío no vacíos; resúmenes hexadecimales minúsculos de 64 caracteres; resumen vacío para registro, obligatorio para firma |
| `Statement.encode(): ByteArray` | Bytes nuevos con prefijos de longitud; no normaliza identidades |
| `sha256(bytes): String` | SHA-256 hexadecimal minúsculo; recibe SPKI o documento según uso |
| `wireBase64(bytes): String` | Base64 estándar con relleno, sin saltos |
| `HardwareKeys(context)` | Conserva application context; Keystore pertenece al UID del host |
| `capabilities(): List<Suite>` | Orden orientativo; vacía sin biometría fuerte; StrongBox primero cuando se anuncia; TEE aún requiere comprobar generación |
| `generate(alias, suite, attestationNonce): HardwareKey` | Hilo de trabajo; alias nuevo (1..80 letras/números ASCII, punto, guion bajo o guion) y nonce de 32 bytes; bloquea; falla sin capacidad o por errores de proveedor; elimina la clave recién generada si no cumple política |
| `open(alias, suite): HardwareKey` | Hilo de trabajo; recupera alias persistido con suite explícita; comprueba política y calcula fingerprint |
| `certificateChain(key): List<ByteArray>` | Hilo de trabajo; comprueba vínculo y devuelve DER con hoja primero; falla si falta cadena; el servidor valida confianza |
| `delete(alias)` | Hilo de trabajo; elimina idempotentemente el alias con prefijo propio; operación local irreversible; cancelar no elimina |
| `HardwareKey.alias`, `.suite`, `.keyId` | Handle de solo lectura y constructor interno; persista alias/cuenta/suite y vuelva a abrir |
| `BiometricSigner(activity, keys)` | Hilo principal, una instancia por actividad; cancela al detener y cierra al destruir |
| `sign(key, statement, prompt, callback)` | Actividad resumed e hilo principal; exige keyId/suite correctos; inicializa Signature antes de CryptoObject; una operación; callback único con `Result<ByteArray>`; errores iniciales síncronos |
| `cancel()` | Hilo principal; idempotente; completa con `CancellationException` y cancela el diálogo |
| `close()` | Hilo principal; cancela y separa observador; no reutilizable después |

`PromptText(title, subtitle = null, cancel = "Cancel")` configura etiquetas del diálogo del sistema. title/cancel no pueden estar vacíos; el host aporta textos localizados. No se permite credencial del dispositivo como alternativa.

`HardwareUnavailableException` indica capacidad insuficiente. `InvalidatedKeyException` exige registrar otra clave. `AuthenticationException(code, message)` conserva el código Android, incluidos bloqueo y cancelación del usuario. Cancelación explícita o de ciclo de vida produce `java.util.concurrent.CancellationException`. Otros errores de seguridad/proveedor se entregan como fallos; nunca cambian la suite. Argumentos incorrectos: `IllegalArgumentException`; hilo incorrecto: `IllegalStateException`. Un intento biométrico no coincidente no termina el callback; un error terminal sí.

La duración por operación de KeyInfo solo admite -1 o 0; rechaza ventanas positivas. La API documenta -1, pero Keystore2 inicia el valor en 0 cuando no existe authTimeout. La generación solicita siempre `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`; la atestación validada por servidor determina la ausencia real de esa etiqueta. Véanse [AOSP KeyInfo](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/keystore/java/android/security/keystore/KeyInfo.java) y [extracción Keystore2](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/keystore/java/android/security/keystore2/AndroidKeyStoreSecretKeyFactorySpi.java).
