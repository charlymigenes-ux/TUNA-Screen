package com.nopal.tunascreen.feature.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PairingScreen(
    host: String,
    port: Int,
    onPaired: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.paired) {
        if (state.paired) onPaired()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Emparejar con $host:$port", style = MaterialTheme.typography.headlineSmall)
        Text("Generá el código de 6 dígitos desde NOPAL (Configuración > TUNA-Screen) y escribilo acá.")
        OutlinedTextField(
            value = state.code,
            onValueChange = viewModel::onCodeChange,
            label = { Text("Código") },
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.submitting) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { viewModel.confirm(host, port) }) { Text("Emparejar") }
        }
    }
}
