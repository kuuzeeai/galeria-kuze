package com.kuze.galeria.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Cliente de miniserve: autenticación básica + lectura de las listas de carpetas.
 * El parser está ajustado a miniserve 0.35 (filas tr.entry-type-file / tr.entry-type-directory).
 */
class ServerApi(private val settings: SettingsStore) {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", Credentials.basic(settings.user, settings.pass))
                .build()
            chain.proceed(request)
        }
        .build()

    val baseUrl: String
        get() {
            val raw = settings.baseUrl.trim().ifEmpty { DEFAULT_URL }
            return if (raw.contains("://")) raw.trimEnd('/') else "http://${raw.trimEnd('/')}"
        }

    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "avif")
    private val videoExt = setOf("mp4", "mkv", "mov", "3gp", "3gpp", "webm", "m4v", "avi")

    private fun urlFor(path: String): HttpUrl? =
        baseUrl.toHttpUrlOrNull()?.resolve(path.ifBlank { "/" })

    /**
     * URL de la miniatura de un archivo: mismo servidor, puerto 8081,
     * misma carpeta y mismo nombre con extensión .jpg.
     */
    fun thumbUrlFor(media: Media): String? {
        val base = baseUrl.toHttpUrlOrNull() ?: return null
        val thumbBase = base.newBuilder().port(THUMB_PORT).build()
        val builder = thumbBase.newBuilder()
        media.folder.split('/').filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        builder.addPathSegment(media.name.substringBeforeLast('.', media.name) + ".jpg")
        return builder.build().toString()
    }

    /** Lee una carpeta y devuelve sus subcarpetas y sus archivos. */
    suspend fun list(path: String): DirListing = withContext(Dispatchers.IO) {
        val target = urlFor(path) ?: throw IllegalArgumentException("La dirección del servidor no es válida")
        val request = Request.Builder()
            .url(target)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) throw UnauthorizedException()
            if (response.code == 403) throw IllegalStateException("El servidor no permite entrar (403)")
            if (!response.isSuccessful) throw IllegalStateException("El servidor respondió ${response.code}")

            val body = response.body?.string().orEmpty()
            val doc = Jsoup.parse(body, target.toString())

            val folders = mutableListOf<FolderRef>()
            for (link in doc.select("a.directory[href]")) {
                val href = link.attr("href")
                if (!href.startsWith("/")) continue
                val name = link.text().trim().trimEnd('/')
                if (name.isEmpty()) continue
                folders += FolderRef(name = name, path = href)
            }

            val files = mutableListOf<Media>()
            for (row in doc.select("tr.entry-type-file")) {
                val link = row.selectFirst("a.file[href]") ?: continue
                val href = link.attr("href")
                if (!href.startsWith("/")) continue

                val name = link.text().trim()
                if (name.isEmpty()) continue

                val ext = name.substringAfterLast('.', "").lowercase()
                val isVideo = ext in videoExt
                if (!isVideo && ext !in imageExt) continue

                val resolved = urlFor(href) ?: continue
                val dateText = row.selectFirst("td.date-cell span")?.text().orEmpty()
                val modified = parseServerDate(dateText) ?: dateFromName(name) ?: 0L

                files += Media(
                    name = name,
                    folder = resolved.pathSegments.dropLast(1).joinToString("/"),
                    path = href,
                    url = resolved.toString(),
                    isVideo = isVideo,
                    modified = modified
                )
            }

            DirListing(folders = folders, files = files)
        }
    }

    private fun parseServerDate(raw: String): Long? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        for (pattern in offsetPatterns) {
            val value = runCatching {
                OffsetDateTime.parse(text, pattern).toInstant().toEpochMilli()
            }.getOrNull()
            if (value != null) return value
        }
        return runCatching {
            LocalDateTime.parse(text.replace(' ', 'T'))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    /** Si la lista no trae fecha, se saca del nombre: IMG_20261118_042827, Screenshot_2026-11-18-04-28-27... */
    private fun dateFromName(name: String): Long? {
        val match = nameDate.find(name) ?: return null
        val (year, month, day, hour, minute, second) = match.destructured
        return runCatching {
            LocalDateTime.of(
                year.toInt(), month.toInt(), day.toInt(),
                hour.toInt(), minute.toInt(), second.toInt()
            ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }

    private companion object {
        const val THUMB_PORT = 8081
        val offsetPatterns = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS xxx")
        )
        val nameDate = Regex("(20\\d{2})(\\d{2})(\\d{2})[^0-9]?(\\d{2})(\\d{2})(\\d{2})")
    }
}
