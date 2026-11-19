package com.kuze.galeria.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Recorte dentro de la app, con el mismo estilo Material 3 Expressive.
 * El marco se puede arrastrar y estirar desde las esquinas.
 */
@Composable
fun CropScreen(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    val handleRadius = with(density) { 34.dp.toPx() }
    val minSize = with(density) { 56.dp.toPx() }

    var container by remember { mutableStateOf(IntSize.Zero) }
    val imageRect = remember(container, bitmap) {
        fitRect(bitmap.width, bitmap.height, container)
    }
    var crop by remember { mutableStateOf(Rect.Zero) }
    var mode by remember { mutableStateOf(CropMode.None) }

    LaunchedEffect(imageRect) {
        if (imageRect.width > 0f && imageRect.height > 0f) {
            val dx = imageRect.width * 0.08f
            val dy = imageRect.height * 0.08f
            crop = Rect(
                imageRect.left + dx,
                imageRect.top + dy,
                imageRect.right - dx,
                imageRect.bottom - dy
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    container = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    onClick = onCancel,
                    size = 46.dp
                )
                Text(
                    text = "Recortar",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { container = it }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageRect, crop, handleRadius) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    mode = hitTest(start, crop, imageRect, handleRadius)
                                },
                                onDragEnd = { mode = CropMode.None },
                                onDragCancel = { mode = CropMode.None },
                                onDrag = { change, drag ->
                                    change.consume()
                                    if (imageRect.width <= 0f) return@detectDragGestures
                                    crop = applyDrag(
                                        crop = crop,
                                        bounds = imageRect,
                                        mode = mode,
                                        drag = drag,
                                        minSize = minSize
                                    )
                                }
                            )
                        }
                ) {
                    if (crop.width <= 0f) return@Canvas

                    val scrim = Color.Black.copy(alpha = 0.55f)
                    // Oscurece todo menos el marco.
                    drawRect(scrim, size = androidx.compose.ui.geometry.Size(size.width, crop.top))
                    drawRect(
                        scrim,
                        topLeft = Offset(0f, crop.bottom),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height - crop.bottom)
                    )
                    drawRect(
                        scrim,
                        topLeft = Offset(0f, crop.top),
                        size = androidx.compose.ui.geometry.Size(crop.left, crop.height)
                    )
                    drawRect(
                        scrim,
                        topLeft = Offset(crop.right, crop.top),
                        size = androidx.compose.ui.geometry.Size(size.width - crop.right, crop.height)
                    )

                    drawRect(
                        color = Color.White,
                        topLeft = crop.topLeft,
                        size = crop.size,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Cuadrícula de tercios.
                    val grid = Color.White.copy(alpha = 0.45f)
                    for (i in 1..2) {
                        val x = crop.left + crop.width * i / 3f
                        drawLine(grid, Offset(x, crop.top), Offset(x, crop.bottom), 1.dp.toPx())
                        val y = crop.top + crop.height * i / 3f
                        drawLine(grid, Offset(crop.left, y), Offset(crop.right, y), 1.dp.toPx())
                    }

                    // Esquinas gruesas para agarrar.
                    val arm = 24.dp.toPx()
                    val thick = 4.dp.toPx()
                    val corners = listOf(
                        crop.topLeft to Offset(1f, 1f),
                        Offset(crop.right, crop.top) to Offset(-1f, 1f),
                        Offset(crop.left, crop.bottom) to Offset(1f, -1f),
                        crop.bottomRight to Offset(-1f, -1f)
                    )
                    corners.forEach { (point, dir) ->
                        drawLine(
                            Color.White,
                            point,
                            Offset(point.x + arm * dir.x, point.y),
                            thick
                        )
                        drawLine(
                            Color.White,
                            point,
                            Offset(point.x, point.y + arm * dir.y),
                            thick
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancelar", color = Color.White)
                }
                Button(
                    onClick = {
                        val result = cropToBitmap(bitmap, crop, imageRect)
                        if (result != null) onCropped(result)
                    },
                    enabled = crop.width > 0f,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Recortar")
                }
            }
        }
    }
}

