plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.edgeplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.edgeplus"
        minSdk = 28
        targetSdk = 35
        versionCode = 20
        versionName = "2.5.3"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "edgeplus"
            keyAlias = "edgeplus"
            keyPassword = "edgeplus"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    // zero external deps, 100% native Android SDK
}
