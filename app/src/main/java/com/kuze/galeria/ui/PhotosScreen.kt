package com.kuze.galeria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuze.galeria.data.Media
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cuadrícula de fotos agrupada por día, con 2, 3 o 4 por fila. */
@Composable
fun PhotosScreen(
    items: List<Media>,
    loader: ImageLoader,
    columns: Int,
    thumbUrlsFor: (Media) -> List<String>,
    showHero: Boolean = false,
    lastSync: Long = 0L,
    onOpen: (index: Int) -> Unit
) {
    val groups = remember(items) { groupByDay(items) }
    val folderCount = remember(items) { items.map { it.folder }.distinct().size }
    val thumbWidth = remember(columns) { thumbWidthFor(columns) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.coerceIn(2, 4)),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (showHero) {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                HeroCard(
                    photoCount = items.size,
                    folderCount = folderCount,
                    updatedText = updatedLabel(lastSync)
                )
            }
        }

        groups.forEach { group ->
            item(
                key = "header-${group.date?.toString() ?: "sin-fecha"}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, top = 12.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = dayLabel(group.date),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${group.entries.size} ${if (group.entries.size == 1) "elemento" else "elementos"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(group.entries, key = { it.media.id }) { entry ->
                ThumbCell(
                    media = entry.media,
                    loader = loader,
                    thumbUrls = thumbUrlsFor(entry.media),
                    sizePx = thumbWidth,
                    onClick = { onOpen(entry.index) }
                )
            }
        }
    }
}

@Composable
fun ThumbCell(
    media: Media,
    loader: ImageLoader,
    thumbUrls: List<String>,
    sizePx: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (media.isVideo) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            ThumbImage(
                media = media,
                loader = loader,
                thumbUrls = thumbUrls,
                sizePx = sizePx,
                contentDescription = media.name,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Miniatura en línea: prueba las copias pequeñas del servidor (puerto 8081) y,
 * si ninguna existe, cae automáticamente a la foto original.
 */
@Composable
fun ThumbImage(
    media: Media,
    loader: ImageLoader,
    thumbUrls: List<String>,
    sizePx: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var attempt by remember(media.id, thumbUrls) { mutableIntStateOf(0) }
    val data = thumbUrls.getOrNull(attempt) ?: media.url

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(data)
            .size(sizePx)
            .crossfade(true)
            .build(),
        imageLoader = loader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onError = {
            if (attempt < thumbUrls.size) attempt += 1
        },
        modifier = modifier
    )
}

data class IndexedMedia(val index: Int, val media: Media)

data class DayGroup(val date: LocalDate?, val entries: List<IndexedMedia>)

fun groupByDay(items: List<Media>): List<DayGroup> {
    val map = LinkedHashMap<LocalDate?, MutableList<IndexedMedia>>()
    items.forEachIndexed { index, media ->
        val day = if (media.modified > 0L) {
            Instant.ofEpochMilli(media.modified).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            null
        }
        map.getOrPut(day) { mutableListOf() }.add(IndexedMedia(index, media))
    }
    return map.map { (date, entries) -> DayGroup(date, entries) }
}

fun dayLabel(date: LocalDate?): String {
    if (date == null) return "Sin fecha"
    val today = LocalDate.now()
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es")))
    }
}

fun updatedLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "sin actualizar"
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "actualizado ahora"
        diff < 3_600_000L -> "actualizado hace ${diff / 60_000L} min"
        diff < 86_400_000L -> "actualizado hace ${diff / 3_600_000L} h"
        else -> "actualizado hace ${diff / 86_400_000L} días"
    }
}
