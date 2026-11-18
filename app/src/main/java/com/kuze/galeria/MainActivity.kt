package com.kuze.galeria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.kuze.galeria.data.AppState
import com.kuze.galeria.data.Media
import com.kuze.galeria.ui.FoldersScreen
import com.kuze.galeria.ui.GaleriaTheme
import com.kuze.galeria.ui.PhotosScreen
import com.kuze.galeria.ui.SettingsScreen
import com.kuze.galeria.ui.ViewerScreen
import com.kuze.galeria.ui.buildImageLoader
import kotlinx.coroutines.launch

private enum class Screen { Library, Settings }

private data class ViewerTarget(val items: List<Media>, val index: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent {
            GaleriaTheme {
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

    var screen by remember {
        mutableStateOf(if (state.settings.isConfigured) Screen.Library else Screen.Settings)
    }
    var viewer by remember { mutableStateOf<ViewerTarget?>(null) }
    var album by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { state.loadInitial() }

    when (screen) {
        Screen.Settings -> SettingsScreen(
            settings = state.settings,
            api = state.api,
            firstRun = !state.settings.isConfigured,
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
                    loader = loader,
                    api = state.api,
                    onClose = { viewer = null }
                )
            } else {
                LibraryScreen(
                    state = state,
                    loader = loader,
                    album = album,
                    onAlbumChange = { album = it },
                    onOpen = { list, index -> viewer = ViewerTarget(list, index) },
                    onSettings = { screen = Screen.Settings }
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
    album: String?,
    onAlbumChange: (String?) -> Unit,
    onOpen: (List<Media>, Int) -> Unit,
    onSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }

    val shown = remember(state.items, album) {
        if (album == null) state.items else state.items.filter { it.folder == album }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = album?.let { if (it.isBlank()) "Raíz" else it } ?: "Galería Kuze",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (album != null) {
                        IconButton(onClick = { onAlbumChange(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { state.refresh() } },
                        enabled = !state.loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (album == null) {
                TabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = { Text("Fotos") }
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = { Text("Carpetas") }
                    )
                }
            }

            val error = state.error
            if (error != null && shown.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = { scope.launch { state.refresh() } },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    shown.isEmpty() && state.loading -> LoadingState(state.progress)

                    shown.isEmpty() && error != null -> MessageState(
                        text = error,
                        buttonText = "Reintentar",
                        onButton = { scope.launch { state.refresh() } }
                    )

                    shown.isEmpty() -> MessageState(
                        text = "Todavía no hay fotos.\nDesliza hacia abajo para refrescar.",
                        buttonText = "Refrescar",
                        onButton = { scope.launch { state.refresh() } }
                    )

                    album != null -> PhotosScreen(shown, loader) { index -> onOpen(shown, index) }

                    tab == 0 -> PhotosScreen(shown, loader) { index -> onOpen(shown, index) }

                    else -> FoldersScreen(state.items, loader) { folder -> onAlbumChange(folder) }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(progress: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Text(
                text = progress.ifBlank { "Leyendo el servidor…" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun MessageState(text: String, buttonText: String?, onButton: (() -> Unit)?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (buttonText != null && onButton != null) {
                Button(onClick = onButton, modifier = Modifier.padding(top = 16.dp)) {
                    Text(buttonText)
                }
            }
        }
    }
}
