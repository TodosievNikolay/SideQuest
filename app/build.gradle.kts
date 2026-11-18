plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Keep a fresh checkout runnable before a Firebase Android app is registered.
if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

val releaseStoreFile = providers.environmentVariable("SIDEQUEST_SIGNING_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("SIDEQUEST_SIGNING_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("SIDEQUEST_SIGNING_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("SIDEQUEST_SIGNING_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
require(releaseSigningValues.all { it == null } || releaseSigningValues.all { !it.isNullOrBlank() }) {
    "Provide all four SIDEQUEST_SIGNING_* environment variables to sign a release."
}

android {
    namespace = "com.example.sidequest"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sidequest"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0-test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("privateRelease") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("privateRelease")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
}
