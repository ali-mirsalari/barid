// Barid: forwards Iranian bank transaction SMS to a server you pair with (Darakav built in).
pluginManagement {
    repositories {
        // Where Google's Maven is unreachable (e.g. from Iran), set googleMavenMirror in
        // ~/.gradle/gradle.properties, for example https://maven.myket.ir (see README.md).
        providers.gradleProperty("googleMavenMirror").orNull?.let { maven(it) }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        providers.gradleProperty("googleMavenMirror").orNull?.let { maven(it) }
        google()
        mavenCentral()
    }
}
rootProject.name = "barid"
include(":app")
