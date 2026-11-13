# Installation — Development

No stable or other package publication has been confirmed. No Maven coordinates are advertised. Use this repository as source, retaining both modules:

```kotlin
// settings.gradle.kts in the host; copy/checkout this repository to vendor/sworn-ink
include(":protocol", ":signature")
project(":protocol").projectDir = file("vendor/sworn-ink/protocol")
project(":signature").projectDir = file("vendor/sworn-ink/signature")
```

The signature module uses `api(project(":protocol"))`. If renaming either module to avoid host conflicts, adjust this project reference too. The complete example uses the original names:

```kotlin
dependencies { implementation(project(":signature")) }
```

Requirements: Android 12/API 31+, enrolled strong biometric, hardware attestation support and a secure lock screen. Source build pins JDK 17, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, SDK 35. The hardware checks reject software keys. These pins are a reproducible development toolchain, not a claim of testing every Android device.

See [quickstart](docs/agents/quickstart.md) for complete host setup and [example source](examples/integration/src/main/kotlin/io/apexfission/signature/example/SigningIntegration.kt).
