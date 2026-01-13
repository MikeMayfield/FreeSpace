plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp) // Apply the KSP plugin replacement for KAPT
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.tmf.freespace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tmf.freespace"
        minSdk = 29
        targetSdk = 35
        versionCode = 10015
        versionName = "1.0.15"

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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.checker.qual)
    implementation(libs.coil.compose)
    implementation(libs.commons.net)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.android) // If using Android
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logging.interceptor) // Optional: For logging requests/responses
    implementation(libs.okhttp)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.retrofit) // Or latest
    implementation(libs.retrofit2.kotlinx.serialization.converter) // Converter for Retrofit
    implementation(libs.room.runtime)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.billing.ktx)
    implementation(libs.google.firebase.auth)

    ksp(libs.room.compiler)

    //FireBase (https://console.firebase.google.com/)
    implementation(platform(libs.firebase.bom))  //Import the Firebase BoM (Bill of Materials). This will manage the versions of all Firebase libraries
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))


    //region Tests
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
    //endregion
}
