// Top-level Gradle settings for Promptr.
// – Repositories for plugins & dependencies
// – Single version-catalog pointing to gradle/libs.versions.toml

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
        google()
        mavenCentral()
    }
}

rootProject.name = "Promptr"
include(":app")
