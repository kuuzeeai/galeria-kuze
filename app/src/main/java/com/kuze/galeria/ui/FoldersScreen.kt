package com.kuze.galeria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.kuze.galeria.data.Media


data class Album(val folder: String, val items: List<Media>) {
    val name: String get() = if (folder.isBlank()) "Raíz" else folder
    val cover: Media? get() = items.firstOrNull { !it.isVideo } ?: items.firstOrNull()
}

/** Carpetas del servidor como álbumes, en tarjetas redondeadas. */
@Composable
fun FoldersScreen(
    items: List<Media>,
    loader: ImageLoader,
    thumbUrlsFor: (Media) -> List<String>,
    virtualFolders: Map<String, Set<String>>,
    onOpenFolder: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onMoveToFolder: (Media, String) -> Unit
) {
    val albums = remember(items) {
        items.groupBy { it.folder }
            .map { (folder, list) -> Album(folder, list.sortedByDescending { it.modified }) }
            .sortedByDescending { it.items.firstOrNull()?.modified ?: 0L }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Carpetas reales del servidor + carpetas creadas solo en la app.
        val virtual = virtualFolders.entries.toList()
        val all = albums + virtual.map { Album(it.key, mediaInFolder(it.key)) }

        items(all, key = { it.folder }) { album ->
            AlbumCard(album, loader, thumbUrlsFor) { onOpenFolder(album.folder) }
        }
    }
}

private fun mediaInFolder(folderName: String): List<Media> {
    return items.filter { it.path in (virtualFolders[folderName] ?: emptySet()) }
}

@Composable
private fun AlbumCard(
    album: Album,
    loader: ImageLoader,
    thumbUrlsFor: (Media) -> List<String>,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                val cover = album.cover
                when {
                    cover == null -> Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    cover.isVideo -> Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    else -> ThumbImage(
                        media = cover,
                        loader = loader,
                        thumbUrls = thumbUrlsFor(cover),
                        sizePx = 240,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.items.size} ${if (album.items.size == 1) "elemento" else "elementos"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
