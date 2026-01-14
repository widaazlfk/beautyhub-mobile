// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()  // ← THIS IS CRITICAL for Firebase dependencies
        mavenCentral()
        // This is correctly added for MPAndroidChart
        maven { url = uri("https://jitpack.io") }

    }
}

rootProject.name = "BeautyHub"
include(":app")