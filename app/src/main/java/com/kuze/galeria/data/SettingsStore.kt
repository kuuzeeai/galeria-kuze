package com.kuze.galeria.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda la URL, el usuario y la clave.
 * Se usa EncryptedSharedPreferences: los datos quedan cifrados en el teléfono.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "galeria_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        // Si el almacén cifrado falla en algún teléfono, no se cae la app.
        context.getSharedPreferences("galeria_secure", Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_URL, value.trim()).apply()

    var user: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER, value.trim()).apply()

    var pass: String
        get() = prefs.getString(KEY_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASS, value).apply()

    /** Cuándo se actualizó la lista por última vez. */
    var cachedAt: Long
        get() = prefs.getLong(KEY_CACHED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_CACHED_AT, value).apply()

    /** Tema oscuro activado. */
    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    /** Paleta de colores elegida: menta, oceano, atardecer, lavanda, bosque o arena. */
    var palette: String
        get() = prefs.getString(KEY_PALETTE, "menta") ?: "menta"
        set(value) = prefs.edit().putString(KEY_PALETTE, value).apply()

    /** Fotos por fila en la cuadrícula: 2, 3 o 4. */
    var columns: Int
        get() = prefs.getInt(KEY_COLUMNS, 4).coerceIn(2, 4)
        set(value) = prefs.edit().putInt(KEY_COLUMNS, value.coerceIn(2, 4)).apply()

    /** Fuente de la app: sans, serif, mono, cursive o grande. */
    var font: String
        get() = prefs.getString(KEY_FONT, "sans") ?: "sans"
        set(value) = prefs.edit().putString(KEY_FONT, value).apply()

    /** Rutas del servidor marcadas como favoritas. */
    var favorites: Set<String>
        get() = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAVORITES, value).apply()

    /** Rutas ocultas: se ocultan de la galería pero el archivo queda en el servidor. */
    var hiddenPaths: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_HIDDEN, value).apply()

    /** Qué mostrar: "ambos" (servidor + local), "solo_servidor" o "solo_local". */
    var localMode: String
        get() = prefs.getString(KEY_LOCAL_MODE, "ambos") ?: "ambos"
        set(value) = prefs.edit().putString(KEY_LOCAL_MODE, value).apply()

    /** Carpetas creadas solo en la app: nombre -> rutas de servidor que contiene. */
    var virtualFolders: Map<String, Set<String>>
        get() {
            val raw = prefs.getString(KEY_VIRTUAL_FOLDERS, "") ?: ""
            if (raw.isBlank()) return emptyMap()
            return raw.split("|").filter { it.isNotBlank() }.associate { part ->
                val (name, paths) = part.split("=", limit = 2)
                name to paths.split(",").filter { it.isNotBlank() }.toSet()
            }
        }
        set(value) {
            val raw = value.entries.joinToString("|") { "${it.key}=${it.value.joinToString(",")}" }
            prefs.edit().putString(KEY_VIRTUAL_FOLDERS, raw).apply()
        }

    /** Versión de la limpieza de caché ya aplicada. */
    var cacheVersion: Int
        get() = prefs.getInt(KEY_CACHE_VERSION, 1)
        set(value) = prefs.edit().putInt(KEY_CACHE_VERSION, value).apply()

    val isConfigured: Boolean
        get() = user.isNotBlank() && pass.isNotBlank() && baseUrl.isNotBlank()

    /** Exporta la configuración a un archivo de texto en la carpeta Descargas. */
    fun exportConfig(file: File): Result<String> = runCatching {
        val json = JSONObject().apply {
            put("baseUrl", baseUrl)
            put("user", user)
            put("darkTheme", darkTheme)
            put("palette", palette)
            put("columns", columns)
            put("font", font)
            put("localMode", localMode)
            put("favorites", favorites.toList())
            put("virtualFolders", JSONObject(virtualFolders.mapValues { it.value.toList() }))
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2))
        file.absolutePath
    }

    /** Importa la configuración desde un archivo de texto. */
    fun importConfig(file: File): Result<String> = runCatching {
        val json = JSONObject(file.readText())
        baseUrl = json.getString("baseUrl")
        user = json.getString("user")
        darkTheme = json.optBoolean("darkTheme", false)
        palette = json.getString("palette")
        columns = json.optInt("columns", 4)
        font = json.getString("font")
        localMode = json.getString("localMode")
        val fav = json.optJSONArray("favorites")
        favorites = if (fav != null) (0 until fav.length()).map { fav.getString(it) }.toSet() else emptySet()
        val folders = json.optJSONObject("virtualFolders")
        virtualFolders = if (folders != null) {
            folders.keys().asSequence().associate { key ->
                val arr = folders.optJSONArray(key)
                key to (if (arr != null) (0 until arr.length()).map { arr.getString(it) }.toSet() else emptySet())
            }
        } else emptyMap()
        "Configuración importada"
    }

    private companion object {
        const val KEY_URL = "url"
        const val KEY_USER = "user"
        const val KEY_PASS = "pass"
        const val KEY_CACHED_AT = "cached_at"
        const val KEY_DARK = "dark_theme"
        const val KEY_PALETTE = "palette"
        const val KEY_COLUMNS = "columns"
        const val KEY_FONT = "font"
        const val KEY_FAVORITES = "favorites"
        const val KEY_LOCAL_MODE = "local_mode"
        const val KEY_VIRTUAL_FOLDERS = "virtual_folders"
        const val KEY_CACHE_VERSION = "cache_version"
        const val KEY_HIDDEN = "hidden_paths"
    }
}
