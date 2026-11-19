package com.kuze.galeria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.ImageLoader
import com.kuze.galeria.data.AppState
import com.kuze.galeria.data.Media
import com.kuze.galeria.ui.ChipRow
import com.kuze.galeria.ui.ExpressiveBottomBar
import com.kuze.galeria.ui.ExpressiveHeader
import com.kuze.galeria.ui.ExpressiveMessage
import com.kuze.galeria.ui.FoldersScreen
import com.kuze.galeria.ui.GaleriaTheme
import com.kuze.galeria.ui.PhotosScreen
import com.kuze.galeria.ui.SettingsScreen
import com.kuze.galeria.ui.ViewerScreen
import com.kuze.galeria.ui.shareMedia
import com.kuze.galeria.ui.thumbWidthFor
import com.kuze.galeria.ui.buildImageLoader
import com.kuze.galeria.ui.buildViewerLoader
import kotlinx.coroutines.launch

private enum class Screen { Library, Settings }

private data class ViewerTarget(val items: List<Media>, val index: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent {
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !state.darkTheme
                controller.isAppearanceLightNavigationBars = !state.darkTheme
            }
            GaleriaTheme(
                darkTheme = state.darkTheme,
                palette = state.palette,
                font = state.font
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GaleriaApp(state)
                }
            }
        }
    }
}

