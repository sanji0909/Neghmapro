package com.example.naghma

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private val Ink = Color(0xFF0B0B12)
private val Panel = Color(0xFF171722)
private val Violet = Color(0xFFB18CFF)
private val Mint = Color(0xFF7EF0CF)

data class Song(val id: Long, val title: String, val artist: String, val uri: Uri, val duration: Long)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private var equalizer: Equalizer? = null
    private val songs = mutableStateListOf<Song>()
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadSongs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED) loadSongs()
        else permission.launch(audioPermission)

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                try {
                    equalizer?.release()
                    equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
                } catch (_: Exception) { equalizer = null }
            }
        })
        setContent {
            var currentIndex by remember { mutableIntStateOf(-1) }
            var playing by remember { mutableStateOf(false) }
            var showEq by remember { mutableStateOf(false) }
            var liked by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Violet, secondary = Mint, background = Ink, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Violet, Mint)), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = Ink, modifier = Modifier.size(27.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("NAGHMA", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text("PRO AUDIO PLAYER", fontSize = 10.sp, color = Mint, letterSpacing = 2.sp)
                            }
                            IconButton(onClick = { showEq = !showEq }) { Icon(Icons.Default.GraphicEq, "Equalizer", tint = Violet) }
                        }
                        Spacer(Modifier.height(22.dp))
                        if (showEq) {
                            EqualizerPanel(onBack = { showEq = false }, eq = equalizer)
                        } else {
                            Text("YOUR LIBRARY", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("Feel every frequency.", fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
                            if (songs.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.MusicNote, null, tint = Violet, modifier = Modifier.size(58.dp))
                                        Spacer(Modifier.height(12.dp))
                                        Text("Your music lives here", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Allow audio access to scan songs on your phone.", color = Color.LightGray)
                                    }
                                }
                            } else {
                                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(songs) { index, song ->
                                        Row(
                                            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    currentIndex = index
                                                    player.setMediaItems(songs.map { MediaItem.fromUri(it.uri) }, index, 0L)
                                                    player.prepare()
                                                    player.play()
                                                    playing = true
                                                }.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(Color(0xFF37265B), Color(0xFF174C49))), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.MusicNote, null, tint = Mint)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(song.artist, color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(formatTime(song.duration), color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (currentIndex in songs.indices) {
                                val song = songs[currentIndex]
                                Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(24.dp)).padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(52.dp).background(Brush.linearGradient(listOf(Violet, Color(0xFF47336E))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(30.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(song.artist, color = Color.LightGray, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = { liked = !liked }) { Icon(Icons.Default.Favorite, "Favorite", tint = if (liked) Violet else Color.Gray) }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Slider(
                                        value = if (player.duration > 0) (player.currentPosition.toFloat() / player.duration).coerceIn(0f, 1f) else 0f,
                                        onValueChange = { if (player.duration > 0) player.seekTo((it * player.duration).toLong()) },
                                        colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Violet)
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(formatTime(player.currentPosition), color = Color.Gray, fontSize = 11.sp)
                                        Text(formatTime(player.duration.coerceAtLeast(0)), color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { player.seekToPreviousMediaItem() }) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(30.dp)) }
                                        Spacer(Modifier.width(14.dp))
                                        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play(); playing = player.isPlaying }, modifier = Modifier.size(58.dp).background(Brush.linearGradient(listOf(Violet, Mint)), CircleShape)) {
                                            Icon(if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", tint = Ink, modifier = Modifier.size(34.dp))
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        IconButton(onClick = { player.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(30.dp)) }
                                    }
                                }
                            } else {
                                Text("Tap a song to start listening", color = Color.Gray, modifier = Modifier.padding(bottom = 20.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    private fun loadSongs() {
        songs.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION)
        contentResolver.query(collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                songs.add(Song(id, cursor.getString(titleCol) ?: "Unknown title", cursor.getString(artistCol) ?: "Unknown artist", ContentUris.withAppendedId(collection, id), cursor.getLong(durationCol)))
            }
        }
    }

    override fun onDestroy() {
        equalizer?.release()
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun EqualizerPanel(onBack: () -> Unit, eq: Equalizer?) {
    Column(Modifier.fillMaxSize()) {
        Text("AUDIO LAB", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Shape your sound", fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
        Text("Device-supported equalizer bands", color = Color.LightGray, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        if (eq == null) {
            Text("Start playback first. Equalizer availability depends on your Android audio effects.", color = Color.LightGray)
        } else {
            val range = eq.bandLevelRange
            val bands = (0 until eq.numberOfBands).toList()
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                bands.forEach { band ->
                    var level by remember(eq, band) { mutableFloatStateOf(eq.getBandLevel(band.toShort()).toFloat()) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("${eq.getCenterFreq(band.toShort()) / 1000}k", fontSize = 10.sp, color = Color.LightGray)
                        Slider(
                            value = level,
                            onValueChange = {
                                level = it
                                try { eq.setBandLevel(band.toShort(), it.toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()) } catch (_: Exception) {}
                            },
                            valueRange = range[0].toFloat()..range[1].toFloat(),
                            modifier = Modifier.height(300.dp).width(42.dp),
                            colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Violet)
                        )
                        Text("${(level / 100).toInt()} dB", fontSize = 9.sp, color = Color.LightGray)
                    }
                }
            }
            Button(onClick = { bands.forEach { try { eq.setBandLevel(it.toShort(), 0) } catch (_: Exception) {} } }, colors = ButtonDefaults.buttonColors(containerColor = Panel)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Reset EQ")
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Back to library") }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val seconds = ms / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
