package com.kuze.galeria.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuze.galeria.data.Media
import com.kuze.galeria.data.ServerApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FavoriteColor = Color(0xFFFF6B81)

/**
 * Visor a pantalla completa: zoom en fotos, videos en streaming y una barra
 * inferior con compartir, editar, favorito, eliminar y más opciones.
 * Los botones usan el estilo Expressive de la app (redondos, colores suaves).
 */
@Composable
fun ViewerScreen(
    items: List<Media>,
    startIndex: Int,
    loader: ImageLoader,
    api: ServerApi,
    isFavorite: (Media) -> Boolean,
    onToggleFavorite: (Media) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    onDownload: (Media) -> Unit,
    onShare: (Media) -> Unit,
    onCreateFolder: (String) -> Unit,
    onMoveToFolder: (Media, String) -> Unit,
    virtualFolders: Map<String, Set<String>>
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size }
    )
    var zoomed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Media?>(null) }
    var details by remember { mutableStateOf<Media?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var creatingFolder by remember { mutableStateOf(false) }
    var movingToFolder by remember { mutableStateOf<String?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showMoveDialog by remember { mutableStateOf(false) }
    var folderNames by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    val current: Media? = remember(pagerState.currentPage, items) {
        items.getOrNull(pagerState.currentPage)
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3600)
            message = null
        }
    }

    LaunchedEffect(virtualFolders) {
        folderNames = virtualFolders.keys.toList()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = items[page]
            if (media.isVideo) {
                VideoPage(media = media, api = api)
            } else {
                ZoomableImage(media = media, loader = loader) { isZoomed -> zoomed = isZoomed }
            }
        }

        TopBar(
            title = items.getOrNull(pagerState.currentPage)?.name.orEmpty(),
            position = "${pagerState.currentPage + 1}/${items.size}",
            onClose = onClose,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .fillMaxWidth()
        )

        if (current != null) {
            BottomActionBar(
                media = current,
                favorite = isFavorite(current),
                busy = downloading,
                onShare = { onShare(current) },
                onDownload = {
                    if (!downloading) {
                        downloading = true
                        scope.launch {
                            downloadMediaToGallery(context, api, current)
                                .onSuccess { name ->
                                    message = "Guardado en Descargas/Galería: $name"
                                }
                                .onFailure {
                                    message = "No se pudo descargar: ${it.message}"
                                }
                            downloading = false
                        }
                    }
                },
                onEdit = { editing = current },
                onFavorite = { onToggleFavorite(current) },
                onDelete = { confirmingDelete = true },
                onDetails = { details = current },
                onCopyLink = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Enlace", current.url))
                    message = "Enlace copiado"
                },
                onRefresh = {
                    onRefresh()
                    message = "Actualizando la galería…"
                },
                onCreateFolder = { showNewFolder = true },
                onRequestMove = { showMoveDialog = true },
                hasFolders = folderNames.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp)
            )
        }

        val notice = message
        if (notice != null) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }

    BackHandler(onBack = onClose)

    val detailsTarget = details
    if (detailsTarget != null) {
        DetailsDialog(media = detailsTarget, onDismiss = { details = null })
    }

    if (confirmingDelete) {
        ExpressiveSheet(onDismiss = { confirmingDelete = false }) {
            Text(
                text = "Eliminar",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp)
            )
            Text(
                text = "Este botón todavía no está activado. Cuando lo activemos podrás borrar " +
                    "la foto del servidor, con confirmación previa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            TextButton(
                onClick = { confirmingDelete = false },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Entendido") }
        }
    }

    val editTarget = editing
    if (editTarget != null) {
        EditorScreen(
            media = editTarget,
            loader = loader,
            onClose = { editing = null },
            onSaved = { name ->
                editing = null
                message = "Copia guardada en DCIM/Camera: $name"
            }
        )
    }

    // Hoja: crear carpeta
    if (showNewFolder) {
        ExpressiveSheet(onDismiss = { showNewFolder = false }) {
            Text(
                text = "Crear carpeta",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 10.dp)
            )
            androidx.compose.material3.OutlinedTextField(
                value = newFolderName,
                onValueChange = { newFolderName = it },
                label = { Text("Nombre de la carpeta") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = { showNewFolder = false },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }
                androidx.compose.material3.Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolder = false
                        }
                    },
                    enabled = newFolderName.isNotBlank(),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Crear") }
            }
        }
    }

    // Hoja: mover a carpeta
    if (showMoveDialog && current != null) {
        ExpressiveSheet(onDismiss = { showMoveDialog = false }) {
            Text(
                text = "Mover a carpeta",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp)
            )
            if (folderNames.isEmpty()) {
                Text(
                    text = "Todavía no hay carpetas creadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            folderNames.forEach { folder ->
                ExpressiveSheetItem(
                    icon = Icons.Filled.Folder,
                    label = folder,
                    onClick = {
                        onMoveToFolder(current, folder)
                        showMoveDialog = false
                        message = "Movido a '$folder'"
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(title: String, position: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.32f),
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        shadowElevation = 10.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp, end = 12.dp)
            )
            Text(
                text = position,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

/**
 * Barra flotante tipo "liquid glass": semitransparente, con borde claro y sombra,
 * para que se lea bien sobre cualquier foto. Orden: compartir, editar, favorito,
 * descargar, eliminar y más.
 */
@Composable
private fun BottomActionBar(
    media: Media,
    favorite: Boolean,
    busy: Boolean,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onCopyLink: () -> Unit,
    onRefresh: () -> Unit,
    onCreateFolder: () -> Unit,
    onRequestMove: () -> Unit,
    hasFolders: Boolean,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.Black.copy(alpha = 0.30f),
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
        shadowElevation = 14.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveAction(
                icon = Icons.Filled.Share,
                label = "Compartir",
                onClick = onShare
            )

            if (!media.isVideo) {
                ExpressiveAction(
                    icon = Icons.Filled.Edit,
                    label = "Editar",
                    onClick = onEdit
                )
            }

            ExpressiveAction(
                icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (favorite) "Favorito" else "Favorito",
                tint = if (favorite) FavoriteColor else Color.White,
                onClick = onFavorite
            )

            ExpressiveAction(
                icon = Icons.Filled.Download,
                label = "Descargar",
                enabled = !busy,
                onClick = onDownload
            )

            if (!media.isVideo) {
                ExpressiveAction(
                    icon = Icons.Filled.DeleteOutline,
                    label = "Eliminar",
                    tint = Color.White.copy(alpha = 0.45f),
                    onClick = onDelete
                )
            }

            ExpressiveAction(
                icon = Icons.Filled.MoreVert,
                label = "Más",
                onClick = { menu = true }
            )
        }
    }

    if (menu) {
        ExpressiveSheet(onDismiss = { menu = false }) {
            ExpressiveSheetItem(
                icon = Icons.Filled.Download,
                label = "Descargar imagen",
                enabled = !busy,
                onClick = {
                    menu = false
                    onDownload()
                }
            )
            ExpressiveSheetItem(
                icon = Icons.Filled.Info,
                label = "Detalles",
                onClick = {
                    menu = false
                    onDetails()
                }
            )
            ExpressiveSheetItem(
                icon = Icons.Filled.Link,
                label = "Copiar enlace",
                onClick = {
                    menu = false
                    onCopyLink()
                }
            )
            ExpressiveSheetItem(
                icon = Icons.Filled.Refresh,
                label = "Recargar galería",
                onClick = {
                    menu = false
                    onRefresh()
                }
            )
            ExpressiveSheetItem(
                icon = Icons.Filled.CreateNewFolder,
                label = "Crear carpeta",
                onClick = {
                    menu = false
                    onCreateFolder()
                }
            )
            ExpressiveSheetItem(
                icon = Icons.Filled.DriveFileMove,
                label = "Mover a carpeta",
                enabled = hasFolders,
                onClick = {
                    menu = false
                    if (hasFolders) onRequestMove()
                }
            )
        }
    }
}

@Composable
private fun ExpressiveAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(52.dp)
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else tint.copy(alpha = 0.4f),
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                )
            ),
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            maxLines = 1,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun DetailsDialog(media: Media, onDismiss: () -> Unit) {
    ExpressiveSheet(onDismiss = onDismiss) {
        Text(
            text = "Detalles",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(media.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Carpeta: ${media.folder.ifBlank { "Raíz" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Tipo: ${if (media.isVideo) "Video" else "Foto"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Fecha: ${formatDate(media.modified)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                media.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.padding(top = 10.dp)
        ) { Text("Cerrar") }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "sin fecha"
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es"))
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
private fun ZoomableImage(
    media: Media,
    loader: ImageLoader,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember(media.id) { mutableStateOf(1f) }
    var offset by remember(media.id) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) { onZoomChange(scale > 1.01f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(1f, 6f)
        scale = next
        offset = if (next <= 1f) Offset.Zero else offset + panChange
    }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.url)
                // Se decodifica a un tamaño razonable para poder hacer zoom sin
                // llenar la memoria; el archivo completo se descarta al salir.
                .size(2048)
                .crossfade(true)
                .build(),
            imageLoader = loader,
            contentDescription = media.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(media.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .transformable(transformState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
private fun VideoPage(media: Media, api: ServerApi) {
    val context = LocalContext.current
    val player = remember(media.id) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(api.client)))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(media.url))
                prepare()
                playWhenReady = false
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
