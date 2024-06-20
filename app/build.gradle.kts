import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreDir = "$rootDir/keystore"

val keystoreProps = Properties()
for (name in arrayOf("r0s.properties", "debug.properties")) {
    val f = file("$keystoreDir/$name")
    if (!f.exists()) continue
    keystoreProps.load(f.inputStream())
    break
}

val dhizukuApiVersion = "2.5.2"

android {
    namespace = "com.rosan.dhizuku.api.xposed"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "1.6 (api_$dhizukuApiVersion)"
    }

    signingConfigs {
        val keyAlias = keystoreProps.getProperty("keyAlias")
        val keyPassword = keystoreProps.getProperty("keyPassword")
        val storeFile = file("$keystoreDir/${keystoreProps.getProperty("storeFile")}")
        val storePassword = keystoreProps.getProperty("storePassword")
        getByName("debug") {
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
            this.storeFile = storeFile
            this.storePassword = storePassword
            enableV1Signing = true
            enableV2Signing = true
        }

        create("release") {
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
            this.storeFile = storeFile
            this.storePassword = storePassword
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(JavaVersion.VERSION_17.majorVersion.toInt())
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.8.0")
    compileOnly("de.robv.android.xposed:api:82")

    compileOnly(project(":hidden-api"))

    implementation("io.github.iamr0s:Dhizuku-API:$dhizukuApiVersion")
}