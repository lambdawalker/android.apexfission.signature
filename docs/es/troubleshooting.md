# Problemas — Desarrollo

| Fallo | Acción |
| --- | --- |
| Capacidades vacías | Configure bloqueo seguro y biometría fuerte; reconocimiento facial débil puede no servir |
| StrongBox no disponible/sin recursos | Muestre error y solicite otra decisión al servidor; no reintente el mismo desafío con TEE |
| Política de hardware/autenticación incorrecta | Rechace la clave recién creada y examine dispositivo/política |
| `InvalidatedKeyException` | Confirme recuperación y solicite nuevo registro/nonce |
| Cancelación de usuario | Conserve clave y detenga solicitud; permita nuevo intento explícito |
| Bloqueo biométrico | Muestre código conservado y espere recuperación del SO; no use credenciales alternativas |
| `CancellationException` de ciclo de vida | Use la actividad actual, desafío válido e intento explícito nuevo |
| Declaración distinta | Detenga antes del diálogo; compruebe issuer/subject exactos, documento, nonce, suite y keyId |
| Atestación rechazada | Revise raíces, revocación, paquete/firmante y arranque con operadores; no debilite política |
| Desafío caducado | Obtenga otro; registro exige nueva clave atestada para nuevo nonce |

No registre tokens JWT, contenido del documento, información biométrica u objetos privados. Certificados, identificadores y fingerprints también identifican usuarios; aplique la retención del host. La biblioteca no instala logger.
