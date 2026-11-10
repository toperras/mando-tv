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

rootProject.name = "MandoTV"

// app:  pantallas (Jetpack Compose) y todo lo específico de Android.
// core: modelo del mando y protocolos en Kotlin puro, sin dependencias de Android,
//       para poder compartirlo con iOS más adelante (Kotlin Multiplatform).
include(":app", ":core")
