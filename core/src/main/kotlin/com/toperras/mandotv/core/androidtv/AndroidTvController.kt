package com.toperras.mandotv.core.androidtv

import com.toperras.mandotv.core.ConnectionError
import com.toperras.mandotv.core.ConnectionState
import com.toperras.mandotv.core.ProtocolId
import com.toperras.mandotv.core.RemoteKey
import com.toperras.mandotv.core.TvCapabilities
import com.toperras.mandotv.core.TvController
import com.toperras.mandotv.core.proto.ProtoMessage
import com.toperras.mandotv.core.proto.ProtoWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Datos de este móvil que la tele pide al conectar. */
data class RemoteDeviceInfo(
    val model: String,
    val vendor: String,
    val packageName: String,
    val appVersion: String,
)

/**
 * Mando para Android TV / Google TV (protocolo Remote v2, puerto 6466).
 *
 * Al conectar, la tele envía «configure» y «set active»; respondemos a ambos y
 * a cada «ping» (si no, la tele corta). Si la tele ya no reconoce nuestro
 * certificado, el TLS falla o cierra enseguida → [ConnectionError.NOT_PAIRED].
 *
 * @param pinnedFingerprint huella del certificado de la tele guardada en la
 *   primera conexión. Si cambia, no nos conectamos (podría ser otro aparato).
 * @param onFirstFingerprint se llama la primera vez para que se guarde.
 */
