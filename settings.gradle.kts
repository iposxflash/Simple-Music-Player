pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Menggunakan sintaks uri() yang direkomendasikan untuk Kotlin DSL
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "Simple-Music-Player"
include(":app")
