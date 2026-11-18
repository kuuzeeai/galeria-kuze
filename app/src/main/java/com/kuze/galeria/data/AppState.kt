package com.kuze.galeria.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import java.io.IOException
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

    private var started = false

    fun toggleTheme() {
        darkTheme = !darkTheme
        settings.darkTheme = darkTheme
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
    }
}