private enum class CropMode { None, Move, TopLeft, TopRight, BottomLeft, BottomRight }

private fun fitRect(bitmapWidth: Int, bitmapHeight: Int, container: IntSize): Rect {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || container.width <= 0 || container.height <= 0) {
        return Rect.Zero
    }
    val scale = min(
        container.width.toFloat() / bitmapWidth,
        container.height.toFloat() / bitmapHeight
    )
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val left = (container.width - width) / 2f
    val top = (container.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun hitTest(
    point: Offset,
    crop: Rect,
    bounds: Rect,
    handleRadius: Float
): CropMode {
    if (crop.width <= 0f) return CropMode.None
    val near = { corner: Offset ->
        abs(point.x - corner.x) <= handleRadius && abs(point.y - corner.y) <= handleRadius
    }
    return when {
        near(crop.topLeft) -> CropMode.TopLeft
        near(Offset(crop.right, crop.top)) -> CropMode.TopRight
        near(Offset(crop.left, crop.bottom)) -> CropMode.BottomLeft
        near(crop.bottomRight) -> CropMode.BottomRight
        crop.contains(point) -> CropMode.Move
        else -> CropMode.None
    }
}

private fun applyDrag(
    crop: Rect,
    bounds: Rect,
    mode: CropMode,
    drag: Offset,
    minSize: Float
): Rect {
    if (mode == CropMode.None) return crop

    fun clampX(value: Float) = value.coerceIn(bounds.left, bounds.right)
    fun clampY(value: Float) = value.coerceIn(bounds.top, bounds.bottom)

    return when (mode) {
        CropMode.Move -> {
            val dx = drag.x.coerceIn(bounds.left - crop.left, bounds.right - crop.right)
            val dy = drag.y.coerceIn(bounds.top - crop.top, bounds.bottom - crop.bottom)
            crop.translate(dx, dy)
        }

        CropMode.TopLeft -> {
            val newLeft = clampX(crop.left + drag.x).coerceAtMost(crop.right - minSize)
            val newTop = clampY(crop.top + drag.y).coerceAtMost(crop.bottom - minSize)
            Rect(newLeft, newTop, crop.right, crop.bottom)
        }

        CropMode.TopRight -> {
            val newRight = clampX(crop.right + drag.x).coerceAtLeast(crop.left + minSize)
            val newTop = clampY(crop.top + drag.y).coerceAtMost(crop.bottom - minSize)
            Rect(crop.left, newTop, newRight, crop.bottom)
        }

        CropMode.BottomLeft -> {
            val newLeft = clampX(crop.left + drag.x).coerceAtMost(crop.right - minSize)
            val newBottom = clampY(crop.bottom + drag.y).coerceAtLeast(crop.top + minSize)
            Rect(newLeft, crop.top, crop.right, newBottom)
        }

        CropMode.BottomRight -> {
            val newRight = clampX(crop.right + drag.x).coerceAtLeast(crop.left + minSize)
            val newBottom = clampY(crop.bottom + drag.y).coerceAtLeast(crop.top + minSize)
            Rect(crop.left, crop.top, newRight, newBottom)
        }

        CropMode.None -> crop
    }
}

private fun cropToBitmap(source: Bitmap, crop: Rect, imageRect: Rect): Bitmap? {
    if (imageRect.width <= 0f || crop.width <= 0f) return null
    val scale = source.width / imageRect.width
    val left = ((crop.left - imageRect.left) * scale).roundToInt().coerceIn(0, source.width - 1)
    val top = ((crop.top - imageRect.top) * scale).roundToInt().coerceIn(0, source.height - 1)
    val width = (crop.width * scale).roundToInt().coerceIn(1, source.width - left)
    val height = (crop.height * scale).roundToInt().coerceIn(1, source.height - top)
    return runCatching { Bitmap.createBitmap(source, left, top, width, height) }.getOrNull()
}
