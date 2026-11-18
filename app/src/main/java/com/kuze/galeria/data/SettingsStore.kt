package com.kuze.galeria.data

import android.content.Context
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
        context.getSharedPreferences("galeria_fallback", Context.MODE_PRIVATE)
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

    val isConfigured: Boolean
        get() = user.isNotBlank() && pass.isNotBlank() && baseUrl.isNotBlank()

    private companion object {
        const val KEY_URL = "url"
        const val KEY_USER = "user"
        const val KEY_PASS = "pass"
        const val KEY_CACHED_AT = "cached_at"
    }
}
