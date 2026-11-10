package com.toperras.mandotv.ui.remote

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toperras.mandotv.BuildConfig
import com.toperras.mandotv.core.ConnectionError
import com.toperras.mandotv.core.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
class RemoteViewModel(application: Application, private val tvId: String) : AndroidViewModel(application) {
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

    /** false si el aparato no controla el volumen por red (ver AndroidTvController.volumeControl). */
    val volumeControl: StateFlow<Boolean?> = controller
        .flatMapLatest { (it as? AndroidTvController)?.volumeControl ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** true mientras la app intenta recuperar sola una conexión que se cortó. */
    private val _reconnecting = MutableStateFlow(false)
    val reconnecting: StateFlow<Boolean> = _reconnecting.asStateFlow()

    /** true si estuvo conectada, se cortó y los reintentos automáticos no lo han arreglado. */
    private val _connectionLost = MutableStateFlow(false)
    val connectionLost: StateFlow<Boolean> = _connectionLost.asStateFlow()

    private var wantsConnection = false
    private var wasConnected = false
    private var reconnectJob: Job? = null

    init {
        // Reconexión automática: un corte breve de wifi (ahorro de energía del
        // móvil, cambio de banda...) no debe obligar al usuario a pulsar nada.
        viewModelScope.launch {
            connection.collect { state ->
                when (state) {
                    ConnectionState.Connected -> {
                        wasConnected = true
                        _reconnecting.value = false
                        _connectionLost.value = false
                    }
                    is ConnectionState.Failed -> {
                        val recoverable = state.error != ConnectionError.NOT_PAIRED
                        if (wantsConnection && wasConnected && recoverable) scheduleReconnect()
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Reintenta con esperas crecientes (1, 2, 4 y 8 s). Cada intento espera su
     * propio resultado: no basta con observar el estado, porque dos fallos
     * idénticos seguidos no producen un cambio visible en el StateFlow.
     */
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        _reconnecting.value = true
        reconnectJob = viewModelScope.launch {
            for (attempt in 0 until MAX_RECONNECT_ATTEMPTS) {
                delay(1_000L shl attempt)
                val current = controller.value ?: return@launch
                if (!wantsConnection) return@launch
                current.connect()
                val result = withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) {
                    current.state.first { it == ConnectionState.Connected || it is ConnectionState.Failed }
                }
                if (result == ConnectionState.Connected) return@launch // el colector limpia los avisos
            }
            _reconnecting.value = false
            _connectionLost.value = true
        }
    }

    init {
        viewModelScope.launch {
            val saved = repository.get(tvId)
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
                    log = { message, error -> Log.d("MandoTV.Remote", message, error) },
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
        reconnectJob?.cancel()
        _reconnecting.value = false
        viewModelScope.launch { controller.value?.disconnect() }
    }

    /** Conexión pedida por el usuario (o al abrir la pantalla): reinicia los reintentos. */
    fun connect() {
        val current = controller.value ?: return
        reconnectJob?.cancel()
        _reconnecting.value = false
        _connectionLost.value = false
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

    private companion object {
        const val MAX_RECONNECT_ATTEMPTS = 4
        const val ATTEMPT_TIMEOUT_MS = 12_000L
    }

    override fun onCleared() {
        val current = controller.value
        // viewModelScope ya está cancelado aquí: cerramos el socket directamente.
        kotlinx.coroutines.runBlocking { current?.disconnect() }
    }
}
