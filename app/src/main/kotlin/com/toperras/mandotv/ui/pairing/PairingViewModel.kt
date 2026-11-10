package com.toperras.mandotv.ui.pairing

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toperras.mandotv.core.ConnectionError
import com.toperras.mandotv.core.ProtocolId
import com.toperras.mandotv.core.SavedTv
import com.toperras.mandotv.core.androidtv.AndroidTvPairing
import com.toperras.mandotv.core.androidtv.WrongPinException
import com.toperras.mandotv.data.KeyStoreIdentity
import com.toperras.mandotv.data.SavedTvRepository
import com.toperras.mandotv.ui.components.classifyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PairingUiState {
    data object Connecting : PairingUiState
    data class AwaitingCode(val wrongCode: Boolean = false, val submitting: Boolean = false) : PairingUiState
    data object Paired : PairingUiState
    data class Failed(val error: ConnectionError, val detail: String?) : PairingUiState
}

class PairingViewModel(
    application: Application,
    private val host: String,
    private val name: String,
) : AndroidViewModel(application) {
    private val repository = SavedTvRepository(application)
    private val _state = MutableStateFlow<PairingUiState>(PairingUiState.Connecting)
    val state: StateFlow<PairingUiState> = _state.asStateFlow()
    private var pairing: AndroidTvPairing? = null

    init {
        start()
    }

    /** Conecta con la tele y le pide que muestre un código. */
    fun start() {
        viewModelScope.launch {
            _state.value = PairingUiState.Connecting
            try {
                val identity = withContext(Dispatchers.IO) { KeyStoreIdentity.getOrCreate() }
                pairing?.close()
                // Este nombre es el que verá el usuario en la tele.
                val session = AndroidTvPairing(host, identity, clientName = "Mando TV (${Build.MODEL})")
                pairing = session
                session.begin()
                _state.value = PairingUiState.AwaitingCode()
            } catch (error: Exception) {
                _state.value = PairingUiState.Failed(classifyError(error), error.message)
            }
        }
    }

    fun submit(code: String) {
        val session = pairing ?: return
        // Comprobación local: si el código no cuadra, no gastamos el intento en la tele.
        if (!session.isCodeValid(code)) {
            _state.value = PairingUiState.AwaitingCode(wrongCode = true)
            return
        }
        viewModelScope.launch {
            _state.value = PairingUiState.AwaitingCode(submitting = true)
            try {
                session.finish(code)
                val id = "androidtv:$host"
                repository.save(SavedTv(id, name, host, ProtocolId.ANDROID_TV, System.currentTimeMillis()))
                // Huella nueva: se guardará en la primera conexión del mando.
                repository.clearFingerprint(id)
                _state.value = PairingUiState.Paired
            } catch (error: WrongPinException) {
                // La tele ha rechazado el código y cierra: hay que pedir otro.
                _state.value = PairingUiState.Failed(ConnectionError.WRONG_PIN, error.message)
            } catch (error: Exception) {
                _state.value = PairingUiState.Failed(classifyError(error), error.message)
            }
        }
    }

    override fun onCleared() {
        pairing?.close()
    }
}
