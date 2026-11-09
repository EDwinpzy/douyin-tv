import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseSigning = signingProperties()

android {
    namespace = "com.dongpiandi.douyintv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dongpiandi.douyintv"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigning != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(releaseSigning.storeFile)
                    storePassword = releaseSigning.storePassword
                    keyAlias = releaseSigning.keyAlias
                    keyPassword = releaseSigning.keyPassword
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

data class ReleaseSigning(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun signingProperties(): ReleaseSigning? {
    val properties = Properties()
    val envPath = System.getenv("DOUYINTV_KEYSTORE_PROPERTIES")
    val sources = listOfNotNull(
        envPath?.let { File(it) },
        rootProject.file("keystore.properties"),
        rootProject.file("local.properties"),
    )
    sources
        .firstOrNull { it.isFile }
        ?.inputStream()
        ?.use { properties.load(it) }
    val storeFile = properties.getProperty("storeFile") ?: return null
    val storePassword = properties.getProperty("storePassword") ?: return null
    val keyAlias = properties.getProperty("keyAlias") ?: return null
    val keyPassword = properties.getProperty("keyPassword") ?: storePassword
    if (!file(storeFile).isFile) return null
    return ReleaseSigning(
        storeFile = storeFile,
        storePassword = storePassword,
        keyAlias = keyAlias,
        keyPassword = keyPassword,
    )
}
