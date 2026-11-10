package com.toperras.mandotv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.toperras.mandotv.R
import com.toperras.mandotv.core.RemoteKey
import com.toperras.mandotv.ui.theme.LocalBrandColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/*
 * Regla de oro del mando: cada gesto envía UNA orden.
 * - Toque normal → clickable: se dispara una vez, al soltar.
 * - Mantener pulsado → solo el volumen repite (holdToRepeat); el resto ignora
 *   la pulsación larga, así nunca se suma una orden extra.
 * - Dos toques rápidos → dos órdenes, como espera el usuario.
 */

private const val LONG_PRESS_MS = 400L
private const val REPEAT_INTERVAL_MS = 180L

/**
 * Toque corto = una orden al soltar. Mantener = repetir cada 180 ms hasta soltar.
 * La repetición se corta si la app deja de estar en primer plano.
 */
@Composable
private fun Modifier.holdToRepeat(
    enabled: Boolean,
    label: String,
    onPressedChange: (Boolean) -> Unit,
    onAction: () -> Unit,
): Modifier {
    val action by rememberUpdatedState(onAction)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return this
        .semantics {
            role = Role.Button
            contentDescription = label
            if (!enabled) disabled()
            // TalkBack: doble toque = una sola orden
            onClick(label) { if (enabled) action(); enabled }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val pointerScope = this
            coroutineScope {
                val scope = this
                pointerScope.awaitEachGesture {
                    awaitFirstDown()
                    onPressedChange(true)
                    var repeated = false
                    val repeater = scope.launch {
                        delay(LONG_PRESS_MS)
                        while (isActive && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            action()
                            repeated = true
                            delay(REPEAT_INTERVAL_MS)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    repeater.cancel()
                    onPressedChange(false)
                    // Toque corto: una sola orden al soltar (si no se canceló el gesto).
                    if (!repeated && up != null) action()
                }
            }
        }
}

/** Botón redondo del mando. */
@Composable
fun RemoteButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    size: Dp = 64.dp,
) {
    val haptics = rememberHaptics()
    val brand = LocalBrandColors.current
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(if (danger) colors.errorContainer else brand.surfaceRaised)
            .border(0.5.dp, colors.outlineVariant, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) {
                haptics.tap()
                onClick()
            }
            .semantics { contentDescription = label },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) colors.error else colors.onSurface,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

/** Cruceta con OK central. */
@Composable
fun DPad(onKey: (RemoteKey) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val brand = LocalBrandColors.current
    val colors = MaterialTheme.colorScheme
    val dpadSize = 236.dp
    val arrowSize = 72.dp

    @Composable
    fun Arrow(key: RemoteKey, icon: ImageVector, label: String, alignment: Alignment) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(4.dp)
                .size(arrowSize)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) {
                    haptics.tap()
                    onKey(key)
                }
                .semantics { contentDescription = label },
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(34.dp))
        }
    }

    Box(
        modifier = modifier
            .size(dpadSize)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(brand.surfaceRaised)
            .border(0.5.dp, colors.outlineVariant, CircleShape),
    ) {
        Box(Modifier.align(Alignment.TopCenter)) {
            Arrow(RemoteKey.UP, Icons.Filled.KeyboardArrowUp, stringResource(R.string.key_up), Alignment.TopCenter)
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            Arrow(RemoteKey.DOWN, Icons.Filled.KeyboardArrowDown, stringResource(R.string.key_down), Alignment.BottomCenter)
        }
        Box(Modifier.align(Alignment.CenterStart)) {
            Arrow(RemoteKey.LEFT, Icons.Filled.KeyboardArrowLeft, stringResource(R.string.key_left), Alignment.CenterStart)
        }
        Box(Modifier.align(Alignment.CenterEnd)) {
            Arrow(RemoteKey.RIGHT, Icons.Filled.KeyboardArrowRight, stringResource(R.string.key_right), Alignment.CenterEnd)
        }
        val okLabel = stringResource(R.string.key_ok)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .size(92.dp)
                .clip(CircleShape)
                .background(brand.gradient)
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = okLabel) {
                    haptics.strong()
                    onKey(RemoteKey.OK)
                }
                .semantics { contentDescription = okLabel },
        ) {
            Text("OK", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Mitad de un balancín (+ o −). */
@Composable
private fun RockerHalf(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    repeatOnHold: Boolean,
    onAction: () -> Unit,
) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.colorScheme
    var pressed by remember { mutableStateOf(false) }
    val base = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(if (pressed) colors.primaryContainer else Color.Transparent)
    val interactive = if (repeatOnHold) {
        base.holdToRepeat(
            enabled = enabled,
            label = label,
            onPressedChange = { pressed = it },
            onAction = { haptics.tap(); onAction() },
        )
    } else {
        base
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) {
                haptics.tap()
                onAction()
            }
            .semantics { contentDescription = label }
    }
    Box(contentAlignment = Alignment.Center, modifier = interactive) {
        Icon(icon, contentDescription = null, tint = if (pressed) colors.primary else colors.onSurface)
    }
}

