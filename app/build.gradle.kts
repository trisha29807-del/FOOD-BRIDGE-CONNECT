// ============================================================
// app/build.gradle.kts
// Replace your existing app/build.gradle.kts with this file
// ============================================================

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")   // Firebase plugin
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.foodbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.foodbridge"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}


dependencies {
    // Existing UI dependencies
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.18.0")

    // Firebase BoM — manages all Firebase library versions automatically
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))

    // Firebase services (no version numbers needed when using BoM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Coroutines for clean async code
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}
