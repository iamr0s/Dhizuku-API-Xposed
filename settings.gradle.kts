pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Xposed Framework API"
            url = uri("https://api.xposed.info")
        }
    }
}

rootProject.name = "Dhizuku-API-Xposed"
include("app", "hidden-api")
