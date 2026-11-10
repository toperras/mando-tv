package com.toperras.mandotv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toperras.mandotv.BuildConfig
import com.toperras.mandotv.R
import com.toperras.mandotv.data.AppSettings
import com.toperras.mandotv.data.ThemePreference
import com.toperras.mandotv.ui.components.AppCard
import com.toperras.mandotv.ui.components.BackTopBar
import com.toperras.mandotv.ui.components.IconBadge
import com.toperras.mandotv.ui.components.ScreenColumn
import com.toperras.mandotv.ui.components.SectionTitle
import com.toperras.mandotv.ui.components.rememberHaptics
import com.toperras.mandotv.ui.theme.LocalBrandColors

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeChange: (ThemePreference) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = rememberHaptics()

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.settings_title), onBack) },
        containerColor = colors.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ScreenColumn(Modifier.padding(bottom = 48.dp)) {
                SectionTitle(stringResource(R.string.settings_appearance))
                ThemeSelector(settings.theme) {
                    haptics.tap()
                    onThemeChange(it)
                }

                SectionTitle(stringResource(R.string.settings_feedback))
                AppCard {
                    IconBadge(Icons.Outlined.Vibration, colors.primary, colors.primaryContainer, size = 40)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_haptics), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_haptics_body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Switch(checked = settings.hapticsEnabled, onCheckedChange = onHapticsChange)
                }

                SectionTitle(stringResource(R.string.settings_language))
                AppCard {
                    IconBadge(Icons.Outlined.Language, colors.primary, colors.primaryContainer, size = 40)
                    Text(
                        stringResource(R.string.settings_language_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                SectionTitle(stringResource(R.string.settings_about))
                AppCard {
                    IconBadge(Icons.Outlined.Info, colors.primary, colors.primaryContainer, size = 40)
                    Text(
                        "${stringResource(R.string.app_name)} · ${stringResource(R.string.home_version, BuildConfig.VERSION_NAME)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Selector de tema en una sola fila: Sistema / Claro / Oscuro. */
@Composable
private fun ThemeSelector(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val brand = LocalBrandColors.current
    val options: List<Triple<ThemePreference, Int, ImageVector>> = listOf(
        Triple(ThemePreference.SYSTEM, R.string.settings_theme_system, Icons.Outlined.PhoneAndroid),
        Triple(ThemePreference.LIGHT, R.string.settings_theme_light, Icons.Outlined.LightMode),
        Triple(ThemePreference.DARK, R.string.settings_theme_dark, Icons.Outlined.DarkMode),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(brand.surfaceRaised)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        options.forEach { (value, label, icon) ->
            val isSelected = value == selected
            val shape = RoundedCornerShape(12.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.surface).border(0.5.dp, colors.outlineVariant, shape)
                        } else {
                            Modifier
                        },
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(value) }
                    .padding(vertical = 8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = if (isSelected) colors.primary else colors.onSurfaceVariant)
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                )
            }
        }
    }
}
