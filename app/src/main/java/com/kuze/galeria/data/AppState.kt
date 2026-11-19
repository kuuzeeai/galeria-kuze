package com.kuze.galeria.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import kotlin.comparisons.compareByDescending
import com.kuze.galeria.ui.downloadMediaToGallery
import kotlinx.coroutines.Dispatchers
import okhttp3.Request
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

    /** Todo lo leído (servidor + local), sin filtrar. */
    var allItems by mutableStateOf<List<Media>>(emptyList())
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

    var localMode by mutableStateOf(settings.localMode)
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
        val next = virtualFolders.mapValues { it.value.toMutableSet() }.toMutableMap()
        // Se "mueve": sale de todas las carpetas virtuales y entra en la elegida.
        next.values.forEach { it.remove(media.path) }
        next[folderName] = ((next[folderName] ?: mutableSetOf()) + media.path).toMutableSet()
        virtualFolders = next
        settings.virtualFolders = next
    }

    suspend fun downloadMedia(media: Media): Result<String> =
        downloadMediaToGallery(context, api, media)

    /**
     * Intenta borrar el archivo físico del servidor con un DELETE HTTP.
     * miniserve 0.35 no hace DELETE, así que la mayoría de las veces falla.
     * Si falla, la foto se oculta igual de la galería (borrar para mí).
     */
    suspend fun deleteOnServer(media: Media): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val url = media.url ?: return@withContext false
        val request = okhttp3.Request.Builder().url(url).delete().build()
        runCatching {
            api.client.newCall(request).execute().use { it.isSuccessful }
        }.getOrNull() ?: false
    }

    /** Oculta una foto de la galería: se quita de la vista pero el archivo queda en el servidor. */
    fun hideMedia(media: Media) {
        val next = settings.hiddenPaths + media.path
        settings.hiddenPaths = next
    }

    /** Muestra de nuevo una foto que estaba oculta. */
    fun unhideMedia(media: Media) {
        val next = settings.hiddenPaths - media.path
        settings.hiddenPaths = next
    }

    fun isHidden(media: Media): Boolean = media.path in settings.hiddenPaths

    fun toggleTheme() {
        darkTheme = !darkTheme
        settings.darkTheme = darkTheme
    }

    fun choosePalette(id: String) {
        palette = id
        settings.palette = id
        // AMOLED siempre es oscuro.
        if (id == "amoled" && !darkTheme) {
            darkTheme = true
            settings.darkTheme = true
        }
    }

    fun chooseColumns(value: Int) {
        columns = value.coerceIn(2, 4)
        settings.columns = columns
    }

    fun chooseFont(id: String) {
        font = id
        settings.font = id
    }

    fun chooseLocalMode(mode: String) {
        localMode = mode
        settings.localMode = mode
        recomputeItems()
    }

    private fun recomputeItems() {
        items = when (localMode) {
            "solo_servidor" -> allItems.filter { !it.local }
            "solo_local" -> allItems.filter { it.local }
            else -> allItems
        }
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
            allItems = cached
            lastSync = settings.cachedAt
            recomputeItems()
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

            val localList = if (localMode != "solo_servidor") repository.loadLocal(context) else emptyList()
            allItems = (fresh + localList).distinctBy { it.path }
                .sortedWith(compareByDescending<Media> { it.modified }.thenBy { it.name })
            lastSync = System.currentTimeMillis()
            settings.cachedAt = lastSync
            cache.save(allItems, api.baseUrl)
            recomputeItems()
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
