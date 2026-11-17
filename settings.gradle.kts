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
    }
}
rootProject.name = "studylock"

include(":core:common")
include(":core:domain")
include(":core:academic-engine")
include(":core:focus")
include(":data:local")
include(":app")
