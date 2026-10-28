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
include(":livegore")
include(":daftporn")
include(":efukt")
include(":theync")
include(":fullfightreplays")
include(":mmacore")
include(":heavyr")
include(":honeydippedcream")
include(":horriblevideos")
include(":inhumanity")
include(":jeffsmodels")
include(":nothingtoxic")
include(":plumperd")
include(":sickjunk")
include(":sexyandfunny")
include(":youporn")
include(":streamedlive")
include(":watchmmafull")
include(":zfilmzoriginals")
include(":sportsreplays")
// rdnutz-iptv and xrares removed — not published (no .cs3 on builds branch)