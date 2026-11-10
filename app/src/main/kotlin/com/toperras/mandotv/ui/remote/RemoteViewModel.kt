package com.toperras.mandotv.ui.remote

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toperras.mandotv.BuildConfig
import com.toperras.mandotv.core.ConnectionState
import com.toperras.mandotv.core.ProtocolId
import com.toperras.mandotv.core.RemoteKey
import com.toperras.mandotv.core.SavedTv
import com.toperras.mandotv.core.TvController
import com.toperras.mandotv.core.androidtv.AndroidTvController
import com.toperras.mandotv.core.androidtv.RemoteDeviceInfo
import com.toperras.mandotv.data.KeyStoreIdentity
import com.toperras.mandotv.data.SavedTvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mantiene la conexión con la tele guardada. La pantalla llama a [resume]
 * al entrar en primer plano y a [pause] al salir: así la conexión se cierra
 * en segundo plano y se rehace sola al volver (sin pedir código otra vez).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SavedTvRepository(application)

    private val _tv = MutableStateFlow<SavedTv?>(null)
    val tv: StateFlow<SavedTv?> = _tv.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Tecla que no se pudo enviar (para avisar al usuario). */
    private val _failedKey = MutableStateFlow<RemoteKey?>(null)
    val failedKey: StateFlow<RemoteKey?> = _failedKey.asStateFlow()

    private val controller = MutableStateFlow<TvController?>(null)
    val connection: StateFlow<ConnectionState> = controller
        .flatMapLatest { it?.state ?: flowOf(ConnectionState.Idle) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Idle)

    val supportedKeys: Set<RemoteKey>? get() = controller.value?.capabilities?.keys

    private var wantsConnection = false

    init {
        viewModelScope.launch {
            val saved = repository.first()
            _tv.value = saved
            if (saved != null && saved.protocol == ProtocolId.ANDROID_TV) {
                val identity = withContext(Dispatchers.IO) { KeyStoreIdentity.getOrCreate() }
                controller.value = AndroidTvController(
                    host = saved.host,
                    identity = identity,
                    deviceInfo = RemoteDeviceInfo(
                        model = Build.MODEL,
                        vendor = Build.MANUFACTURER,
                        packageName = BuildConfig.APPLICATION_ID,
                        appVersion = BuildConfig.VERSION_NAME,
                    ),
                    pinnedFingerprint = repository.fingerprint(saved.id),
                    onFirstFingerprint = { fingerprint ->
                        viewModelScope.launch { repository.saveFingerprint(saved.id, fingerprint) }
                    },
                )
                if (wantsConnection) connect()
            }
            _loaded.value = true
        }
    }

    fun resume() {
        wantsConnection = true
        connect()
    }

    fun pause() {
        wantsConnection = false
        viewModelScope.launch { controller.value?.disconnect() }
    }

    fun connect() {
        val current = controller.value ?: return
        viewModelScope.launch { current.connect() }
    }

    fun send(key: RemoteKey) {
        val current = controller.value ?: return
        _failedKey.value = null
        viewModelScope.launch {
            try {
                current.sendKey(key)
            } catch (_: Exception) {
                _failedKey.value = key
            }
        }
    }

    fun forget(onDone: () -> Unit) {
        val saved = _tv.value ?: return
        viewModelScope.launch {
            controller.value?.disconnect()
            repository.remove(saved.id)
            onDone()
        }
    }

    override fun onCleared() {
        val current = controller.value
        // viewModelScope ya está cancelado aquí: cerramos el socket directamente.
        kotlinx.coroutines.runBlocking { current?.disconnect() }
    }
}
