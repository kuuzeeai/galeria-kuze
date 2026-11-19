package com.kuze.galeria.ui

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Cargador de miniaturas: usa el mismo cliente con usuario y clave del servidor,
 * guarda hasta 100 MB de miniaturas en el teléfono y tira lo más viejo automáticamente.
 */
fun buildImageLoader(context: Context, client: OkHttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .okHttpClient(client)
        .respectCacheHeaders(false)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.10f)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(context.cacheDir, "img"))
                .maxSizeBytes(100L * 1024L * 1024L)
                .build()
        }
        .crossfade(true)
        .build()

/**
 * Cargador para la foto completa del visor: **no escribe nada en el almacenamiento**.
 * La foto se baja a la memoria, se muestra y se olvida al cerrar el visor; la caché
 * en disco queda reservada solo para las miniaturas.
 */
fun buildViewerLoader(context: Context, client: OkHttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .okHttpClient(client)
        .respectCacheHeaders(false)
        .diskCache(null)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.12f)
                .build()
        }
        .crossfade(true)
        .build()
