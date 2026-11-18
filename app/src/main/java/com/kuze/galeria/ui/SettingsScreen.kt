package com.kuze.galeria.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kuze.galeria.data.DEFAULT_URL
import com.kuze.galeria.data.ServerApi
import com.kuze.galeria.data.SettingsStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface TestState {
    data object Running : TestState
    data class Ok(val text: String) : TestState
    data class Fail(val text: String) : TestState
}

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    api: ServerApi,
    firstRun: Boolean,
    palette: String,
    columns: Int,
    font: String,
    itemCount: Int,
    onPalette: (String) -> Unit,
    onColumns: (Int) -> Unit,
    onFont: (String) -> Unit,
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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!firstRun) {
                    CircleIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Volver",
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onBack,
                        size = 46.dp
                    )
                }
                Text(
                    text = if (firstRun) "Conectar" else "Ajustes",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (firstRun) {
                            "Escribe la dirección del servidor de fotos y el usuario y la clave de miniserve. Se guardan cifrados en este teléfono."
                        } else {
                            "Cambia la dirección si el celular viejo tiene otra IP. Los datos quedan cifrados en este teléfono."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Dirección del servidor") },
                placeholder = { Text(DEFAULT_URL) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Usuario") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Clave") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = {
                    saveFields()
                    test = TestState.Running
                    scope.launch {
                        test = try {
                            val listing = api.list("/")
                            val carpetas = listing.folders.size
                            val archivos = listing.files.size
                            TestState.Ok("Conexión correcta: $carpetas carpetas y $archivos archivos en la raíz.")
                        } catch (e: Exception) {
                            TestState.Fail(e.message ?: "No se pudo conectar")
                        }
                    }
                },
                enabled = test != TestState.Running,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
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
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (firstRun) "Guardar y ver fotos" else "Guardar")
            }

            if (!firstRun) {
                DesignSection(
                    palette = palette,
                    columns = columns,
                    font = font,
                    itemCount = itemCount,
                    onPalette = onPalette,
                    onColumns = onColumns,
                    onFont = onFont
                )

                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun DesignSection(
    palette: String,
    columns: Int,
    font: String,
    itemCount: Int,
    onPalette: (String) -> Unit,
    onColumns: (Int) -> Unit,
    onFont: (String) -> Unit
) {
    Text(
        text = "Diseño",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(top = 14.dp)
    )
    Text(
        text = "Los cambios se ven al instante.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    DesignLabel("Paleta de colores")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paletteOptions.forEach { option ->
            val selected = option.id == palette
            Surface(
                onClick = { onPalette(option.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(option.swatch)
                    )
                    Text(option.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    DesignLabel("Fotos por fila")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(2, 3, 4).forEach { option ->
            val selected = option == columns
            Surface(
                onClick = { onColumns(option) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$option por fila", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "≈${approxKbFor(option)} KB",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    val totalMb = (approxKbFor(columns).toLong() * itemCount / 1024.0).roundToInt()
    Text(
        text = "Con $columns por fila cada miniatura pesa ≈${approxKbFor(columns)} KB" +
            if (itemCount > 0) " y la caché completa ≈$totalMb MB para $itemCount archivos." else ".",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = "Al cambiar de filas se descarga solo la miniatura de ese tamaño.\n" +
            "En el teléfono solo se guardan miniaturas: las fotos completas se ven sin ocupar espacio.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    DesignLabel("Fuente")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fontOptions.forEach { option ->
            val selected = option.id == font
            Surface(
                onClick = { onFont(option.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = option.family()),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DesignLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 6.dp)
    )
}
