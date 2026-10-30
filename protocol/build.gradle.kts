plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies { testImplementation(kotlin("test-junit")); testImplementation("org.json:json:20240303") }
tasks.test { systemProperty("vectorPath", file("test-vectors/v1.json").absolutePath) }
