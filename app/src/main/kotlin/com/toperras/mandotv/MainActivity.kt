package com.toperras.mandotv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toperras.mandotv.data.AppSettings
import com.toperras.mandotv.data.SettingsRepository
import com.toperras.mandotv.ui.components.LocalHapticsEnabled
import com.toperras.mandotv.ui.home.HomeScreen
import com.toperras.mandotv.ui.remote.RemoteDemoScreen
import com.toperras.mandotv.ui.settings.SettingsScreen
import com.toperras.mandotv.ui.theme.MandoTvTheme
import com.toperras.mandotv.ui.theme.isDarkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MandoTvApp() }
    }
}

/** Rutas de navegación. */
private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DEMO = "demo"
}

@Composable
private fun MandoTvApp() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context.applicationContext) }
    // null hasta leer los ajustes: así no aparece un instante en claro y luego en oscuro.
    val settings: AppSettings? by repository.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val current = settings ?: return

    MandoTvTheme(dark = isDarkTheme(current.theme)) {
        CompositionLocalProvider(LocalHapticsEnabled provides current.hapticsEnabled) {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        onOpenDemo = { nav.navigate(Routes.DEMO) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settings = current,
                        onThemeChange = { scope.launch { repository.setTheme(it) } },
                        onHapticsChange = { scope.launch { repository.setHapticsEnabled(it) } },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(Routes.DEMO) {
                    RemoteDemoScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
