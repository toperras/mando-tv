package com.toperras.mandotv

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.toperras.mandotv.data.SavedTvRepository
import com.toperras.mandotv.ui.addtv.AddTvScreen
import com.toperras.mandotv.ui.pairing.PairingScreen
import com.toperras.mandotv.ui.remote.RemoteScreen
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
    const val ADD_TV = "add-tv"
    const val PAIR = "pair?host={host}&name={name}&found={found}"
    const val REMOTE = "remote?id={id}"

    fun pair(host: String, name: String, found: Boolean) =
        "pair?host=${Uri.encode(host)}&name=${Uri.encode(name)}&found=$found"

    fun remote(id: String) = "remote?id=${Uri.encode(id)}"
}

@Composable
private fun MandoTvApp() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context.applicationContext) }
    val tvRepository = remember { SavedTvRepository(context.applicationContext) }
    // null hasta leer los ajustes: así no aparece un instante en claro y luego en oscuro.
    val settings: AppSettings? by repository.settings.collectAsStateWithLifecycle(initialValue = null)
    val tvs by tvRepository.tvs.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val current = settings ?: return

    MandoTvTheme(dark = isDarkTheme(current.theme)) {
        CompositionLocalProvider(LocalHapticsEnabled provides current.hapticsEnabled) {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        savedTvs = tvs,
                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        onOpenDemo = { nav.navigate(Routes.DEMO) },
                        onConnectTv = { nav.navigate(Routes.ADD_TV) },
                        onOpenRemote = { id -> nav.navigate(Routes.remote(id)) },
                    )
                }
                composable(Routes.ADD_TV) {
                    AddTvScreen(
                        onTvChosen = { host, name, found -> nav.navigate(Routes.pair(host, name, found)) },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(
                    Routes.PAIR,
                    arguments = listOf(
                        navArgument("host") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                        navArgument("found") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { entry ->
                    val host = entry.arguments?.getString("host").orEmpty()
                    val name = entry.arguments?.getString("name").orEmpty()
                    PairingScreen(
                        host = host,
                        name = name,
                        foundByDiscovery = entry.arguments?.getBoolean("found") ?: false,
                        // Al emparejar, vamos al mando y quitamos de la pila las pantallas de conexión.
                        onPaired = { id ->
                            nav.navigate(Routes.remote(id)) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(
                    Routes.REMOTE,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    RemoteScreen(
                        tvId = entry.arguments?.getString("id").orEmpty(),
                        onRepair = { host, name ->
                            nav.navigate(Routes.pair(host, name, found = false)) { popUpTo(Routes.HOME) }
                        },
                        onBack = { nav.popBackStack() },
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
