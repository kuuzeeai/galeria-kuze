package com.kuze.galeria.ui

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Cargador de miniaturas: usa el mismo cliente con usuario y clave del servidor,
 * y guarda hasta 500 MB de miniaturas en el teléfono.
 */
fun buildImageLoader(context: Context, client: OkHttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .okHttpClient(client)
        .respectCacheHeaders(false)
        .diskCache {
            DiskCache.Builder()
                .directory(File(context.cacheDir, "img"))
                .maxSizeBytes(500L * 1024L * 1024L)
                .build()
        }
        .crossfade(true)
        .build()
