package com.kuze.galeria.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import com.kuze.galeria.ui.downloadMediaToGallery
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Estado de la app: lista de fotos, carga, errores y ajustes.
 */
class AppState(context: Context) {

    val context: Context = context.applicationContext
    val settings = SettingsStore(this.context)
    val api = ServerApi(settings)

    private val repository = MediaRepository(api)
    private val cache = IndexCache(this.context)
    private val refreshMutex = Mutex()

    private var started = false

    var items by mutableStateOf<List<Media>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var progress by mutableStateOf("")
        private set

    var lastSync by mutableStateOf(0L)
        private set

    var darkTheme by mutableStateOf(settings.darkTheme)
        private set

    var palette by mutableStateOf(settings.palette)
        private set

    var columns by mutableStateOf(settings.columns)
        private set

    var font by mutableStateOf(settings.font)
        private set

    var favorites by mutableStateOf(settings.favorites)
        private set

    var virtualFolders by mutableStateOf(settings.virtualFolders)
        private set

    fun isFavorite(media: Media): Boolean = media.path in favorites

    fun toggleFavorite(media: Media) {
        val next = if (media.path in favorites) {
            favorites - media.path
        } else {
            favorites + media.path
        }
        favorites = next
        settings.favorites = next
    }

    fun createVirtualFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed in virtualFolders.keys) return
        val next = virtualFolders.toMutableMap()
        next[trimmed] = emptySet()
        virtualFolders = next
        settings.virtualFolders = next
    }

    fun moveToVirtualFolder(media: Media, folderName: String) {
        val next = virtualFolders.toMutableMap { (k, v) -> k to v.toMutableSet() }
        // Se "mueve": sale de todas las carpetas virtuales y entra en la elegida.
        next.values.forEach { it.remove(media.path) }
        next[folderName] = (next[folderName] ?: emptySet()) + media.path
        virtualFolders = next
        settings.virtualFolders = next
    }

    fun removeFromVirtualFolders(media: Media) {
        val next = virtualFolders.toMutableMap { (k, v) -> k to v.toMutableSet() }
        var changed = false
        next.values.forEach { if (it.remove(media.path)) changed = true }
        if (changed) {
            virtualFolders = next
            settings.virtualFolders = next
        }
    }

    fun mediaInVirtualFolder(folderName: String): List<Media> {
        val paths = virtualFolders[folderName] ?: return emptyList()
        return items.filter { it.path in paths }
    }

    suspend fun downloadMedia(media: Media): Result<String> =
        downloadMediaToGallery(context, api, media)

    fun toggleTheme() {
        darkTheme = !darkTheme
        settings.darkTheme = darkTheme
    }

    fun choosePalette(id: String) {
        palette = id
        settings.palette = id
    }

    fun chooseColumns(value: Int) {
        columns = value.coerceIn(2, 4)
        settings.columns = columns
    }

    fun chooseFont(id: String) {
        font = id
        settings.font = id
    }

    /**
     * Borra una sola vez la caché vieja, donde quedaban guardadas las fotos
     * completas abiertas en el visor. Ahora el visor no escribe en el disco.
     */
    fun prepareStorage() {
        if (settings.cacheVersion >= CACHE_VERSION) return
        runCatching {
            File(context.cacheDir, "img").deleteRecursively()
        }
        settings.cacheVersion = CACHE_VERSION
    }

    /** Primer arranque: muestra lo guardado y actualiza si está viejo. */
    suspend fun loadInitial() {
        if (started) return
        started = true
        if (!settings.isConfigured) return

        val cached = cache.load(api.baseUrl)
        if (cached != null) {
            items = cached
            lastSync = settings.cachedAt
        }
        val stale = cached == null || System.currentTimeMillis() - settings.cachedAt > STALE_MS
        if (stale) refresh()
    }

    /** Vuelve a leer todo el servidor. */
    suspend fun refresh() {
        if (!settings.isConfigured) return
        if (!refreshMutex.tryLock()) return
        try {
            loading = true
            error = null
            progress = "Conectando…"

            val fresh = repository.loadAll { folders, files ->
                progress = "$folders carpetas · $files archivos"
            }

            items = fresh
            lastSync = System.currentTimeMillis()
            settings.cachedAt = lastSync
            cache.save(fresh, api.baseUrl)
        } catch (e: Exception) {
            error = friendlyError(e)
        } finally {
            loading = false
            progress = ""
            refreshMutex.unlock()
        }
    }

    private fun friendlyError(e: Throwable): String = when (e) {
        is UnauthorizedException -> "Usuario o clave incorrectos. Revísalos en Ajustes."
        is UnknownHostException, is ConnectException, is SocketTimeoutException ->
            "Conéctate al Wi-Fi de casa"
        is IOException -> "Conéctate al Wi-Fi de casa"
        else -> e.message?.takeIf { it.isNotBlank() } ?: "No se pudo conectar con el servidor"
    }

    private companion object {
        const val STALE_MS = 10 * 60 * 1000L
        const val CACHE_VERSION = 2
    }
}
