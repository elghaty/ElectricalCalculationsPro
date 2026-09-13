import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(17)
}

val ciSigningFile = rootProject.file("ci-signing.properties")

val ciSigningProperties = Properties().apply {
    if (ciSigningFile.isFile) {
        ciSigningFile.inputStream().use {
            load(it)
        }
    }
}

fun decodeSigningProperty(name: String): String? {
    val value = ciSigningProperties.getProperty(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
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

val ciStoreFile =
    ciSigningProperties
        .getProperty("storeFile")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val ciStorePassword =
    decodeSigningProperty("storePasswordB64")

val ciKeyAlias =
    decodeSigningProperty("keyAliasB64")

val ciKeyPassword =
    decodeSigningProperty("keyPasswordB64")

val hasCiSigning =
    ciSigningFile.isFile &&
        !ciStoreFile.isNullOrBlank() &&
        !ciStorePassword.isNullOrBlank() &&
        !ciKeyAlias.isNullOrBlank() &&
        !ciKeyPassword.isNullOrBlank() &&
        rootProject.file(ciStoreFile!!).isFile

android {
    namespace = "com.electrical.calculationspro"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.electrical.calculationspro"

        minSdk = 24

        targetSdk = 35

        versionCode =
            project.findProperty("versionCode")
                ?.toString()
                ?.toIntOrNull()
                ?: System.getenv("VERSION_CODE")
                    ?.toIntOrNull()
                ?: 1

        versionName =
            project.findProperty("versionName")
                ?.toString()
                ?: System.getenv("VERSION_NAME")
                ?: "1.0"
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

    if (hasCiSigning) {
        signingConfigs {
            create("releaseCi") {
                storeFile =
                    rootProject.file(
                        ciStoreFile!!
                    )

                storePassword =
                    ciStorePassword

                keyAlias =
                    ciKeyAlias

                keyPassword =
                    ciKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false

            if (hasCiSigning) {
                signingConfig =
                    signingConfigs.getByName(
                        "releaseCi"
                    )
            }
        }

        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {

    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    implementation(
        "androidx.activity:activity-compose:1.10.0"
    )

    implementation(
        "androidx.compose.ui:ui:1.7.6"
    )

    implementation(
        "androidx.compose.material3:material3:1.3.1"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview:1.7.6"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling:1.7.6"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    implementation(
        "androidx.room:room-runtime:2.6.1"
    )

    implementation(
        "androidx.room:room-ktx:2.6.1"
    )

    ksp(
        "androidx.room:room-compiler:2.6.1"
    )
}
