// Gradle settings for CloudStream extension repository
pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("file://${settings.rootDir.parentFile}/maven-local") }
    }
}

rootProject.name = "rdnutz-cloudstream-extensions"
include(":crazyshit")
include(":kaotic")
include(":worldstar")
include(":xrares")
include(":livegore")
include(":daftporn")
include(":efukt")
include(":heavyfetish")
include(":nothingtoxic")
include(":theync")
include(":usacrime")