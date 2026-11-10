package com.toperras.mandotv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toperras.mandotv.BuildConfig
import com.toperras.mandotv.R
import com.toperras.mandotv.ui.components.ActionTile
import com.toperras.mandotv.ui.components.AppCard
import com.toperras.mandotv.ui.components.IconBadge
import com.toperras.mandotv.ui.components.ScreenColumn
import com.toperras.mandotv.ui.components.SectionTitle
import com.toperras.mandotv.ui.components.rememberHaptics
import com.toperras.mandotv.ui.theme.LocalBrandColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, onOpenDemo: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val brand = LocalBrandColors.current
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val connectSoon = stringResource(R.string.home_connect_soon)
    val lostSoon = stringResource(R.string.home_lost_soon)

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenColumn(Modifier.padding(top = 12.dp, bottom = 48.dp)) {
                // Barra superior: logo + nombre + Ajustes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(brand.gradient),
                    ) {
                        Icon(Icons.Filled.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp).weight(1f).semantics { heading() },
                    )
                    val settingsLabel = stringResource(R.string.home_settings)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(0.5.dp, colors.outlineVariant, CircleShape)
                            .clickable(role = Role.Button, onClickLabel = settingsLabel) {
                                haptics.tap()
                                onOpenSettings()
                            },
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = settingsLabel)
                    }
                }

                // Tarjeta principal con degradado y círculos decorativos
                val heroShape = RoundedCornerShape(28.dp)
                Box(Modifier.fillMaxWidth().clip(heroShape).background(brand.gradient)) {
                    Box(
                        Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-80).dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
                    )
                    Box(
                        Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = (-60).dp, y = 50.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
                    )
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
                        Text(
                            stringResource(R.string.home_hero_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(R.string.home_hero_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable(role = Role.Button) {
                                    haptics.tap()
                                    scope.launch { snackbar.showSnackbar(connectSoon) }
                                }
                                .padding(horizontal = 24.dp),
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF312E81))
                            Text(
                                stringResource(R.string.home_connect_tv),
                                color = Color(0xFF312E81),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }

                SectionTitle(stringResource(R.string.home_my_tvs))
                AppCard {
                    IconBadge(Icons.Outlined.Tv, colors.onSurfaceVariant, colors.surfaceVariant, size = 56)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_no_tvs_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.home_no_tvs_body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }

                SectionTitle(stringResource(R.string.home_more))
                ActionTile(
                    icon = Icons.Outlined.SportsEsports,
                    title = stringResource(R.string.home_demo_title),
                    description = stringResource(R.string.home_demo_body),
                    onClick = onOpenDemo,
                    iconTint = colors.primary,
                    iconBackground = colors.primaryContainer,
                )
                ActionTile(
                    icon = Icons.Outlined.SupportAgent,
                    title = stringResource(R.string.home_lost_title),
                    description = stringResource(R.string.home_lost_body),
                    onClick = { scope.launch { snackbar.showSnackbar(lostSoon) } },
                    iconTint = brand.warning,
                    iconBackground = brand.warningSoft,
                    badge = stringResource(R.string.common_coming_soon),
                )

                Text(
                    stringResource(R.string.home_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}
