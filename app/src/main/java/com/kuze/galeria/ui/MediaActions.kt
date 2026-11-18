package com.kuze.galeria.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.kuze.galeria.data.Media
import com.kuze.galeria.data.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SHARE_DIR = "compartir"

/** Carpeta de la galería donde se guarda la copia editada. */
const val GALLERY_FOLDER = "DCIM/Camera"

/** Descarga el archivo a una carpeta temporal y abre el menú de compartir de Android. */
suspend fun shareMedia(context: Context, api: ServerApi, media: Media): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }

            // Las copias compartidas se borran solas después de 10 minutos.
            val limit = System.currentTimeMillis() - 10 * 60 * 1000L
            dir.listFiles()?.forEach { if (it.lastModified() < limit) it.delete() }

            val safeName = media.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, safeName)

            api.client.newCall(Request.Builder().url(media.url).build()).execute().use { response ->
                if (!response.isSuccessful) error("El servidor respondió ${response.code}")
                val body = response.body ?: error("Respuesta vacía")
                file.outputStream().use { out -> body.byteStream().copyTo(out) }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (media.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir"))
        }
    }

/**
 * Aplica brillo, contraste, saturación y rotación a la foto.
 * Devuelve un bitmap nuevo; el original no se toca.
 */
fun adjustColorMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
    val matrix = ColorMatrix().apply { setSaturation(saturation) }
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return matrix
}

fun renderAdjusted(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Int
): Bitmap {
    val matrix = adjustColorMatrix(brightness, contrast, saturation)

    val width = source.width
    val height = source.height
    val adjusted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(adjusted)
    canvas.drawBitmap(
        source,
        0f,
        0f,
        Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
    )

    if (rotation % 360 == 0) return adjusted

    val turn = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(adjusted, 0, 0, width, height, turn, true)
}

/**
 * Guarda una copia nueva en la galería del teléfono (DCIM/Camera).
 * El archivo original del servidor nunca se modifica.
 */
suspend fun saveEditedToGallery(context: Context, bitmap: Bitmap): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            val name = "KUZE_editada_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, GALLERY_FOLDER)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear el archivo en la galería")

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            } ?: error("No se pudo escribir la foto")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            name
        }
    }
