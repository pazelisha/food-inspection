plugins {
    alias(libs.plugins.android.application)
    // Add Firebase plugins
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.foodgradeinspection"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.foodgradeinspection"
        minSdk = 27
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Base Android dependencies
    implementation(libs.appcompat)
    implementation(libs.material)

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore:25.0.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")

    implementation(libs.firebase.auth)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}