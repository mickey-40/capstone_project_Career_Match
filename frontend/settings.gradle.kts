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

// IMPORTANT: no versionCatalogs{} block here.
// Gradle will automatically use gradle/libs.versions.toml as `libs`.

rootProject.name = "CareerMatchAI"
include(":app")