@Composable
private fun GaleriaApp(state: AppState) {
    val scope = rememberCoroutineScope()
    val loader = remember { buildImageLoader(state.context, state.api.client) }
    val viewerLoader = remember { buildViewerLoader(state.context, state.api.client) }

    var screen by remember {
        mutableStateOf(if (state.settings.isConfigured) Screen.Library else Screen.Settings)
    }
    var viewer by remember { mutableStateOf<ViewerTarget?>(null) }
    var album by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(message) {
        if (message != null) {
            delay(3600)
            message = null
        }
    }

    // El botón y el gesto de atrás vuelven a la pantalla anterior, no cierran la app.
    BackHandler(enabled = viewer != null || album != null || screen == Screen.Settings) {
        when {
            viewer != null -> viewer = null
            album != null -> album = null
            else -> screen = Screen.Library
        }
    }

    LaunchedEffect(Unit) {
        state.prepareStorage()
        state.loadInitial()
    }

    when (screen) {
        Screen.Settings -> SettingsScreen(
            settings = state.settings,
            api = state.api,
            firstRun = !state.settings.isConfigured,
            palette = state.palette,
            columns = state.columns,
            font = state.font,
            localMode = state.localMode,
            itemCount = state.items.size,
            onPalette = { state.choosePalette(it) },
            onColumns = { state.chooseColumns(it) },
            onFont = { state.chooseFont(it) },
            onLocalMode = { state.chooseLocalMode(it) },
            onSaved = {
                screen = Screen.Library
                scope.launch { state.refresh() }
            },
            onBack = { screen = Screen.Library }
        )

        Screen.Library -> {
            val target = viewer
            if (target != null) {
                ViewerScreen(
                    items = target.items,
                    startIndex = target.index,
                    loader = viewerLoader,
                    api = state.api,
                    isFavorite = { state.isFavorite(it) },
                    onToggleFavorite = { state.toggleFavorite(it) },
                    onRefresh = { scope.launch { state.refresh() } },
                    onDownload = { scope.launch { state.downloadMedia(it) } },
                    onShare = { scope.launch { shareMedia(state.context, state.api, it) } },
                    onDelete = { media, mode ->
                        scope.launch {
                            if (mode == "para_mi") {
                                state.hideMedia(media)
                                message = "Ocultada de la galería"
                            } else {
                                val ok = state.deleteOnServer(media)
                                if (ok) {
                                    state.hideMedia(media)
                                    message = "Borrada del servidor"
                                } else {
                                    message = "El servidor no permite borrar (miniserve no hace DELETE)"
                                }
                            }
                        }
                    },
                    onCreateFolder = { name ->
                        scope.launch { state.createVirtualFolder(name) }
                    },
                    onMoveToFolder = { media, folderName -> state.moveToVirtualFolder(media, folderName) },
                    virtualFolders = state.virtualFolders,
                    onClose = { viewer = null }
                )
            } else {
                LibraryScreen(
                    state = state,
                    loader = loader,
                    gridState = gridState,
                    album = album,
                    darkTheme = state.darkTheme,
                    onToggleTheme = { state.toggleTheme() },
                    onAlbumChange = { album = it },
                    onOpen = { list, index -> viewer = ViewerTarget(list, index) },
                    onSettings = { screen = Screen.Settings },
                    virtualFolders = state.virtualFolders,
                    toast = message
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
    state: AppState,
    loader: ImageLoader,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    album: String?,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAlbumChange: (String?) -> Unit,
    onOpen: (List<Media>, Int) -> Unit,
    onSettings: () -> Unit,
    virtualFolders: Map<String, Set<String>>,
    toast: String? = null
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var filter by remember { mutableIntStateOf(0) }

    // Atrás desde Carpetas o desde un filtro vuelve a la galería, no cierra la app.
    BackHandler(enabled = album == null && (tab != 0 || filter != 0)) {
        if (tab != 0) tab = 0 else filter = 0
    }

    val base = remember(state.items, album, state.virtualFolders) {
        if (album == null) state.items
        else {
            val virtual = state.virtualFolders[album]
            if (virtual != null) state.items.filter { it.path in virtual }
            else state.items.filter { it.folder == album }
        }
    }
    val shown = remember(base, filter, state.favorites, state.hiddenPaths) {
        val visible = base.filter { it.path !in state.hiddenPaths }
        when (filter) {
            1 -> visible.filter { !it.isVideo }
            2 -> visible.filter { it.isVideo }
            3 -> visible.filter { it.path in state.favorites }
            else -> visible
        }
    }

    val title = album?.let { if (it.isBlank()) "Raíz" else it } ?: "Galería"
    val subtitle = if (album == null) {
        "${state.items.size} archivos"
    } else {
        "${base.size} ${if (base.size == 1) "elemento" else "elementos"}"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ExpressiveBottomBar(
                selectedIndex = tab,
                onSelect = { index ->
                    tab = index
                    if (index == 1 && album != null) onAlbumChange(null)
                },
                onRefresh = { scope.launch { state.refresh() } },
                refreshing = state.loading
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ExpressiveHeader(
                title = title,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onSettings = onSettings,
                onBack = if (album != null) ({ onAlbumChange(null) }) else null,
                subtitle = subtitle
            )

            if (album == null) {
                val labels = mutableListOf("Todo", "Fotos", "Videos")
                if (state.favorites.isNotEmpty()) labels += "Favoritos"
                ChipRow(
                    labels = labels,
                    selectedIndex = filter.coerceIn(0, labels.lastIndex),
                    onSelect = { filter = it }
                )
            }

            Spacer(Modifier.height(4.dp))

            val error = state.error
            if (error != null && shown.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            val notice = toast
            if (notice != null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = { scope.launch { state.refresh() } },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                when {
                    shown.isEmpty() && state.loading -> LoadingState(state.progress)

                    shown.isEmpty() && error != null -> ExpressiveMessage(
                        title = error,
                        detail = "Revisa que el celular viejo esté prendido y en el mismo Wi-Fi.",
                        icon = Icons.Filled.WifiOff,
                        buttonText = "Reintentar",
                        onButton = { scope.launch { state.refresh() } }
                    )

                    shown.isEmpty() -> ExpressiveMessage(
                        title = "Todavía no hay fotos",
                        detail = "Desliza hacia abajo para refrescar.",
                        icon = Icons.Filled.PhotoLibrary,
                        buttonText = "Refrescar",
                        onButton = { scope.launch { state.refresh() } }
                    )

                    album != null -> PhotosScreen(
                        items = shown,
                        loader = loader,
                        gridState = gridState,
                        columns = state.columns,
                        thumbUrlsFor = { state.api.thumbUrlsFor(it, thumbWidthFor(state.columns)) },
                        onOpen = { index -> onOpen(shown, index) }
                    )

                    tab == 0 -> PhotosScreen(
                        items = shown,
                        loader = loader,
                        gridState = gridState,
                        columns = state.columns,
                        thumbUrlsFor = { state.api.thumbUrlsFor(it, thumbWidthFor(state.columns)) },
                        showHero = true,
                        lastSync = state.lastSync,
                        onOpen = { index -> onOpen(shown, index) }
                    )

                    else -> FoldersScreen(
                        items = state.items,
                        loader = loader,
                        thumbUrlsFor = { state.api.thumbUrlsFor(it, 480) },
                        virtualFolders = state.virtualFolders,
                        onOpenFolder = { folder -> onAlbumChange(folder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(progress: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = progress.ifBlank { "Leyendo el servidor…" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}
