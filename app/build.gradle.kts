import java.util.Base64
import java.util.Properties

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

val signingPropertiesFile =
    rootProject.file("ci-signing.properties")

val signingProperties = Properties()

if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use {
        signingProperties.load(it)
    }
}

fun decodeProperty(name: String): String? {
    val value = signingProperties.getProperty(name)
        ?: return null

    return try {
        String(
            Base64.getDecoder().decode(value),
            Charsets.UTF_8
        )
    } catch (_: Exception) {
        null
    }
}

val ciStorePassword =
    decodeProperty("storePasswordB64")

val ciKeyAlias =
    decodeProperty("keyAliasB64")

val ciKeyPassword =
    decodeProperty("keyPasswordB64")

val ciKeystorePath =
    signingProperties.getProperty("storeFile")

val ciKeystoreFile =
    if (!ciKeystorePath.isNullOrBlank()) {
        rootProject.file(ciKeystorePath)
    } else {
        rootProject.file("release.keystore")
    }

val hasCiSigning =
    !ciStorePassword.isNullOrEmpty() &&
    !ciKeyAlias.isNullOrEmpty() &&
    !ciKeyPassword.isNullOrEmpty() &&
    ciKeystoreFile.exists()

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

    signingConfigs {

        create("ciRelease") {

            check(hasCiSigning) {
                """
                CI RELEASE SIGNING IS NOT CONFIGURED.

                Required:
                - release.keystore
                - store password
                - key alias
                - key password
                """.trimIndent()
            }

            storeFile =
                ciKeystoreFile

            storePassword =
                ciStorePassword!!

            keyAlias =
                ciKeyAlias!!

            keyPassword =
                ciKeyPassword!!
        }
    }

    buildTypes {

        debug {

            isMinifyEnabled = false
        }

        release {

            isMinifyEnabled = false

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
