package com.kuze.galeria.data

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
        }
}
