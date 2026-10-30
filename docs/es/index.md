# Sworn Ink Android — Desarrollo

No existe una versión estable publicada y confirmada. Esta guía describe el código fuente. Android firma declaraciones vinculadas al servidor; el servicio Go verifica la atestación, evita repeticiones, valida JWT y conserva pruebas y recibos. El host obtiene los tokens, verifica el correo y presenta el documento para su aprobación.

| Tarea | Guía |
| --- | --- |
| Instalar y conectar el host | [Inicio rápido](quickstart.md) |
| Entender la política de hardware | [Conceptos](concepts.md) |
| Utilizar las declaraciones Kotlin | [API](api.md) |
| Registrar, firmar, cancelar y recuperar | [Recetas](recipes.md) |
| Diagnosticar o actualizar | [Problemas](troubleshooting.md), [Migración](migration.md) |
| Validar en hardware real | [Pruebas](testing.md) |
| Conocer los límites de confianza | [Limitaciones](limitations.md) |

Invariantes: respetar la suite seleccionada por el servidor; usar un nonce nuevo de 32 bytes para registrar; firmar `Statement.encode()` mediante SHA256withECDSA; codificar binarios HTTP con base64 estándar y relleno; vincular la cuenta a issuer/subject, nunca al correo. Conservar los bytes exactos de la declaración y la firma ECDSA DER. Un resumen del documento no es una declaración del protocolo.

[Instalación](installation.md) · [Protocolo autoritativo de Go](https://github.com/lambdawalker/go.apexfission.aws.signature/blob/feat/sworn-ink-signatures/docs/agents/protocol.md)
