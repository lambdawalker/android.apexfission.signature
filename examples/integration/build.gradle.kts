plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "io.apexfission.signature.example"
    compileSdk = 35
    defaultConfig { minSdk = 31 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation(project(":signature")) }
