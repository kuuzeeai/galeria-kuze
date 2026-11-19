package com.kuze.galeria.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContentUris
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recorre todas las carpetas del servidor una sola vez y devuelve la lista completa.
 * Nada se mueve ni se escribe en el servidor.
 */
class MediaRepository(private val api: ServerApi) {

    suspend fun loadAll(onProgress: (folders: Int, files: Int) -> Unit = { _, _ -> }): List<Media> =
        withContext(Dispatchers.IO) {
            val collected = mutableListOf<Media>()
            val visited = mutableSetOf<String>()
            var folderCount = 0

            suspend fun walk(path: String) {
                if (!visited.add(path)) return
                val listing = api.list(path)
                folderCount += 1
                collected += listing.files
                onProgress(folderCount, collected.size)
                for (child in listing.folders) {
                    if (child.path !in visited) walk(child.path)
                }
            }

            walk("/")

            collected.distinctBy { it.path }
                .sortedWith(compareByDescending<Media> { it.modified }.thenBy { it.name })

    /** Lee fotos y videos guardados en el teléfono (galería local). Devuelve vacío si no hay permiso. */
    suspend fun loadLocal(context: Context): List<Media> =
        withContext(Dispatchers.IO) {
            val collected = mutableListOf<Media>()
            try {
                val resolver = context.contentResolver
                val baseCols = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
                )
                val images = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    baseCols,
                    null, null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )
                collected += cursorToMedia(resolver, images, isVideo = false)
                val videos = resolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    baseCols,
                    null, null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )
                collected += cursorToMedia(resolver, videos, isVideo = true)
            } catch (e: Exception) {
                // Sin permiso de almacenamiento o no disponible: se ignora y se deja vacío.
            }
            collected
        }

    private fun cursorToMedia(
        resolver: ContentResolver,
        cursor: Cursor?,
        isVideo: Boolean
    ): List<Media> {
        cursor?.use { c ->
            val idxId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val idxName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val idxData = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val idxDate = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val idxBucket = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val list = mutableListOf<Media>()
            while (c.moveToNext()) {
                val id = c.getString(idxId)
                val name = c.getString(idxName)
                val data = c.getString(idxData)
                val modified = c.getLong(idxDate)
                val bucket = c.getString(idxBucket)
                val uri: Uri = if (isVideo) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toLong())
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong())
                }
                list += Media(
                    name = name.ifEmpty { "Sin nombre" },
                    folder = bucket.ifEmpty { "Local" },
                    path = "local://${if (isVideo) "video" else "image"}/$id",
                    url = uri.toString(),
                    isVideo = isVideo,
                    modified = modified,
                    local = true
                )
            }
            list
        } ?: emptyList()
    }
}
