# Limitaciones — Desarrollo

API 31+ permite comprobar nivel exacto de KeyInfo y política de autenticación. Las capacidades no garantizan recursos, aprovisionamiento remoto, raíces confiables ni aceptación del servidor. Un dispositivo puede anunciar StrongBox y fallar al crear o atestar. Se rechazan claves software y credenciales del dispositivo.

El servidor valida cadena completa, raíces configuradas por operadores y revocación reciente. Exportar una cadena no prueba confianza ni identidad de la aplicación. La invalidación por nueva inscripción se solicita y comprueba localmente; la atestación no demuestra explícitamente esa bandera. No se afirma identidad biométrica, validez legal, sellado temporal independiente, emisión JWT, verificación de correo, defensa remota contra repetición ni retención de pruebas.

No se expone firma de resúmenes: enviar un hash de 32 bytes a SHA256withECDSA volvería a calcular su resumen y no firmaría la declaración Sworn Ink. La API de producción solo acepta declaraciones tipadas.

No hay composables o pantallas propias. Capturas y previews no prueban cumplimiento biométrico. Use pruebas en dispositivos físicos y la matriz manual. Pasar pruebas JVM o compilar no implica validar dispositivos, servidor real ni AWS.
