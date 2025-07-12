// Module-level Gradle in Kotlin DSL, using the libs catalog.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mercymayagames.promptr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mercymayagames.promptr"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Lock orientation for all activities
        manifestPlaceholders["screenOrientation"] = "landscape"
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Core UI + Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Coroutines for background file parsing
    implementation(libs.kotlinx.coroutines.android)

    // Text-extraction libs
    implementation(libs.itextpdf)
        // DOCX Text Extraction (Apache POI full jars)
        // Strip Log4J so we avoid the MethodHandle API 26+ crash
        implementation(libs.poi) {
                exclude(group = "org.apache.logging.log4j")
            }
        implementation(libs.poiOoxml) {
                exclude(group = "org.apache.logging.log4j")
            }

    // Provide a no-op SLF4J binding so POI’s logging has somewhere to go
    implementation(libs.slf4j.nop)

    // Logging
    implementation(libs.timber)
}