class AndroidTvController(
    private val host: String,
    private val identity: ClientIdentity,
    private val deviceInfo: RemoteDeviceInfo,
    private var pinnedFingerprint: String?,
    private val onFirstFingerprint: (String) -> Unit,
    private val port: Int = REMOTE_PORT,
    /** Registro de diagnóstico (en Android se conecta a Logcat). */
    private val log: (message: String, error: Throwable?) -> Unit = { _, _ -> },
) : TvController {

    override val protocol = ProtocolId.ANDROID_TV
    override val capabilities = TvCapabilities(keys = KEY_CODES.keys)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /**
     * Lo que el aparato dice de su volumen al conectar: true si tiene un control
     * de volumen propio, false si no informa de ninguno (p. ej. Google TV Streamer
     * o Chromecast, donde el volumen lo lleva la tele por IR o HDMI-CEC), null si
     * aún no lo sabemos.
     */
    private val _volumeControl = MutableStateFlow<Boolean?>(null)
    val volumeControl: StateFlow<Boolean?> = _volumeControl.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connection: TlsConnection? = null
    private var readerJob: Job? = null

    override suspend fun connect() {
        disconnect()
        _state.value = ConnectionState.Connecting
        val conn = try {
            withContext(Dispatchers.IO) { TlsConnection.open(host, port, identity, CONNECT_TIMEOUT_MS) }
        } catch (error: Exception) {
            log("No se pudo conectar con $host:$port", error)
            _state.value = ConnectionState.Failed(classify(error, connected = false), error.message)
            return
        }

        val pinned = pinnedFingerprint
        if (pinned != null && pinned != conn.serverFingerprint) {
            conn.close()
            _state.value = ConnectionState.Failed(ConnectionError.NOT_PAIRED, "El certificado de la tele ha cambiado")
            return
        }

        // La tele envía un ping cada ~5 s (medido con el Google TV Streamer): si en
        // 20 s (4 pings) no llega nada, damos la conexión por perdida y la app
        // reintenta sola.
        conn.setReadTimeout(READ_TIMEOUT_MS)
        connection = conn
        readerJob = scope.launch { readLoop(conn) }
    }

    private fun CoroutineScope.readLoop(conn: TlsConnection) {
        var connected = false
        try {
            while (isActive) {
                val message = ProtoMessage.parse(conn.receive())
                if (!message.has(FIELD_PING_REQUEST)) {
                    log("← campos ${message.fieldNumbers} ${describe(message)}", null)
                }
                when {
                    message.has(FIELD_CONFIGURE) -> conn.send(RemoteMessages.configure(deviceInfo))
                    message.has(FIELD_SET_ACTIVE) -> {
                        conn.send(RemoteMessages.setActive())
                        if (pinnedFingerprint == null) {
                            pinnedFingerprint = conn.serverFingerprint
                            onFirstFingerprint(conn.serverFingerprint)
                        }
                        connected = true
                        _state.value = ConnectionState.Connected
                    }
                    message.has(FIELD_VOLUME_LEVEL) -> {
                        val maxVolume = message.message(FIELD_VOLUME_LEVEL)?.int(6) ?: 0
                        _volumeControl.value = maxVolume > 0
                    }
                    message.has(FIELD_PING_REQUEST) -> {
                        val value = message.message(FIELD_PING_REQUEST)?.int(1) ?: 0
                        conn.send(RemoteMessages.pingResponse(value))
                    }
                    // remote_error, remote_start, volumen...: de momento no hacen falta.
                }
            }
        } catch (error: Exception) {
            // Solo informamos si esta sigue siendo la conexión activa (no si la cerramos nosotros).
            log("Conexión terminada (conectado=$connected, activa=${connection === conn})", error)
            if (connection === conn) {
                connection = null
                conn.close()
                _state.value = ConnectionState.Failed(classify(error, connected), error.message)
            }
        }
    }

    override suspend fun disconnect() {
        val conn = connection
        connection = null
        readerJob?.cancel()
        readerJob = null
        conn?.close()
        _state.value = ConnectionState.Idle
    }

    override suspend fun sendKey(key: RemoteKey) {
        val conn = connection
        check(conn != null && _state.value == ConnectionState.Connected) { "No hay conexión con la tele" }
        val code = KEY_CODES[key] ?: throw UnsupportedOperationException("Tecla no admitida: $key")
        // Una sola escritura, sin reintentos: un toque nunca produce dos acciones.
        withContext(Dispatchers.IO) { conn.send(RemoteMessages.keyPress(code)) }
    }

    /** Resumen legible de los mensajes que nos interesan para diagnosticar. */
    private fun describe(message: ProtoMessage): String {
        message.message(FIELD_VOLUME_LEVEL)?.let { v ->
            return "volumen: modelo=${v.string(3)} max=${v.int(6)} nivel=${v.int(7)} silencio=${v.int(8)}"
        }
        message.message(FIELD_REMOTE_START)?.let { return "encendida=${it.int(1)}" }
        message.message(FIELD_ERROR)?.let { return "error de la tele: campos ${it.fieldNumbers}" }
        return ""
    }

    private fun classify(error: Exception, connected: Boolean): ConnectionError = when (error) {
        is SocketTimeoutException -> ConnectionError.TIMEOUT
        is ConnectException, is NoRouteToHostException, is UnknownHostException -> ConnectionError.UNREACHABLE
        // La tele no acepta nuestro certificado, o cierra nada más conectar:
        // ha olvidado este móvil y hay que volver a emparejar.
        is SSLException -> if (connected) ConnectionError.UNREACHABLE else ConnectionError.NOT_PAIRED
        is EOFException -> if (connected) ConnectionError.UNREACHABLE else ConnectionError.NOT_PAIRED
        else -> ConnectionError.UNKNOWN
    }

    companion object {
        const val REMOTE_PORT = 6466
        // Teles lentas (Mi TV 4A con Android 9) pueden tardar varios segundos en el
        // handshake TLS con certificado de cliente, sobre todo recién despertadas.
        private const val CONNECT_TIMEOUT_MS = 12_000
        private const val READ_TIMEOUT_MS = 20_000

        private const val FIELD_CONFIGURE = 1
        private const val FIELD_SET_ACTIVE = 2
        private const val FIELD_PING_REQUEST = 8
        private const val FIELD_ERROR = 3
        private const val FIELD_REMOTE_START = 40
        private const val FIELD_VOLUME_LEVEL = 50

        /** Teclas del mando → códigos de tecla de Android (KeyEvent). */
        val KEY_CODES: Map<RemoteKey, Int> = mapOf(
            RemoteKey.POWER to 26,
            RemoteKey.UP to 19,
            RemoteKey.DOWN to 20,
            RemoteKey.LEFT to 21,
            RemoteKey.RIGHT to 22,
            RemoteKey.OK to 23,
            RemoteKey.BACK to 4,
            RemoteKey.HOME to 3,
            RemoteKey.MENU to 82,
            RemoteKey.MUTE to 164,
            RemoteKey.VOLUME_UP to 24,
            RemoteKey.VOLUME_DOWN to 25,
            RemoteKey.CHANNEL_UP to 166,
            RemoteKey.CHANNEL_DOWN to 167,
        )
    }
}

/** Mensajes del protocolo del mando (RemoteMessage). */
internal object RemoteMessages {
    /** Funciones que anunciamos a la tele (mismo valor que usa la app oficial). */
    private const val FEATURES = 622
    private const val DIRECTION_SHORT = 3

    fun configure(info: RemoteDeviceInfo): ByteArray = ProtoWriter().message(1) {
        int(1, FEATURES)
        message(2) {
            string(1, info.model)
            string(2, info.vendor)
            int(3, 1)
            string(4, "1")
            string(5, info.packageName)
            string(6, info.appVersion)
        }
    }.toByteArray()

    fun setActive(): ByteArray = ProtoWriter().message(2) { int(1, FEATURES) }.toByteArray()

    fun pingResponse(value: Int): ByteArray = ProtoWriter().message(9) { int(1, value) }.toByteArray()

    fun keyPress(keyCode: Int): ByteArray = ProtoWriter().message(10) {
        int(1, keyCode)
        int(2, DIRECTION_SHORT)
    }.toByteArray()
}
