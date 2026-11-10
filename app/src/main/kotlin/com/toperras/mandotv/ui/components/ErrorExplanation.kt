package com.toperras.mandotv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toperras.mandotv.R
import com.toperras.mandotv.core.ConnectionError
import com.toperras.mandotv.core.androidtv.PairingRejectedException
import com.toperras.mandotv.core.androidtv.WrongPinException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Traduce una excepción de red a un motivo que sabemos explicar. */
fun classifyError(error: Throwable): ConnectionError = when (error) {
    is WrongPinException -> ConnectionError.WRONG_PIN
    is PairingRejectedException -> ConnectionError.REJECTED_ON_TV
    is SocketTimeoutException -> ConnectionError.TIMEOUT
    is ConnectException, is NoRouteToHostException, is UnknownHostException -> ConnectionError.UNREACHABLE
    is SSLException, is IllegalStateException -> ConnectionError.UNEXPECTED_RESPONSE
    else -> ConnectionError.UNKNOWN
}

/**
 * Tarjeta de error que separa lo que la app ha OBSERVADO de las POSIBLES
 * causas, para no afirmar cosas que no sabemos.
 */
@Composable
fun ErrorExplanation(error: ConnectionError, host: String, detail: String?) {
    val colors = MaterialTheme.colorScheme
    val (observed, causes) = when (error) {
        ConnectionError.UNREACHABLE -> stringResource(R.string.error_unreachable, host) to stringResource(R.string.error_unreachable_causes)
        ConnectionError.TIMEOUT -> stringResource(R.string.error_timeout, host) to stringResource(R.string.error_timeout_causes)
        ConnectionError.REJECTED_ON_TV -> stringResource(R.string.error_rejected) to stringResource(R.string.error_rejected_causes)
        ConnectionError.WRONG_PIN -> stringResource(R.string.error_wrong_pin) to stringResource(R.string.error_wrong_pin_causes)
        ConnectionError.NOT_PAIRED -> stringResource(R.string.error_not_paired) to stringResource(R.string.error_not_paired_causes)
        ConnectionError.UNEXPECTED_RESPONSE -> stringResource(R.string.error_unexpected, host) to stringResource(R.string.error_unexpected_causes)
        ConnectionError.UNKNOWN -> stringResource(R.string.error_unknown, detail ?: "?") to stringResource(R.string.error_unknown_causes)
    }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.error)
                Text(stringResource(R.string.error_observed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(observed, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.error_causes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier,
            )
            Text(causes, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
