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
    }
}

rootProject.name = "Jarvis"

// All 10 modules defining the JARVIS architecture
include(":app")
include(":core")
include(":voice")
include(":orchestrator")
include(":ai")
include(":tools")
include(":android-integration")
include(":accessibility")
include(":memory")
include(":security")
