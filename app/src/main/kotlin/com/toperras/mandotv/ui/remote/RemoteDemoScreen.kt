package com.toperras.mandotv.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toperras.mandotv.R
import com.toperras.mandotv.core.RemoteKey
import com.toperras.mandotv.ui.components.BackTopBar
import com.toperras.mandotv.ui.components.CardShape
import com.toperras.mandotv.ui.components.RemotePanel
import com.toperras.mandotv.ui.components.ScreenColumn
import com.toperras.mandotv.ui.components.label
import com.toperras.mandotv.ui.theme.LocalBrandColors

/**
 * Mando sin tele conectada. Muestra la última tecla y un contador:
 * sirve para comprobar que un toque cuenta exactamente una vez.
 */
@Composable
fun RemoteDemoScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val brand = LocalBrandColors.current
    var lastKey by rememberSaveable { mutableStateOf<RemoteKey?>(null) }
    var count by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.remote_title), onBack) },
        containerColor = colors.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ScreenColumn(Modifier.padding(bottom = 48.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(brand.warningSoft)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Outlined.Science, contentDescription = null, tint = brand.warning)
                    Text(stringResource(R.string.remote_demo_banner), color = brand.warning, style = MaterialTheme.typography.bodyMedium)
                }

                // Pantallita con la última tecla: TalkBack la anuncia al cambiar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(colors.surface)
                        .border(0.5.dp, colors.outlineVariant, CardShape)
                        .padding(16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        lastKey?.let { stringResource(R.string.remote_last_key, it.label()) }
                            ?: stringResource(R.string.remote_no_key_yet),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.remote_press_count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }

                RemotePanel(onKey = { key ->
                    lastKey = key
                    count++
                })
            }
        }
    }
}
