package com.toperras.mandotv.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** true si el usuario tiene activada la vibración en Ajustes. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** Vibración corta al pulsar, solo si está activada en Ajustes. */
class Haptics(private val view: View, private val enabled: Boolean) {
    fun tap() {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun strong() {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { Haptics(view, enabled) }
}
