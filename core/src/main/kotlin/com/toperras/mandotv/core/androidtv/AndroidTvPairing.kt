package com.toperras.mandotv.core.androidtv

import com.toperras.mandotv.core.proto.ProtoMessage
import com.toperras.mandotv.core.proto.ProtoWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

/** La tele ha rechazado el emparejamiento (status ≠ 200). */
class PairingRejectedException(val status: Int) : Exception("La tele ha rechazado el emparejamiento ($status)")

/** El código no corresponde al que muestra la tele. */
class WrongPinException : Exception("El código no es correcto")

/**
 * Emparejamiento Android TV Remote v2 (puerto 6467).
 *
 * 1. [begin]: nos presentamos y pedimos un código hexadecimal de 6 caracteres.
 *    Al terminar, la tele muestra el código en pantalla.
 * 2. [finish]: con el código que escribe el usuario calculamos el secreto y
 *    lo enviamos. Si la tele lo acepta, ya recordará este móvil.
 *
 * El primer byte del código permite comprobar en local si está bien escrito
 * ([isCodeValid]) sin gastar el intento en la tele.
 */
class AndroidTvPairing(
    private val host: String,
    private val identity: ClientIdentity,
    private val clientName: String,
    private val port: Int = PAIRING_PORT,
) {
    private var connection: TlsConnection? = null

    suspend fun begin() = withContext(Dispatchers.IO) {
        close()
        val conn = TlsConnection.open(host, port, identity, TIMEOUT_MS)
        connection = conn
        exchange(conn, PairingMessages.request(clientName), expectedField = 11)
        exchange(conn, PairingMessages.option(), expectedField = 20)
        exchange(conn, PairingMessages.configuration(), expectedField = 31)
        // Ahora la tele muestra el código. No ponemos límite de espera mientras
        // el usuario lo escribe.
        conn.setReadTimeout(0)
    }

    /** true si el código tiene el formato correcto y coincide con la tele. */
    fun isCodeValid(code: String): Boolean {
        val conn = connection ?: return false
        val normalized = code.trim().uppercase()
        if (!CODE_PATTERN.matches(normalized)) return false
        val hash = secretFor(conn, normalized)
        return hash[0] == normalized.substring(0, 2).toInt(16).toByte()
    }

    suspend fun finish(code: String) = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("Emparejamiento no iniciado")
        if (!isCodeValid(code)) throw WrongPinException()
        conn.setReadTimeout(TIMEOUT_MS)
        val response = sendAndReceive(conn, PairingMessages.secret(secretFor(conn, code.trim().uppercase())))
        when (val status = response.int(2) ?: 0) {
            STATUS_OK -> Unit
            STATUS_BAD_SECRET -> throw WrongPinException()
            else -> throw PairingRejectedException(status)
        }
        close()
    }

    fun close() {
        connection?.close()
        connection = null
    }

    /**
     * secreto = SHA-256(módulo y exponente del cliente, módulo y exponente de la
     * tele, últimos 4 caracteres del código). Los números van como bytes sin
     * signo y sin ceros a la izquierda (el exponente 65537 queda 01 00 01).
     */
    private fun secretFor(conn: TlsConnection, code: String): ByteArray {
        val client = identity.certificate.publicKey as RSAPublicKey
        val server = conn.serverCertificate.publicKey as RSAPublicKey
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(unsignedBytes(client.modulus))
        digest.update(unsignedBytes(client.publicExponent))
        digest.update(unsignedBytes(server.modulus))
        digest.update(unsignedBytes(server.publicExponent))
        digest.update(hexToBytes(code.substring(2)))
        return digest.digest()
    }

    private fun exchange(conn: TlsConnection, message: ByteArray, expectedField: Int) {
        val response = sendAndReceive(conn, message)
        val status = response.int(2) ?: 0
        if (status != STATUS_OK) throw PairingRejectedException(status)
        check(response.has(expectedField)) {
            "Respuesta inesperada de la tele (campos ${response.fieldNumbers})"
        }
    }

    private fun sendAndReceive(conn: TlsConnection, message: ByteArray): ProtoMessage {
        conn.send(message)
        return ProtoMessage.parse(conn.receive())
    }

    companion object {
        const val PAIRING_PORT = 6467
        private const val TIMEOUT_MS = 10_000
        private const val STATUS_OK = 200
        private const val STATUS_BAD_SECRET = 402
        private val CODE_PATTERN = Regex("^[0-9A-F]{6}$")

        internal fun unsignedBytes(value: BigInteger): ByteArray {
            val bytes = value.toByteArray()
            val firstNonZero = bytes.indexOfFirst { it != 0.toByte() }.coerceAtLeast(0)
            return bytes.copyOfRange(firstNonZero, bytes.size)
        }

        internal fun hexToBytes(hex: String): ByteArray =
            ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}

/** Mensajes del protocolo de emparejamiento («polo»). */
internal object PairingMessages {
    private const val PROTOCOL_VERSION = 2
    private const val STATUS_OK = 200
    private const val ENCODING_HEXADECIMAL = 3
    private const val CODE_LENGTH = 6
    private const val ROLE_INPUT = 1

    private fun envelope(build: ProtoWriter.() -> Unit): ByteArray =
        ProtoWriter().int(1, PROTOCOL_VERSION).int(2, STATUS_OK).apply(build).toByteArray()

    fun request(clientName: String) = envelope {
        message(10) {
            string(1, "atvremote") // service_name
            string(2, clientName) // client_name: lo que verá el usuario en la tele
        }
    }

    fun option() = envelope {
        message(20) {
            message(1) { int(1, ENCODING_HEXADECIMAL); int(2, CODE_LENGTH) } // input_encodings
            int(3, ROLE_INPUT) // preferred_role
        }
    }

    fun configuration() = envelope {
        message(30) {
            message(1) { int(1, ENCODING_HEXADECIMAL); int(2, CODE_LENGTH) } // encoding
            int(2, ROLE_INPUT) // client_role
        }
    }

    fun secret(hash: ByteArray) = envelope {
        message(40) { bytes(1, hash) }
    }
}
