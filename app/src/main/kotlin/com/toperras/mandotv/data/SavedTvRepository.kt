package com.toperras.mandotv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.toperras.mandotv.core.ProtocolId
import com.toperras.mandotv.core.SavedTv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/*
 * Teles guardadas. Solo datos no secretos: nombre, IP, protocolo y la huella
 * pública del certificado de la tele. La clave del mando está en el
 * AndroidKeyStore (KeyStoreIdentity).
 *
 * Se pueden guardar varias teles. Cuando exista la versión Pro, la gratuita
 * quedará limitada a una (decisión aplazada hasta la fase de monetización).
 */

private val Context.tvStore by preferencesDataStore(name = "tvs")

class SavedTvRepository(private val context: Context) {
    private val tvsKey = stringPreferencesKey("tvs")

    val tvs: Flow<List<SavedTv>> = context.tvStore.data.map { prefs -> decode(prefs[tvsKey]) }

    suspend fun get(id: String): SavedTv? = tvs.first().firstOrNull { it.id == id }

    /** Guarda la tele; si ya existía (mismo id) la actualiza y la pone la primera. */
    suspend fun save(tv: SavedTv) {
        context.tvStore.edit { prefs ->
            prefs[tvsKey] = encode(listOf(tv) + decode(prefs[tvsKey]).filterNot { it.id == tv.id })
        }
    }

    suspend fun remove(id: String) {
        context.tvStore.edit { prefs ->
            prefs[tvsKey] = encode(decode(prefs[tvsKey]).filterNot { it.id == id })
            prefs.remove(fingerprintKey(id))
        }
    }

    suspend fun fingerprint(id: String): String? = context.tvStore.data.first()[fingerprintKey(id)]

    suspend fun saveFingerprint(id: String, fingerprint: String) {
        context.tvStore.edit { it[fingerprintKey(id)] = fingerprint }
    }

    suspend fun clearFingerprint(id: String) {
        context.tvStore.edit { it.remove(fingerprintKey(id)) }
    }

    private fun fingerprintKey(id: String) = stringPreferencesKey("fingerprint_$id")

    private fun encode(tvs: List<SavedTv>): String = JSONArray().apply {
        tvs.forEach { tv ->
            put(
                JSONObject()
                    .put("id", tv.id)
                    .put("name", tv.name)
                    .put("host", tv.host)
                    .put("protocol", tv.protocol.name)
                    .put("addedAt", tv.addedAt),
            )
        }
    }.toString()

    private fun decode(raw: String?): List<SavedTv> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.getJSONObject(index)
                val protocol = runCatching { ProtocolId.valueOf(item.getString("protocol")) }.getOrNull()
                    ?: return@mapNotNull null
                SavedTv(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    host = item.getString("host"),
                    protocol = protocol,
                    addedAt = item.optLong("addedAt"),
                )
            }
        }.getOrDefault(emptyList())
    }
}
