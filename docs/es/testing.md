# Validación — Desarrollo

Pruebas de host comparan bytes UTF-8 con prefijos contra el vector JSON compartido (`protocol/test-vectors/v1.json`), rechazan declaraciones malformadas y verifican fingerprint/base64. Instrumentación comprueba nivel generado, biometría por operación, invalidación solicitada, alias duplicados, exportación y rechazo explícito de StrongBox.

```sh
./gradlew :protocol:test :signature:testDebugUnitTest :signature:lintDebug :signature:assembleDebug :signature:assembleDebugAndroidTest :examples:integration:assembleDebug
# Physical API31+ device, enrolled strong biometric, secure lock screen:
./gradlew :signature:connectedDebugAndroidTest
python3 scripts/docs.py verify
```

Las pruebas pueden omitir escenarios si falta biometría inscrita; una omisión no demuestra cumplimiento. Emuladores no prueban confianza de hardware. No añada mecanismos para saltarse autenticación.

| Escenario físico | Resultado exigido |
| --- | --- |
| Registro StrongBox y TEE cuando existan | Nivel exacto, cadena confiable y posesión validada por servidor |
| Biometría válida para dos firmas | Dos diálogos; firmas verifican declaraciones exactas |
| Biometría incorrecta, botón negativo, atrás, bloqueo | Sin firma ante error terminal; fallo no terminal puede mantener diálogo |
| Cancelar, Home, rotar, destruir | Un callback de fallo; sin éxito tardío ni envío |
| Registrar nueva biometría | Clave previa inutilizable; nuevo registro |
| Quitar/reiniciar bloqueo | Clave previa inutilizable, sin alternativas software/credenciales |
| Declaración/documento alterados | Host detiene antes del diálogo o servidor rechaza |
| StrongBox ausente o agotado | Fallo explícito; no crear TEE para ese desafío |
| Muerte del proceso y reapertura | Recuperar alias vinculado a cuenta/suite; no reanudar firma automática |

Registre modelo, SO/parche, firmante de aplicación, suite, política/raíces del servidor, escenario, resultado y códigos redactados en un informe revisado. No se incluye informe físico hasta ejecutar estos escenarios. No hay capturas: el único diálogo pertenece al SO y se valida con esta matriz.
