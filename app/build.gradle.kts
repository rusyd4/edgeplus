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
        versionCode = 4
        versionName = "1.2.1"
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
    implementation(fileTree("libs") { include("*.aar", "*.jar") })
}
