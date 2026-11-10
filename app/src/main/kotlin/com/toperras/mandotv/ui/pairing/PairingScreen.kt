package com.toperras.mandotv.ui.pairing

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toperras.mandotv.R
import com.toperras.mandotv.ui.components.BackTopBar
import com.toperras.mandotv.ui.components.ErrorExplanation
import com.toperras.mandotv.ui.components.PrimaryButton
import com.toperras.mandotv.ui.components.ScreenColumn

@Composable
fun PairingScreen(host: String, name: String, onPaired: () -> Unit, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val vm: PairingViewModel = viewModel(key = "pair-$host") { PairingViewModel(application, host, name) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(state) {
        if (state == PairingUiState.Paired) onPaired()
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.pair_title), onBack) },
        containerColor = colors.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenColumn(Modifier.padding(top = 8.dp, bottom = 48.dp)) {
                when (val current = state) {
                    PairingUiState.Connecting, PairingUiState.Paired -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            Text(stringResource(R.string.pair_connecting, name), style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    is PairingUiState.AwaitingCode -> CodeEntry(
                        wrongCode = current.wrongCode,
                        submitting = current.submitting,
                        onSubmit = vm::submit,
                    )

                    is PairingUiState.Failed -> {
                        Text(stringResource(R.string.pair_failed_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        ErrorExplanation(current.error, host, current.detail)
                        PrimaryButton(text = stringResource(R.string.pair_retry), icon = Icons.Filled.Refresh, onClick = vm::start)
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeEntry(wrongCode: Boolean, submitting: Boolean, onSubmit: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val complete = code.length == 6

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Text(stringResource(R.string.pair_code_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.pair_code_body), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    OutlinedTextField(
        value = code,
        onValueChange = { input ->
            // Solo 0-9 y A-F, en mayúsculas, máximo 6
            code = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
        },
        label = { Text(stringResource(R.string.pair_code_label)) },
        singleLine = true,
        isError = wrongCode,
        supportingText = if (wrongCode) {
            { Text(stringResource(R.string.pair_wrong_code)) }
        } else {
            null
        },
        textStyle = TextStyle(
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (complete) onSubmit(code) }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    PrimaryButton(
        text = stringResource(R.string.pair_submit),
        icon = Icons.Filled.Link,
        enabled = complete,
        loading = submitting,
        onClick = { onSubmit(code) },
    )
}
