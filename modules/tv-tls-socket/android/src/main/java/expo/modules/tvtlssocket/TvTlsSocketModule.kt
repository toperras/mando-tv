package expo.modules.tvtlssocket

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/*
 * Módulo nativo mínimo para hablar con teles Android TV / Google TV.
 *
 * Hace solo lo que JavaScript no puede hacer:
 *  1. Crear y guardar la identidad del mando (clave RSA + certificado) en el
 *     AndroidKeyStore. La clave privada nunca sale del almacén seguro y
 *     sobrevive al cierre de la app.
 *  2. Abrir una conexión TLS presentando ese certificado de cliente.
 *
 * Toda la lógica del protocolo (mensajes, emparejamiento, teclas) está en
 * TypeScript para poder reutilizarla en iOS.
 *
 * Seguridad: las teles usan certificados autofirmados, así que esta conexión
 * acepta el certificado de la tele SOLO en estos sockets (no se toca la
 * configuración global). JavaScript recibe la huella SHA-256 del certificado
 * de la tele y la compara con la guardada al emparejar.
 */
class TvTlsSocketModule : Module() {
  private val sockets = ConcurrentHashMap<String, SSLSocket>()
  private val executor = Executors.newCachedThreadPool()

  override fun definition() = ModuleDefinition {
    Name("TvTlsSocket")

    Events("onData", "onClose")

    AsyncFunction("ensureClientIdentity") { alias: String ->
      val certificate = getOrCreateCertificate(alias)
      publicKeyInfo(certificate)
    }

    AsyncFunction("deleteClientIdentity") { alias: String ->
      val keyStore = loadKeyStore()
      if (keyStore.containsAlias(alias)) {
        keyStore.deleteEntry(alias)
      }
    }

    AsyncFunction("connect") { id: String, host: String, port: Int, alias: String, timeoutMs: Int, promise: Promise ->
      executor.execute {
        try {
          val socket = openSocket(host, port, alias, timeoutMs)
          sockets.put(id, socket)?.let { closeQuietly(it) }
          val serverCertificate = socket.session.peerCertificates.firstOrNull() as? X509Certificate
            ?: throw TlsException("La tele no ha enviado certificado")
          val info = publicKeyInfo(serverCertificate).toMutableMap()
          info["fingerprint"] = sha256Hex(serverCertificate.encoded)
          startReading(id, socket)
          promise.resolve(info)
        } catch (error: Exception) {
          promise.reject(TlsException(describe(error)))
        }
      }
    }

    AsyncFunction("write") { id: String, base64: String, promise: Promise ->
      val socket = sockets[id]
      if (socket == null) {
        promise.reject(TlsException("Conexión cerrada"))
        return@AsyncFunction
      }
      executor.execute {
        try {
          val bytes = Base64.decode(base64, Base64.NO_WRAP)
          // Un solo write por mensaje: así un mensaje nunca se envía dos veces.
          synchronized(socket) {
            socket.outputStream.write(bytes)
            socket.outputStream.flush()
          }
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(TlsException(describe(error)))
        }
      }
    }

    Function("close") { id: String ->
      sockets.remove(id)?.let { closeQuietly(it) }
    }

    OnDestroy {
      sockets.values.forEach { closeQuietly(it) }
      sockets.clear()
      executor.shutdownNow()
    }
  }

  // ---------- Identidad del mando ----------

  private fun loadKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

  private fun getOrCreateCertificate(alias: String): X509Certificate {
    val keyStore = loadKeyStore()
    if (!keyStore.containsAlias(alias)) {
      val start = Calendar.getInstance()
      val end = Calendar.getInstance().apply { add(Calendar.YEAR, 20) }
      val spec = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT
      )
        .setKeySize(2048)
        .setCertificateSubject(X500Principal("CN=atvremote"))
        .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
        .setCertificateNotBefore(start.time)
        .setCertificateNotAfter(end.time)
        // TLS 1.2 firma con PKCS#1 y distintos resúmenes; TLS 1.3 exige PSS.
        .setDigests(
          KeyProperties.DIGEST_NONE,
          KeyProperties.DIGEST_SHA256,
          KeyProperties.DIGEST_SHA384,
          KeyProperties.DIGEST_SHA512
        )
        .setSignaturePaddings(
          KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
          KeyProperties.SIGNATURE_PADDING_RSA_PSS
        )
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
        .build()
      KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE).apply {
        initialize(spec)
        generateKeyPair()
      }
    }
    return keyStore.getCertificate(alias) as X509Certificate
  }

  private fun publicKeyInfo(certificate: X509Certificate): Map<String, String> {
    val key = certificate.publicKey as? RSAPublicKey
      ?: throw TlsException("El certificado no usa una clave RSA")
    return mapOf(
      "modulus" to unsignedHex(key.modulus),
      "exponent" to unsignedHex(key.publicExponent)
    )
  }

  // ---------- Conexión TLS ----------

  private fun openSocket(host: String, port: Int, alias: String, timeoutMs: Int): SSLSocket {
    val keyStore = loadKeyStore()
    val privateKey = keyStore.getKey(alias, null) as? PrivateKey
      ?: throw TlsException("No existe la identidad del mando")
    val certificate = keyStore.getCertificate(alias) as X509Certificate

    val context = SSLContext.getInstance("TLS")
    context.init(arrayOf(ClientKeyManager(alias, privateKey, certificate)), arrayOf(AcceptTvCertificate), null)

    val plain = Socket()
    plain.connect(InetSocketAddress(host, port), timeoutMs)
    plain.soTimeout = 0
    plain.tcpNoDelay = true
    val socket = context.socketFactory.createSocket(plain, host, port, true) as SSLSocket
    socket.soTimeout = timeoutMs
    socket.startHandshake()
    socket.soTimeout = 0
    return socket
  }

  private fun startReading(id: String, socket: SSLSocket) {
    executor.execute {
      val buffer = ByteArray(16 * 1024)
      var reason: String? = null
      try {
        val input = socket.inputStream
        while (true) {
          val count = input.read(buffer)
          if (count < 0) break
          if (count > 0) {
            sendEvent(
              "onData",
              mapOf("id" to id, "data" to Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP))
            )
          }
        }
      } catch (error: Exception) {
        reason = describe(error)
      } finally {
        // Solo avisamos si este socket sigue siendo el activo para ese id.
        if (sockets.remove(id, socket)) {
          closeQuietly(socket)
          sendEvent("onClose", mapOf("id" to id, "error" to reason))
        }
      }
    }
  }

  private fun closeQuietly(socket: Socket) {
    try {
      socket.close()
    } catch (_: Exception) {
    }
  }

  private fun describe(error: Exception): String =
    "${error.javaClass.simpleName}: ${error.message ?: "sin detalle"}"

  private fun unsignedHex(value: BigInteger): String = value.toString(16).uppercase()

  private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }

  /** Presenta siempre la identidad del mando, sea cual sea lo que pida la tele. */
  private class ClientKeyManager(
    private val alias: String,
    private val privateKey: PrivateKey,
    private val certificate: X509Certificate
  ) : X509ExtendedKeyManager() {
    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = alias
    override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?) = alias
    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(alias)
    override fun getCertificateChain(alias: String?) = arrayOf(certificate)
    override fun getPrivateKey(alias: String?) = privateKey
    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = null
    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) = null
  }

  /**
   * Acepta el certificado autofirmado de la tele. Solo se usa en este módulo;
   * la comprobación real se hace en JavaScript comparando la huella guardada.
   */
  private object AcceptTvCertificate : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
  }

  private class TlsException(message: String) : CodedException(message)

  private companion object {
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
  }
}
