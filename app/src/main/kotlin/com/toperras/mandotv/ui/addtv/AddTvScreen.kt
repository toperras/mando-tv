package com.toperras.mandotv.ui.addtv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toperras.mandotv.R
import com.toperras.mandotv.discovery.TvDiscovery
import com.toperras.mandotv.ui.components.ActionTile
import com.toperras.mandotv.ui.components.BackTopBar
import com.toperras.mandotv.ui.components.PrimaryButton
import com.toperras.mandotv.ui.components.ScreenColumn
import com.toperras.mandotv.ui.components.SectionTitle

private val IP_PATTERN = Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$")

/**
 * Buscar tele: lista las Android TV que responden por mDNS en la wifi y
 * siempre ofrece escribir la IP a mano (por si la búsqueda falla).
 */
@Composable
fun AddTvScreen(onTvChosen: (host: String, name: String, foundByDiscovery: Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val discovery = remember { TvDiscovery(context.applicationContext) }
    val tvsFlow = remember { discovery.androidTvs() }
    val tvs by tvsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var ip by rememberSaveable { mutableStateOf("") }
    val ipValid = IP_PATTERN.matches(ip.trim())

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.addtv_title), onBack) },
        containerColor = colors.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenColumn(Modifier.padding(bottom = 48.dp)) {
                Text(stringResource(R.string.addtv_hint), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

                SectionTitle(stringResource(R.string.addtv_found))
                tvs.forEach { tv ->
                    ActionTile(
                        icon = Icons.Filled.Tv,
                        title = tv.name,
                        description = "${stringResource(R.string.addtv_android_tv)} · ${tv.host}",
                        onClick = { onTvChosen(tv.host, tv.name, true) },
                        iconTint = colors.primary,
                        iconBackground = colors.primaryContainer,
                    )
                }
                // La búsqueda sigue mientras la pantalla está abierta.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.addtv_searching), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }

                SectionTitle(stringResource(R.string.addtv_manual))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.filter { c -> c.isDigit() || c == '.' }.take(15) },
                    label = { Text(stringResource(R.string.addtv_ip_label)) },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true,
                    isError = ip.isNotEmpty() && !ipValid,
                    supportingText = {
                        Text(
                            stringResource(
                                if (ip.isNotEmpty() && !ipValid) R.string.addtv_ip_invalid else R.string.addtv_ip_help,
                            ),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (ipValid) onTvChosen(ip.trim(), ip.trim(), false) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = stringResource(R.string.addtv_connect),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = ipValid,
                    onClick = { onTvChosen(ip.trim(), ip.trim(), false) },
                )
            }
        }
    }
}
