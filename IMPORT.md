# Installation — Development

There is no confirmed published package and no release archive yet. Clone the repository as source; keep `protocol` (pure Kotlin/JVM) and `signature` (Android) together. Use JDK 17, Android SDK 35 and the checked-in Gradle wrapper. The root build pins Gradle 8.11.1, AGP 8.9.2 and Kotlin 2.1.20. Plugin repositories require Google Maven, Maven Central and the Gradle Plugin Portal. Minimum runtime: Android API 31, secure lock screen, enrolled strong biometric and a hardware-backed Android Keystore.

In a host checkout with these source modules included:

```kotlin
// settings.gradle.kts
include(":protocol", ":signature")
project(":protocol").projectDir = file("vendor/sworn-ink/protocol")
project(":signature").projectDir = file("vendor/sworn-ink/signature")
// host build.gradle.kts
// dependencies { implementation(project(":signature")) }
```

The signature manifest merges `android.permission.USE_BIOMETRIC`. No runtime permission request is needed for that permission. This library does not add Internet permission; the host's network layer owns it and TLS configuration. The host needs FragmentActivity and must integrate its own JWT provider and HTTP client.

`protocol` is exported transitively by `signature`. No Maven, JitPack, repository credentials, release tag or publication command is required. `0.0.0-SNAPSHOT` is an internal development build identity only. Renaming modules requires adjusting the signature module's `api(project(":protocol"))` reference.
