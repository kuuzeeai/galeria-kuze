package com.kuze.galeria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/** Cuadrícula de fotos agrupada por día. */
@Composable
fun PhotosScreen(
    items: List<Media>,
    loader: ImageLoader,
    onOpen: (index: Int) -> Unit
) {
    val groups = remember(items) { groupByDay(items) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        groups.forEach { group ->
            item(
                key = "header-${group.date?.toString() ?: "sin-fecha"}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 6.dp)) {
                    Text(
                        text = dayLabel(group.date),
                        style = MaterialTheme.typography.titleSmall
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
                    onClick = { onOpen(entry.index) }
                )
            }
        }
    }
}

@Composable
fun ThumbCell(media: Media, loader: ImageLoader, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (media.isVideo) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1B2226)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier.size(34.dp),
                    tint = Color.White
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(media.url)
                    .size(320)
                    .crossfade(true)
                    .build(),
                imageLoader = loader,
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Cabecera de un mes con su conteo, usada en álbumes. */
@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
        else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es")))
    }
}
