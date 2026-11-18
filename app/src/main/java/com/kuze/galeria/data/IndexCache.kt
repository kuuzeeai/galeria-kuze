package com.kuze.galeria.data

import android.content.Context
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Guarda en el teléfono la última lista leída del servidor,
 * para abrir la app rápido y sin conexión.
 */
class IndexCache(context: Context) {

    private val file = File(context.filesDir, "media-index.tsv")

    @Synchronized
    fun save(items: List<Media>, baseUrl: String) {
        runCatching {
            val text = StringBuilder()
            text.append('#').append(encode(baseUrl)).append('\n')
            for (item in items) {
                text.append(encode(item.name)).append('\t')
                    .append(encode(item.folder)).append('\t')
                    .append(encode(item.path)).append('\t')
                    .append(encode(item.url)).append('\t')
                    .append(if (item.isVideo) '1' else '0').append('\t')
                    .append(item.modified).append('\n')
            }
            file.writeText(text.toString())
        }
    }

    @Synchronized
    fun load(baseUrl: String): List<Media>? = runCatching {
        if (!file.exists()) return null
        val lines = file.readLines()
        if (lines.size <= 1) return null
        if (lines.first().removePrefix("#") != encode(baseUrl)) return null

        lines.drop(1).mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 6) return@mapNotNull null
            Media(
                name = decode(parts[0]),
                folder = decode(parts[1]),
                path = decode(parts[2]),
                url = decode(parts[3]),
                isVideo = parts[4] == "1",
                modified = parts[5].toLongOrNull() ?: 0L
            )
        }
    }.getOrNull()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}
