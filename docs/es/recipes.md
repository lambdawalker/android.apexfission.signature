# Recetas — Desarrollo

La integración compilada está en `examples/integration/src/main/kotlin/io/apexfission/signature/example/SigningIntegration.kt`. Define el límite de transporte; el host implementa tokens y HTTP reales. Compile con `./gradlew :examples:integration:assembleDebug`. No añade pantallas.

Registro: solicite un desafío `register` con `keys.capabilities()`; decodifique nonce base64 con relleno y compruebe caducidad/suite. Genere un alias nuevo en un hilo de trabajo. Construya la declaración con `key.keyId` y firme desde una actividad resumed. Envíe challengeId, certificateChain y signature. Exporte certificados antes de mostrar el diálogo. Persista alias/suite con issuer/subject exactos después de aceptación del servidor. Un registro fallido puede dejar una clave local sin registrar; elimínela explícitamente al abandonar.

Firma: abra el alias persistido con la suite esperada en un hilo de trabajo; calcule el resumen exacto; solicite desafío `sign` con keyId/documentSHA256. Reconstruya todos los campos usando issuer/subject autenticados y compare bytes con el servidor. Firme y envíe challengeId/document/signature. Reintente solo los mismos bytes; cambiar documento requiere otro desafío y otra autenticación.

Cancelación: llame `signer.cancel()` en el hilo principal. Fondo, rotación y destrucción también cancelan. No envíe solicitudes ligadas a un diálogo fallido. Procese una firma ya entregada antes de perder estado por navegación. Cancelar no elimina claves.

Recuperación: nueva biometría o reinicio del bloqueo puede invalidar claves. Quite el vínculo local, solicite otro desafío y genere alias nuevo. No reutilice nonce ni cambie la etiqueta de StrongBox a TEE. El host decide auditoría y revocación de registros antiguos; la biblioteca no ofrece eliminación remota.
