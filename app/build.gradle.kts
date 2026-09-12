plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val ciVersionName = System.getenv("VERSION_NAME") ?: "1.0.0"

val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
val keyAliasValue = System.getenv("KEY_ALIAS")
val keyPasswordValue = System.getenv("KEY_PASSWORD")

val hasReleaseSigning = listOf(
    keystoreBase64,
    keystorePassword,
    keyAliasValue,
    keyPasswordValue
).all { !it.isNullOrBlank() }

if (hasReleaseSigning) {
    val keystoreFile = rootProject.file("release.keystore")

    if (!keystoreFile.exists()) {
        keystoreFile.writeBytes(
            java.util.Base64
                .getDecoder()
                .decode(keystoreBase64)
        )
    }

    android.signingConfigs.create("ciRelease") {
        storeFile = keystoreFile
        storePassword = keystorePassword
        keyAlias = keyAliasValue
        keyPassword = keyPasswordValue
    }
}

android {
    namespace = "com.electrical.calculationspro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.electrical.calculationspro"
        minSdk = 26
        targetSdk = 35

        versionCode = ciVersionCode
        versionName = ciVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("ciRelease")
            }

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
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
    }
}

dependencies {

    val composeBom =
        platform(
            "androidx.compose:compose-bom:2024.10.01"
        )

    implementation(composeBom)

    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"
    )

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation(
        "androidx.activity:activity-compose:1.9.3"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.8.3"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.8.7"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}
