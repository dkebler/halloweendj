package com.example.spookydj

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.spookydj.data.PlaylistStore
import com.example.spookydj.model.Track
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppUI(
                        onPlay = { track, _ ->
                            playTrack(track)
                        },
                        onStop = {
                            player.stop()
                        },
                        takePersistable = { uri ->
                            try {
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (_: SecurityException) { }
                        }
                    )
                }
            }
        }
    }

    private fun playTrack(track: Track) {
        lifecycleScope.launch {
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(track.uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.repeatMode =
                if (track.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.playWhenReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppUI(
    onPlay: (Track, List<Track>) -> Unit,
    onStop: () -> Unit,
    takePersistable: (Uri) -> Unit
) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf(listOf<Track>()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    val pickMultiple = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
            val newTracks = uris.map { uri ->
                takePersistable(uri)
                Track(
                    uri = uri,
                    displayName = resolveDisplayName(context, uri) ?: uri.lastPathSegment.orEmpty(),
                    loop = false
                )
            }
            tracks = tracks + newTracks
        }
    )

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("SpookyDJ", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    pickMultiple.launch(arrayOf("audio/*"))
                }) {
                    Text("Add Tracks")
                }
                OutlinedButton(onClick = { onStop() }) {
                    Text("Stop")
                }
            }

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tap 'Add Tracks' to choose audio files")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tracks, key = { _, t -> t.uri.toString() }) { index, track ->
                        TrackRow(
                            track = track,
                            onPlay = { onPlay(track, tracks) },
                            onToggleLoop = { checked ->
                                tracks = tracks.toMutableList().also {
                                    it[index] = it[index].copy(loop = checked)
                                }
                            },
                            onRemove = {
                                tracks = tracks.toMutableList().also { it.removeAt(index) }
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = tracks.isNotEmpty(),
                    onClick = { showSaveDialog = true; playlistName = "" }
                ) {
                    Text("Save Playlist")
                }
                OutlinedButton(
                    onClick = { showLoadDialog = true }
                ) {
                    Text("Load Playlist")
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = playlistName.isNotBlank(),
                    onClick = {
                        PlaylistStore.savePlaylist(context, playlistName.trim(), tracks)
                        showSaveDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLoadDialog) {
        val names = PlaylistStore.getPlaylistNames(context)
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Load Playlist") },
            text = {
                if (names.isEmpty()) {
                    Text("No saved playlists yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        names.forEach { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val loaded = PlaylistStore.loadPlaylist(context, name)
                                    tracks = loaded
                                    showLoadDialog = false
                                }) { Text("Load") }
                                TextButton(onClick = {
                                    PlaylistStore.deletePlaylist(context, name)
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(
                track.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onPlay) {
                    Text("Play")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = track.loop,
                        onCheckedChange = onToggleLoop
                    )
                    Text("Loop")
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else null
    }
}