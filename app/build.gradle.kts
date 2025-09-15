plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp) // Apply the KSP plugin replacement for KAPT
}

android {
    namespace = "com.tmf.freespace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tmf.freespace"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    ksp {
        arg("room.schemaLocation", "${projectDir}/schemas")
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android) // If using Android
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.commons.net)
    implementation(libs.retrofit) // Or latest
    implementation(libs.kotlinx.serialization.json) // Or latest
    implementation(libs.retrofit2.kotlinx.serialization.converter) // Converter for Retrofit
    implementation(libs.kotlinx.serialization.json) // Or latest
    implementation(libs.retrofit2.kotlinx.serialization.converter) // Converter for Retrofit
    implementation(libs.logging.interceptor) // Optional: For logging requests/responses
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.0.0")


//    implementation(libs.firebase.crashlytics.buildtools)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.powermock.module.junit4)
    testImplementation(libs.powermock.api.mockito2)
    testImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.rules) // Added for GrantPermissionRule
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.kotlinx.coroutines.test)
}
