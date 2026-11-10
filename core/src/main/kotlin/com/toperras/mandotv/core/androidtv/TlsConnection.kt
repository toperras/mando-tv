package com.toperras.mandotv.core.androidtv

import com.toperras.mandotv.core.proto.ProtoWriter
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

/**
 * Identidad del mando: certificado y clave privada que presentamos a la tele.
 * En Android la clave vive en el AndroidKeyStore (nunca sale de allí).
 */
interface ClientIdentity {
    val certificate: X509Certificate
    val privateKey: PrivateKey
}

/**
 * Conexión TLS con certificado de cliente, con mensajes protobuf precedidos
 * por su longitud (varint). Las operaciones son bloqueantes: llamarlas desde
 * Dispatchers.IO.
 *
 * Seguridad: las teles usan certificados autofirmados, así que aquí se acepta
 * el certificado de la tele SOLO en esta conexión (no se toca nada global).
 * Quien la usa compara [serverFingerprint] con la huella guardada.
 */
class TlsConnection private constructor(
    private val socket: SSLSocket,
    val serverCertificate: X509Certificate,
) {
    private val input: InputStream = BufferedInputStream(socket.inputStream)
    private val output = socket.outputStream
    private val writeLock = Any()

    /** Huella SHA-256 del certificado de la tele, en hexadecimal. */
    val serverFingerprint: String = MessageDigest.getInstance("SHA-256")
        .digest(serverCertificate.encoded)
        .joinToString("") { "%02X".format(it) }

    /** Tiempo máximo esperando datos de la tele; 0 = sin límite. */
    fun setReadTimeout(millis: Int) {
        socket.soTimeout = millis
    }

    /** Envía un mensaje en una sola escritura (así nunca se envía a medias ni dos veces). */
    fun send(message: ByteArray) {
        val frame = ProtoWriter.encodeVarint(message.size.toLong()) + message
        synchronized(writeLock) {
            output.write(frame)
            output.flush()
        }
    }

    /** Lee el siguiente mensaje completo. Lanza EOFException si la tele cierra. */
    fun receive(): ByteArray {
        var length = 0
        var shift = 0
        while (true) {
            val byte = input.read()
            if (byte < 0) throw EOFException("La tele ha cerrado la conexión")
            length = length or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) break
            shift += 7
            if (shift > 28) throw IllegalStateException("Longitud de mensaje no válida")
        }
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = input.read(buffer, read, length - read)
            if (count < 0) throw EOFException("La tele ha cerrado la conexión")
            read += count
        }
        return buffer
    }

    fun close() {
        runCatching { socket.close() }
    }

    companion object {
        fun open(host: String, port: Int, identity: ClientIdentity, timeoutMs: Int): TlsConnection {
            val context = SSLContext.getInstance("TLS")
            context.init(arrayOf(FixedKeyManager(identity)), arrayOf(AcceptTvCertificate), null)

            val plain = Socket()
            try {
                plain.connect(InetSocketAddress(host, port), timeoutMs)
                plain.tcpNoDelay = true
                val socket = context.socketFactory.createSocket(plain, host, port, true) as SSLSocket
                socket.soTimeout = timeoutMs
                socket.startHandshake()
                val certificate = socket.session.peerCertificates.firstOrNull() as? X509Certificate
                    ?: throw IllegalStateException("La tele no ha enviado certificado")
                return TlsConnection(socket, certificate)
            } catch (error: Exception) {
                runCatching { plain.close() }
                throw error
            }
        }
    }
}

/** Presenta siempre la identidad del mando, pida lo que pida la tele. */
private class FixedKeyManager(private val identity: ClientIdentity) : X509ExtendedKeyManager() {
    private val alias = "mandotv"
    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = alias
    override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?) = alias
    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(alias)
    override fun getCertificateChain(alias: String?) = arrayOf(identity.certificate)
    override fun getPrivateKey(alias: String?) = identity.privateKey
    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null
}

/** Acepta el certificado autofirmado de la tele (solo en esta conexión). */
private object AcceptTvCertificate : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
