package com.nopal.tunascreen.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DiscoveryScreen(
    onServerChosen: (host: String, port: Int) -> Unit,
    viewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Conectá tu servidor NOPAL", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text("Buscando NOPAL en tu red local...")

        if (state.discovered.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(state.discovered) { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(server.name ?: server.host)
                            Text("${server.host}:${server.port}")
                        }
                        Button(onClick = {
                            viewModel.checkAndProceed(server.host, server.port) {
                                onServerChosen(server.host, server.port)
                            }
                        }) { Text("Elegir") }
                    }
                }
            }
        }

        Text("Conexión manual")
        OutlinedTextField(
            value = state.manualHost,
            onValueChange = viewModel::onManualHostChange,
            label = { Text("IP de NOPAL") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.manualPort,
            onValueChange = viewModel::onManualPortChange,
            label = { Text("Puerto") },
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            if (state.checking) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    val port = state.manualPort.toIntOrNull() ?: 8420
                    viewModel.checkAndProceed(state.manualHost, port) {
                        onServerChosen(state.manualHost, port)
                    }
                }) { Text("Conectar") }
            }
        }
    }
}
