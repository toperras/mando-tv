package com.toperras.mandotv.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.toperras.mandotv.core.androidtv.ClientIdentity
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.security.auth.x500.X500Principal

/**
 * Identidad del mando guardada en el AndroidKeyStore: una clave RSA 2048 con
 * su certificado autofirmado. La clave privada nunca sale del almacén seguro
 * y se conserva al cerrar la app (se borra al desinstalarla).
 *
 * Se usa la misma identidad para todas las teles, como hace la app oficial.
 */
object KeyStoreIdentity {
    private const val KEY_STORE = "AndroidKeyStore"
    // v2: la v1 no permitía operaciones RSA sin relleno y el TLS fallaba (ver generate()).
    private const val ALIAS = "mandotv_remote_identity_v2"
    private const val OLD_ALIAS = "mandotv_remote_identity"

    /** Lento la primera vez (genera la clave): llamar desde Dispatchers.IO. */
    fun getOrCreate(): ClientIdentity {
        val keyStore = KeyStore.getInstance(KEY_STORE).apply { load(null) }
        if (keyStore.containsAlias(OLD_ALIAS)) keyStore.deleteEntry(OLD_ALIAS)
        if (!keyStore.containsAlias(ALIAS)) generate()
        val certificate = keyStore.getCertificate(ALIAS) as X509Certificate
        val privateKey = keyStore.getKey(ALIAS, null) as PrivateKey
        return object : ClientIdentity {
            override val certificate = certificate
            override val privateKey = privateKey
        }
    }

    private fun generate() {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.YEAR, 20) }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(2048)
            .setCertificateSubject(X500Principal("CN=atvremote"))
            .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            .setCertificateNotBefore(start.time)
            .setCertificateNotAfter(end.time)
            // TLS 1.2 firma con PKCS#1 y varios resúmenes; TLS 1.3 exige PSS.
            .setDigests(
                KeyProperties.DIGEST_NONE,
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512,
            )
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
            // Conscrypt (el TLS de Android) firma con claves del KeyStore haciendo
            // la operación RSA «en bruto» y aplicando él el relleno: sin NONE falla
            // con «RSA routines: internal error» durante el handshake.
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setRandomizedEncryptionRequired(false)
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE).apply {
            initialize(spec)
            generateKeyPair()
        }
    }
}
