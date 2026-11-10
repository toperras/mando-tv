// Módulo con la lógica del mando y los protocolos.
// Regla: aquí NO se importa nada de android.*; solo Kotlin y java.net/javax.net.
// Lo que dependa de Android (almacén de claves, mDNS, infrarrojos) se define como
// interfaz aquí y se implementa en :app. Así se podrá migrar a Kotlin Multiplatform.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.toperras.mandotv.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
