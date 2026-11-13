# Instalación — Desarrollo

No hay paquetes publicados confirmados ni historial de versiones. Clone el repositorio y conserve juntos `protocol` (Kotlin/JVM) y `signature` (Android). Use JDK 17, SDK Android 35 y el wrapper incluido. El proyecto fija Gradle 8.11.1, AGP 8.9.2 y Kotlin 2.1.20. Los repositorios de plugins son Google Maven, Maven Central y Gradle Plugin Portal. Requisitos: Android API 31, bloqueo seguro, biometría fuerte registrada y Android Keystore respaldado por hardware.

En un host con los módulos fuente:

```kotlin
// settings.gradle.kts
include(":protocol", ":signature")
project(":protocol").projectDir = file("vendor/sworn-ink/protocol")
project(":signature").projectDir = file("vendor/sworn-ink/signature")
// host build.gradle.kts
// dependencies { implementation(project(":signature")) }
```

El manifiesto de signature incorpora `android.permission.USE_BIOMETRIC`; no requiere solicitar un permiso en ejecución. El host gestiona Internet, TLS, FragmentActivity, su proveedor JWT y cliente HTTP. `protocol` se exporta transitivamente. No se anuncian coordenadas Maven o JitPack; `0.0.0-SNAPSHOT` es una identidad de compilación de desarrollo. Si cambia los nombres de los módulos, ajuste `api(project(":protocol"))`.
