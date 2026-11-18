package com.kuze.galeria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

data class Album(val folder: String, val items: List<Media>) {
    val name: String get() = if (folder.isBlank()) "Raíz" else folder
    val cover: Media? get() = items.firstOrNull { !it.isVideo } ?: items.firstOrNull()
}

/** Lista de carpetas del servidor como álbumes. */
@Composable
fun FoldersScreen(
    items: List<Media>,
    loader: ImageLoader,
    onOpenFolder: (String) -> Unit
) {
    val albums = remember(items) {
        items.groupBy { it.folder }
            .map { (folder, list) -> Album(folder, list.sortedByDescending { it.modified }) }
            .sortedByDescending { it.items.firstOrNull()?.modified ?: 0L }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums, key = { it.folder }) { album ->
            AlbumRow(album, loader) { onOpenFolder(album.folder) }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun AlbumRow(album: Album, loader: ImageLoader, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val cover = album.cover
            if (cover == null || cover.isVideo) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cover.url)
                        .size(200)
                        .crossfade(true)
                        .build(),
                    imageLoader = loader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(album.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = "${album.items.size} ${if (album.items.size == 1) "elemento" else "elementos"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
