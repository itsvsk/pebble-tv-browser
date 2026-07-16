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

rootProject.name = "TvBrowser"

include(":app")
include(":core:common")
include(":core:design")
include(":core:db")
include(":core:webview")
include(":core:adblock")
include(":core:cursor")
include(":core:downloads")
include(":feature:home")
include(":feature:browser")
include(":feature:tabs")
include(":feature:bookmarks")
include(":feature:history")
include(":feature:downloads")
include(":feature:settings")
