plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.speakerapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.speakerapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Matches Kotlin 2.0.21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Compose BOM — Controls all Compose versions
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))

    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // THIS IS THE CRITICAL FIX: The dependency for the extended icons
    implementation("androidx.compose.material:material-icons-extended")

    // Retrofit for HTTP calls to your FastAPI backend
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
}