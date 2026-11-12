plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystore = System.getenv("FOLIO_KEYSTORE")

android {
    namespace = "io.github.miceuz.folio"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.miceuz.folio"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = System.getenv("FOLIO_VERSION") ?: "dev"
    }

    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("FOLIO_KEYSTORE_PASS")
                keyAlias = "folio"
                keyPassword = System.getenv("FOLIO_KEYSTORE_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
