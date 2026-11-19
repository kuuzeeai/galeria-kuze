package com.kuze.galeria.ui

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kuze.galeria.data.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Editor sencillo: brillo, contraste, saturación y giro. Al guardar crea una copia
 * nueva en la galería (DCIM/Camera) y el original del servidor queda intacto.
 */
@Composable
fun EditorScreen(
    media: Media,
    loader: ImageLoader,
    onClose: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember(media.id) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var brightness by remember(media.id) { mutableFloatStateOf(0f) }
    var contrast by remember(media.id) { mutableFloatStateOf(1f) }
    var saturation by remember(media.id) { mutableFloatStateOf(1f) }
    var temperature by remember(media.id) { mutableFloatStateOf(0f) }
    var rotation by remember(media.id) { mutableIntStateOf(0) }
    var cropping by remember(media.id) { mutableStateOf(false) }

    LaunchedEffect(media.id) {
        val loaded = runCatching {
            if (media.isVideo) {
                // Extraer un frame del video para editarlo como imagen.
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, android.net.Uri.parse(media.url))
                    val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
                    frame
                }
            } else {
                val request = ImageRequest.Builder(context)
                    .data(media.url)
                    .size(2048)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                val drawable = (result as? SuccessResult)?.drawable
                drawable?.let { runCatching { it.toBitmap() }.getOrNull() }
            }
        }.getOrNull()
        if (loaded != null) {
            bitmap = loaded
        } else {
            error = "No se pudo abrir para editarlo"
        }
    }

    val preview = remember(bitmap, rotation) {
        val source = bitmap
        if (source == null || rotation % 360 == 0) {
            source
        } else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }

    val cropSource = preview
    if (cropping && cropSource != null) {
        CropScreen(
            bitmap = cropSource,
            onCancel = { cropping = false },
            onCropped = { cropped ->
                bitmap = cropped
                rotation = 0
                cropping = false
            }
        )
        return
    }

    fun reset() {        brightness = 0f
        contrast = 1f
        saturation = 1f
        rotation = 0
    }

    fun save() {
        val source = bitmap ?: return
        saving = true
        error = null
        scope.launch {
            val adjusted = withContext(Dispatchers.Default) {
                renderAdjusted(source, brightness, contrast, saturation, rotation, temperature)
            }
            val result = saveEditedToGallery(context, adjusted)
            saving = false
            result
                .onSuccess { onSaved(it) }
                .onFailure { error = it.message ?: "No se pudo guardar la copia" }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onClose,
                    size = 46.dp
                )
                Column(Modifier.weight(1f)) {
                    Text("Editar", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = media.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = { save() },
                    enabled = bitmap != null && !saving,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar copia")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    preview != null -> {
                        val filter = remember(brightness, contrast, saturation, temperature) {
                            ColorMatrixColorFilter(
                                adjustColorMatrix(brightness, contrast, saturation, temperature)
                            )
                        }
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            update = { view ->
                                view.setImageBitmap(preview)
                                view.colorFilter = filter
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    error != null -> Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )

                    else -> CircularProgressIndicator()
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SliderRow(
                        label = "Brillo",
                        value = brightness,
                        range = -100f..100f,
                        onChange = { brightness = it }
                    )
                    SliderRow(
                        label = "Contraste",
                        value = contrast,
                        range = 0.6f..1.8f,
                        onChange = { contrast = it }
                    )
                    SliderRow(
                        label = "Saturación",
                        value = saturation,
                        range = 0f..1.8f,
                        onChange = { saturation = it }
                    )
                    SliderRow(
                        label = "Temperatura",
                        value = temperature,
                        range = -100f..100f,
                        onChange = { temperature = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { rotation = (rotation + 90) % 360 }) {
                            Icon(
                                Icons.Filled.RotateRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("  Girar")
                        }
                        TextButton(onClick = { cropping = true }, enabled = preview != null) {
                            Icon(
                                Icons.Filled.Crop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("  Recortar")
                        }
                        TextButton(onClick = { reset() }) {
                            Icon(
                                Icons.Filled.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("  Restablecer")
                        }
                    }
                }
            }

            Text(
                text = "Se guarda una copia nueva en la galería (DCIM/Camera). " +
                    "El original del servidor no se toca y la copia aparece en la app al terminar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.size(width = 82.dp, height = 20.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
    }
}
