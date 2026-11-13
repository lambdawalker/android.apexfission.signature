plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "io.apexfission.signature"
    compileSdk = 35
    defaultConfig { minSdk = 31; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = false }
    lint {
        warningsAsErrors = true
        abortOnError = true
        // Keep tested compatibility pins deliberate. Newly available versions are advisory, not correctness failures.
        informational += "GradleDependency"
    }
}
dependencies {
    api(project(":protocol"))
    api("androidx.biometric:biometric:1.1.0")
    api("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.7")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
}
dependencies { testImplementation(kotlin("test-junit")) }
