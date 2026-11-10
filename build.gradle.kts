// Archivo raíz: solo declara los plugins; cada módulo aplica los suyos.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
