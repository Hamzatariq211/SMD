pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // CRITICAL: Add Agora Maven repository for SDK access
        maven {
            url = uri("https://download.agora.io/android/release")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "i210396_i211384"
include(":app")
