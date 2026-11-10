package com.toperras.mandotv.ui.remote

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import com.toperras.mandotv.ui.components.AppCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toperras.mandotv.R
import com.toperras.mandotv.core.ConnectionError
import com.toperras.mandotv.core.ConnectionState
import com.toperras.mandotv.ui.components.CardShape
import com.toperras.mandotv.ui.components.ErrorExplanation
import com.toperras.mandotv.ui.components.PrimaryButton
import com.toperras.mandotv.ui.components.RemotePanel
import com.toperras.mandotv.ui.components.ScreenColumn
import com.toperras.mandotv.ui.components.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(tvId: String, onRepair: (host: String, name: String) -> Unit, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val vm: RemoteViewModel = viewModel(key = "remote-$tvId") { RemoteViewModel(application, tvId) }
    val volumeControl by vm.volumeControl.collectAsStateWithLifecycle()
    val tv by vm.tv.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val failedKey by vm.failedKey.collectAsStateWithLifecycle()
    val reconnecting by vm.reconnecting.collectAsStateWithLifecycle()
    val connectionLost by vm.connectionLost.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var confirmForget by rememberSaveable { mutableStateOf(false) }

    // Conectado solo mientras la pantalla está visible; al volver se reconecta solo.
    LifecycleStartEffect(Unit) {
        vm.resume()
        onStopOrDispose { vm.pause() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tv?.name ?: stringResource(R.string.remote_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (tv != null) {
                        IconButton(onClick = { confirmForget = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.remote_forget))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ScreenColumn(Modifier.padding(bottom = 48.dp)) {
                val current = tv
                if (current != null) {
                    StatusCard(connection, current.host, reconnecting, onRetry = vm::connect)

                    val failed = (connection as? ConnectionState.Failed)?.takeUnless { reconnecting }
                    if (failed != null && connectionLost) {
                        // Estaba conectada y se cortó: no tiene sentido sugerir que no es una Android TV.
                        Text(
                            stringResource(R.string.remote_lost),
                            color = colors.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (failed != null) {
                        ErrorExplanation(failed.error, current.host, failed.detail, knownTv = true)
                        if (failed.error == ConnectionError.NOT_PAIRED) {
                            PrimaryButton(
                                text = stringResource(R.string.remote_repair),
                                icon = Icons.Filled.Link,
                                onClick = { onRepair(current.host, current.name) },
                            )
                        }
                    }

                    failedKey?.let { key ->
                        Text(
                            stringResource(R.string.remote_send_failed, key.label()),
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    RemotePanel(
                        onKey = vm::send,
                        supportedKeys = vm.supportedKeys,
                        enabled = connection == ConnectionState.Connected,
                    )

                    // Los botones de volumen siguen activos (algunos aparatos sí los
                    // reenvían por HDMI-CEC), pero explicamos por qué pueden no hacer nada.
                    if (volumeControl == false && connection == ConnectionState.Connected) {
                        AppCard {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(R.string.remote_volume_note_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    stringResource(R.string.remote_volume_note),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text(stringResource(R.string.remote_forget)) },
            text = { Text(stringResource(R.string.remote_forget_confirm, tv?.name ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmForget = false
                    vm.forget(onBack)
                }) { Text(stringResource(R.string.remote_forget), color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmForget = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** Estado de la conexión con un punto de color. TalkBack lo anuncia al cambiar. */
@Composable
private fun StatusCard(state: ConnectionState, host: String, reconnecting: Boolean, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (label, dot) = when {
        reconnecting && state != ConnectionState.Connected ->
            stringResource(R.string.remote_status_reconnecting) to colors.onSurfaceVariant
        else -> statusLabel(state)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .border(0.5.dp, colors.outlineVariant, CardShape)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(dot))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_tv_address, host), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (state is ConnectionState.Failed && !reconnecting) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.remote_retry), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun statusLabel(state: ConnectionState): Pair<String, androidx.compose.ui.graphics.Color> {
    val colors = MaterialTheme.colorScheme
    return when (state) {
        ConnectionState.Connected -> stringResource(R.string.remote_status_connected) to colors.primary
        ConnectionState.Connecting, ConnectionState.Searching -> stringResource(R.string.remote_status_connecting) to colors.onSurfaceVariant
        is ConnectionState.Failed -> stringResource(R.string.remote_status_failed) to colors.error
        else -> stringResource(R.string.remote_status_idle) to colors.onSurfaceVariant
    }
}
