package com.toperras.mandotv.core

import kotlinx.coroutines.flow.StateFlow

/** Protocolos que la app sabe hablar. Se irán añadiendo. */
enum class ProtocolId {
    SAMSUNG_LEGACY,
    SAMSUNG_TIZEN,
    LG_WEBOS,
    ANDROID_TV,
    INFRARED,
}

/** Motivos de error que la interfaz sabe explicar al usuario. */
enum class ConnectionError {
    /** Nadie responde en esa IP/puerto. */
    UNREACHABLE,
    /** Algo responde, pero no parece una tele compatible. */
    UNEXPECTED_RESPONSE,
    /** La tele no respondió a tiempo. */
    TIMEOUT,
    /** El usuario rechazó el aviso en la tele (o no lo aceptó a tiempo). */
    REJECTED_ON_TV,
    /** El código introducido no es el que muestra la tele. */
    WRONG_PIN,
    /** La tele ya no reconoce este móvil: hay que volver a emparejar. */
    NOT_PAIRED,
    UNKNOWN,
}

/**
 * Estado observable de la conexión. Incluye estados que no todos los
 * protocolos usan (aceptación en la tele, PIN), para que la interfaz sea común.
 */
sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Searching : ConnectionState
    data object Connecting : ConnectionState
    data object AwaitingTvAcceptance : ConnectionState
    data object AwaitingPin : ConnectionState
    data object Connected : ConnectionState
    data class Failed(val error: ConnectionError, val detail: String? = null) : ConnectionState
}

/** Lo que permite la tele conectada. La interfaz desactiva lo que no esté aquí. */
data class TvCapabilities(
    val keys: Set<RemoteKey>,
    /** Escribir texto en buscadores. */
    val text: Boolean = false,
    /** Encender la tele cuando está totalmente apagada. */
    val powerOnFromOff: Boolean = false,
)

/**
 * Contrato común de todos los adaptadores.
 *
 * Regla de oro: [sendKey] envía UNA orden y nunca reintenta por su cuenta,
 * para que un toque no pueda producir dos acciones en la tele.
 */
interface TvController {
    val protocol: ProtocolId
    val capabilities: TvCapabilities
    val state: StateFlow<ConnectionState>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun sendKey(key: RemoteKey)
    suspend fun sendText(text: String) {
        throw UnsupportedOperationException("Esta tele no admite escribir texto")
    }
    suspend fun submitPin(pin: String) {
        throw UnsupportedOperationException("Esta tele no usa PIN")
    }
}

/** Tele guardada en el móvil (sin secretos: tokens y claves van aparte, cifrados). */
data class SavedTv(
    val id: String,
    val name: String,
    val host: String,
    val protocol: ProtocolId,
    val addedAt: Long,
)
