import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val versionCodeValue =
    System.getenv("VERSION_CODE")
        ?.toIntOrNull()
        ?: 1

val versionNameValue =
    System.getenv("VERSION_NAME")
        ?: "1.0.0"

val keystoreBase64 =
    System.getenv("KEYSTORE_BASE64")

val keystorePassword =
    System.getenv("KEYSTORE_PASSWORD")

val keyAlias =
    System.getenv("KEY_ALIAS")

val keyPassword =
    System.getenv("KEY_PASSWORD")

val hasCiSigning =
    !keystoreBase64.isNullOrBlank() &&
    !keystorePassword.isNullOrBlank() &&
    !keyAlias.isNullOrBlank() &&
    !keyPassword.isNullOrBlank()

if (hasCiSigning) {

    val keystoreFile =
        rootProject.file("release.keystore")

    if (!keystoreFile.exists()) {
        keystoreFile.writeBytes(
            Base64
                .getDecoder()
                .decode(keystoreBase64)
        )
    }

    android.signingConfigs.create("ciRelease") {
        storeFile = keystoreFile
        storePassword = keystorePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
    }
}

android {

    namespace = "com.electrical.calculationspro"

    compileSdk = 35

    defaultConfig {

        applicationId =
            "com.electrical.calculationspro"

        minSdk = 26

        targetSdk = 35

        versionCode =
            versionCodeValue

        versionName =
            versionNameValue
    }

    buildTypes {

        debug {
            isMinifyEnabled = false
        }

        release {

            isMinifyEnabled = false

            check(hasCiSigning) {
                """
                RELEASE SIGNING IS NOT CONFIGURED.

                Required GitHub Actions Secrets:

                KEYSTORE_BASE64
                KEYSTORE_PASSWORD
                KEY_ALIAS
                KEY_PASSWORD

                A permanent signing key is required so that
                future APK versions can update the existing
                application installation.
                """.trimIndent()
            }

            signingConfig =
                signingConfigs.getByName(
                    "ciRelease"
                )

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
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

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-graphics"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

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
