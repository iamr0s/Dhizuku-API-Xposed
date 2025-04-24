import org.jetbrains.kotlin.konan.properties.loadProperties
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
}

val keystoreDir = "$rootDir/keystore"

val keystoreProps = Properties()
for (name in arrayOf("r0s.properties", "debug.properties")) {
    val f = file("$keystoreDir/$name")
    if (!f.exists()) continue
    keystoreProps.load(f.inputStream())
    break
}

android {
    namespace = "com.rosan.dhizuku.api.xposed"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = compileSdk

        val versionProps = loadProperties("$rootDir/version.properties")
        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName") + " (API: " + libs.versions.dhizuku.get() + ")"

        multiDexEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
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
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_21
        sourceCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }
}

dependencies {
    compileOnly(libs.annotation)
    compileOnly(project(":hidden-api"))

    compileOnly(libs.xposed.api)
    implementation(libs.dhizuku)
}
