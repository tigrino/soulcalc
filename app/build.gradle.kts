/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Release signing credentials are shared across projects. They live in
// ~/.gradle/gradle.properties, which Gradle merges into every build, and can be
// overridden by environment variables for CI. The keystore itself is never kept
// in the repository.
val releaseKeystore: File? =
    (providers.gradleProperty("tigrKeystoreFile").orNull ?: System.getenv("ANDROID_KEYSTORE_FILE"))
        ?.let(::File)
        ?.takeIf { it.exists() }

android {
    namespace = "net.tigr.soulcalc"
    compileSdk = 34

    signingConfigs {
        create("release") {
            releaseKeystore?.let { keystore ->
                storeFile = keystore
                storePassword = providers.gradleProperty("tigrKeystorePassword").orNull
                    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = "soulcalc"
                keyPassword = providers.gradleProperty("tigrKeyPassword").orNull
                    ?: System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "net.tigr.soulcalc"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "SoulCalc-${variant.versionName}-${variant.buildType.name}.apk"
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
