plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rhdevs.shieldx"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rhdevs.shieldx"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.2.0" // Versi naik!
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Ikon tambahan
    
    // TAMBAHKAN INI UNTUK NAVIGASI KOMPLEKS
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    compileOnly("de.robv.android.xposed:api:82")
}