/** Balancín vertical (+ / −) para volumen o canales. */
@Composable
fun Rocker(
    title: String,
    upLabel: String,
    downLabel: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    enabled: Boolean,
    repeatOnHold: Boolean = false,
) {
    val brand = LocalBrandColors.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(38.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(76.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(shape)
            .background(brand.surfaceRaised)
            .border(0.5.dp, colors.outlineVariant, shape)
            .padding(vertical = 4.dp),
    ) {
        RockerHalf(Icons.Filled.Add, upLabel, enabled, repeatOnHold, onUp)
        Text(title, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        RockerHalf(Icons.Filled.Remove, downLabel, enabled, repeatOnHold, onDown)
    }
}

/**
 * Mando completo. Los botones que la tele no admite (supportedKeys), o todos si
 * enabled es false, aparecen atenuados y desactivados.
 */
@Composable
fun RemotePanel(
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    supportedKeys: Set<RemoteKey>? = null,
    enabled: Boolean = true,
) {
    fun on(key: RemoteKey) = enabled && (supportedKeys?.contains(key) ?: true)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            RemoteButton(Icons.Filled.Menu, stringResource(R.string.key_menu), { onKey(RemoteKey.MENU) }, enabled = on(RemoteKey.MENU))
            RemoteButton(
                Icons.Filled.PowerSettingsNew,
                stringResource(R.string.key_power),
                { onKey(RemoteKey.POWER) },
                enabled = on(RemoteKey.POWER),
                danger = true,
            )
        }

        DPad(onKey = onKey, enabled = on(RemoteKey.OK), modifier = Modifier.align(Alignment.CenterHorizontally))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RemoteButton(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.key_back), { onKey(RemoteKey.BACK) }, enabled = on(RemoteKey.BACK))
            RemoteButton(Icons.Filled.Home, stringResource(R.string.key_home), { onKey(RemoteKey.HOME) }, enabled = on(RemoteKey.HOME))
            RemoteButton(Icons.AutoMirrored.Filled.VolumeOff, stringResource(R.string.key_mute), { onKey(RemoteKey.MUTE) }, enabled = on(RemoteKey.MUTE))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Rocker(
                title = stringResource(R.string.remote_volume),
                upLabel = stringResource(R.string.key_volume_up),
                downLabel = stringResource(R.string.key_volume_down),
                onUp = { onKey(RemoteKey.VOLUME_UP) },
                onDown = { onKey(RemoteKey.VOLUME_DOWN) },
                enabled = on(RemoteKey.VOLUME_UP),
                repeatOnHold = true,
            )
            Rocker(
                title = stringResource(R.string.remote_channel),
                upLabel = stringResource(R.string.key_channel_up),
                downLabel = stringResource(R.string.key_channel_down),
                onUp = { onKey(RemoteKey.CHANNEL_UP) },
                onDown = { onKey(RemoteKey.CHANNEL_DOWN) },
                enabled = on(RemoteKey.CHANNEL_UP),
            )
        }
    }
}

/** Nombre accesible de cada tecla (para mostrar «Última tecla: …»). */
@Composable
fun RemoteKey.label(): String = stringResource(
    when (this) {
        RemoteKey.POWER -> R.string.key_power
        RemoteKey.UP -> R.string.key_up
        RemoteKey.DOWN -> R.string.key_down
        RemoteKey.LEFT -> R.string.key_left
        RemoteKey.RIGHT -> R.string.key_right
        RemoteKey.OK -> R.string.key_ok
        RemoteKey.BACK -> R.string.key_back
        RemoteKey.HOME -> R.string.key_home
        RemoteKey.MENU -> R.string.key_menu
        RemoteKey.MUTE -> R.string.key_mute
        RemoteKey.VOLUME_UP -> R.string.key_volume_up
        RemoteKey.VOLUME_DOWN -> R.string.key_volume_down
        RemoteKey.CHANNEL_UP -> R.string.key_channel_up
        RemoteKey.CHANNEL_DOWN -> R.string.key_channel_down
    },
)
