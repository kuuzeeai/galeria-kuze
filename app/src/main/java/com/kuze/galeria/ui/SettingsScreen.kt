package com.kuze.galeria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kuze.galeria.data.DEFAULT_URL
import com.kuze.galeria.data.ServerApi
import com.kuze.galeria.data.SettingsStore
import kotlinx.coroutines.launch

private sealed interface TestState {
    data object Running : TestState
    data class Ok(val text: String) : TestState
    data class Fail(val text: String) : TestState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    api: ServerApi,
    firstRun: Boolean,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(settings.baseUrl.ifBlank { DEFAULT_URL }) }
    var user by remember { mutableStateOf(settings.user) }
    var pass by remember { mutableStateOf(settings.pass) }
    var test by remember { mutableStateOf<TestState?>(null) }

    fun saveFields() {
        settings.baseUrl = url
        settings.user = user
        settings.pass = pass
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (firstRun) "Conectar con el servidor" else "Ajustes") },
                navigationIcon = {
                    if (!firstRun) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (firstRun) {
                    "Escribe la dirección del servidor de fotos y el usuario y la clave que te dio miniserve."
                } else {
                    "Cambia la dirección si el celular viejo tiene otra IP."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Dirección del servidor") },
                placeholder = { Text(DEFAULT_URL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Clave") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Estos datos se guardan cifrados dentro de este teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    saveFields()
                    test = TestState.Running
                    scope.launch {
                        test = try {
                            val listing = api.list("/")
                            val fotos = listing.folders.size
                            val archivos = listing.files.size
                            TestState.Ok("Conexión correcta: $fotos carpetas y $archivos archivos en la raíz.")
                        } catch (e: Exception) {
                            TestState.Fail(e.message ?: "No se pudo conectar")
                        }
                    }
                },
                enabled = test != TestState.Running,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (test == TestState.Running) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Probar conexión")
                }
            }

            when (val state = test) {
                is TestState.Ok -> Text(state.text, color = MaterialTheme.colorScheme.primary)
                is TestState.Fail -> Text(state.text, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }

            Button(
                onClick = {
                    saveFields()
                    onSaved()
                },
                enabled = url.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (firstRun) "Guardar y ver fotos" else "Guardar")
            }

            if (!firstRun) {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar")
                }
            }
        }
    }
}
