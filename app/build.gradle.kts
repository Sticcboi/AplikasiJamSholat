import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.andre.jamsholat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.andre.jamsholat"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Google Play Services Location (for FusedLocationProviderClient)
    implementation ("com.google.android.gms:play-services-location:21.0.1") // Or the latest version
    // Networking
    implementation ("com.squareup.okhttp3:okhttp:4.9.1") // Or the latest version
    implementation ("com.google.android.material:material:1.10.0")
// Coroutines for asynchronous operations
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0") // Or the latest version
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0") // Or the latest version
    val appcompatVersion = "1.7.1" // You can use the latest stable version

    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    // For loading and tinting drawables on older versions of the platform
    implementation("androidx.appcompat:appcompat-resources:$appcompatVersion")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}