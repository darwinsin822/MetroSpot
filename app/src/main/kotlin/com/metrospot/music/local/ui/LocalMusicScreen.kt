/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.local.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrospot.music.LocalPlayerConnection
import com.metrospot.music.R
import com.metrospot.music.local.model.LocalAlbum
import com.metrospot.music.local.model.LocalArtist
import com.metrospot.music.local.model.LocalAudioTrack
import com.metrospot.music.local.model.LocalFolder
import com.metrospot.music.local.viewmodel.LocalMusicTab
import com.metrospot.music.local.viewmodel.LocalMusicViewModel
import com.metrospot.music.local.model.LocalSortOrder
import com.metrospot.music.playback.queues.ListQueue
import com.metrospot.music.ui.component.EmptyPlaceholder
import com.metrospot.music.ui.component.NavigationTitle
import com.metrospot.music.ui.utils.backToMain
import com.metrospot.music.utils.makeTimeString

// ──────────────────────────────────────────────────────────────────────────────
// Colour helpers
// ──────────────────────────────────────────────────────────────────────────────

/** Derives a stable accent colour from any string (folder/album/artist name). */
private fun accentColorFromString(text: String): Color {
    val hash = text.hashCode()
    val hue = ((hash and 0xFF_FF_FF).toLong() * 360L / 0xFF_FF_FFL).toFloat()
    return Color.hsl(hue.coerceIn(0f, 359f), 0.55f, 0.42f)
}

// ──────────────────────────────────────────────────────────────────────────────
// Root screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun LocalMusicScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.backToMain() },
    viewModel: LocalMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    val scanResult by viewModel.scanResult.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val filteredFolders by viewModel.filteredFolders.collectAsState()
    val filteredArtists by viewModel.filteredArtists.collectAsState()
    val filteredAlbums by viewModel.filteredAlbums.collectAsState()

    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()

    // Currently playing track id for highlight
    val currentMediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }
    val isPlaying by playerConnection?.isEffectivelyPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val nowPlayingId = currentMediaMetadata?.id  // matches LocalAudioTrack.contentUriString

    var hasPermission by remember {
        mutableStateOf(LocalMusicViewModel.hasStoragePermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.rescan()
        } else {
            Toast.makeText(context, context.getString(R.string.local_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    // Back handler: drill-down levels
    BackHandler(enabled = selectedFolder != null || selectedAlbum != null || selectedArtist != null) {
        when {
            selectedFolder != null -> viewModel.selectFolder(null)
            selectedAlbum != null  -> viewModel.selectAlbum(null)
            selectedArtist != null -> viewModel.selectArtist(null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────
            NavigationTitle(
                title = stringResource(R.string.filter_local),
                label = stringResource(R.string.filter_library),
                onClick = onBack,
                onPlayAllClick = if (filteredTracks.isNotEmpty()) {
                    {
                        playerConnection?.playQueue(
                            ListQueue(
                                title = context.getString(R.string.filter_local),
                                items = filteredTracks.map { it.toMediaItem() },
                                startIndex = 0,
                            )
                        )
                    }
                } else null
            )

            // ── Scanning progress bar ────────────────────────────────────────
            AnimatedVisibility(visible = scanResult.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // ── Permission wall ──────────────────────────────────────────────
            if (!hasPermission) {
                PermissionRequiredCard(
                    onRequest = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(perm)
                    }
                )
                return@Column
            }

            // ── Search + rescan row ──────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort menu (songs tab only)
                if (activeTab == LocalMusicTab.SONGS && selectedFolder == null &&
                    selectedAlbum == null && selectedArtist == null) {
                    var showSort by remember { mutableStateOf(false) }
                    Box {
                        FilledTonalIconButton(
                            onClick = { showSort = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.tune),
                                contentDescription = stringResource(R.string.local_sort),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                            LocalSortOrder.values().forEach { order ->
                                val label = when (order) {
                                    LocalSortOrder.TITLE_ASC        -> stringResource(R.string.local_sort_title_asc)
                                    LocalSortOrder.TITLE_DESC       -> stringResource(R.string.local_sort_title_desc)
                                    LocalSortOrder.ARTIST_ASC       -> stringResource(R.string.local_sort_artist)
                                    LocalSortOrder.DURATION_DESC    -> stringResource(R.string.local_sort_duration)
                                    LocalSortOrder.DATE_MODIFIED_DESC -> stringResource(R.string.local_sort_date_added)
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOrder == order) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = if (sortOrder == order) {
                                        { Icon(painterResource(R.drawable.play), null, Modifier.size(14.dp),
                                              tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = { viewModel.setSortOrder(order); showSort = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Rescan button
                FilledTonalIconButton(
                    onClick = { viewModel.rescan() },
                    modifier = Modifier.size(48.dp)
                ) {
                    if (scanResult.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = stringResource(R.string.local_rescan),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Detail or main tabs ──────────────────────────────────────────
            AnimatedContent(
                targetState = Triple(selectedFolder, selectedAlbum, selectedArtist),
                transitionSpec = {
                    (slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(220)))
                        .togetherWith(slideOutVertically(targetOffsetY = { -it / 5 }) + fadeOut(tween(180)))
                        .using(SizeTransform(clip = false))
                },
                label = "detail_transition"
            ) { (folder, album, artist) ->
                when {
                    folder != null -> FolderDetailView(
                        folder = folder,
                        nowPlayingId = nowPlayingId,
                        isPlaying = isPlaying,
                        onBack = { viewModel.selectFolder(null) },
                        onPlaySong = { index, songs ->
                            playerConnection?.playQueue(
                                ListQueue(title = folder.name, items = songs.map { it.toMediaItem() }, startIndex = index)
                            )
                        },
                        onShuffle = {
                            val shuffled = folder.tracks.shuffled()
                            playerConnection?.playQueue(
                                ListQueue(title = folder.name, items = shuffled.map { it.toMediaItem() }, startIndex = 0)
                            )
                        }
                    )
                    album != null -> AlbumDetailView(
                        album = album,
                        nowPlayingId = nowPlayingId,
                        isPlaying = isPlaying,
                        onBack = { viewModel.selectAlbum(null) },
                        onPlaySong = { index, songs ->
                            playerConnection?.playQueue(
                                ListQueue(title = album.title, items = songs.map { it.toMediaItem() }, startIndex = index)
                            )
                        },
                        onShuffle = {
                            val shuffled = album.tracks.shuffled()
                            playerConnection?.playQueue(
                                ListQueue(title = album.title, items = shuffled.map { it.toMediaItem() }, startIndex = 0)
                            )
                        }
                    )
                    artist != null -> ArtistDetailView(
                        artist = artist,
                        nowPlayingId = nowPlayingId,
                        isPlaying = isPlaying,
                        onBack = { viewModel.selectArtist(null) },
                        onPlaySong = { index, songs ->
                            playerConnection?.playQueue(
                                ListQueue(title = artist.name, items = songs.map { it.toMediaItem() }, startIndex = index)
                            )
                        },
                        onShuffle = {
                            val shuffled = artist.tracks.shuffled()
                            playerConnection?.playQueue(
                                ListQueue(title = artist.name, items = shuffled.map { it.toMediaItem() }, startIndex = 0)
                            )
                        }
                    )
                    else -> MainTabsContent(
                        activeTab = activeTab,
                        filteredFolders = filteredFolders,
                        filteredArtists = filteredArtists,
                        filteredAlbums = filteredAlbums,
                        filteredTracks = filteredTracks,
                        nowPlayingId = nowPlayingId,
                        isPlaying = isPlaying,
                        onTabChange = { viewModel.setTab(it) },
                        onFolderClick = { viewModel.selectFolder(it) },
                        onPlayFolder = { folder ->
                            playerConnection?.playQueue(
                                ListQueue(title = folder.name, items = folder.tracks.map { it.toMediaItem() }, startIndex = 0)
                            )
                        },
                        onArtistClick = { viewModel.selectArtist(it) },
                        onPlayArtist = { artist ->
                            playerConnection?.playQueue(
                                ListQueue(title = artist.name, items = artist.tracks.map { it.toMediaItem() }, startIndex = 0)
                            )
                        },
                        onAlbumClick = { viewModel.selectAlbum(it) },
                        onPlaySong = { index, songs ->
                            val ctx = context
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = ctx.getString(R.string.filter_local),
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index
                                )
                            )
                        },
                        onShuffleSongs = {
                            val shuffled = filteredTracks.shuffled()
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.filter_local),
                                    items = shuffled.map { it.toMediaItem() },
                                    startIndex = 0
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Main tabs scaffold
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainTabsContent(
    activeTab: LocalMusicTab,
    filteredFolders: List<LocalFolder>,
    filteredArtists: List<LocalArtist>,
    filteredAlbums: List<LocalAlbum>,
    filteredTracks: List<LocalAudioTrack>,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onTabChange: (LocalMusicTab) -> Unit,
    onFolderClick: (LocalFolder) -> Unit,
    onPlayFolder: (LocalFolder) -> Unit,
    onArtistClick: (LocalArtist) -> Unit,
    onPlayArtist: (LocalArtist) -> Unit,
    onAlbumClick: (LocalAlbum) -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
    onShuffleSongs: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            LocalMusicTab.values().forEach { tab ->
                val count = when (tab) {
                    LocalMusicTab.FOLDERS -> filteredFolders.size
                    LocalMusicTab.ARTISTS -> filteredArtists.size
                    LocalMusicTab.ALBUMS  -> filteredAlbums.size
                    LocalMusicTab.SONGS   -> filteredTracks.size
                }
                val tabTitle = when (tab) {
                    LocalMusicTab.FOLDERS -> stringResource(R.string.local_folders)
                    LocalMusicTab.ARTISTS -> stringResource(R.string.local_artists)
                    LocalMusicTab.ALBUMS  -> stringResource(R.string.local_albums)
                    LocalMusicTab.SONGS   -> stringResource(R.string.local_songs)
                }
                Tab(
                    selected = activeTab == tab,
                    onClick = { onTabChange(tab) },
                    text = {
                        Text(
                            text = if (count > 0) "$tabTitle ($count)" else tabTitle,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Per-tab content
        when (activeTab) {
            LocalMusicTab.FOLDERS -> FoldersTabContent(
                folders = filteredFolders,
                onFolderClick = onFolderClick,
                onPlayFolder = onPlayFolder
            )
            LocalMusicTab.ARTISTS -> ArtistsTabContent(
                artists = filteredArtists,
                onArtistClick = onArtistClick,
                onPlayArtist = onPlayArtist
            )
            LocalMusicTab.ALBUMS -> AlbumsTabContent(
                albums = filteredAlbums,
                onAlbumClick = onAlbumClick
            )
            LocalMusicTab.SONGS -> SongsTabContent(
                tracks = filteredTracks,
                nowPlayingId = nowPlayingId,
                isPlaying = isPlaying,
                onPlaySong = onPlaySong,
                onShuffleSongs = if (filteredTracks.isNotEmpty()) onShuffleSongs else null
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Now-Playing animated equalizer bars
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayingBarsIndicator(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(290, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { frac ->
            val h = if (isPlaying) frac else 0.35f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14 * h).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Folders tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FoldersTabContent(
    folders: List<LocalFolder>,
    onFolderClick: (LocalFolder) -> Unit,
    onPlayFolder: (LocalFolder) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyPlaceholder(icon = R.drawable.folder, text = stringResource(R.string.local_music_empty))
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.path }) { folder ->
            val accent = remember(folder.name) { accentColorFromString(folder.name) }
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clickable { onFolderClick(folder) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.folder),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.local_track_count, folder.trackCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                    }

                    FilledTonalIconButton(onClick = { onPlayFolder(folder) }) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = stringResource(R.string.local_play_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Artists tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistsTabContent(
    artists: List<LocalArtist>,
    onArtistClick: (LocalArtist) -> Unit,
    onPlayArtist: (LocalArtist) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyPlaceholder(icon = R.drawable.artist, text = stringResource(R.string.local_music_empty))
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists, key = { it.name }) { artist ->
            val accent = remember(artist.name) { accentColorFromString(artist.name) }
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clickable { onArtistClick(artist) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    // Artist avatar with initial letter
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.1f))
                                )
                            )
                    ) {
                        Text(
                            text = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.local_artist_meta,
                                artist.trackCount,
                                artist.albums.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                    }

                    FilledTonalIconButton(onClick = { onPlayArtist(artist) }) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Albums tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AlbumsTabContent(
    albums: List<LocalAlbum>,
    onAlbumClick: (LocalAlbum) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyPlaceholder(icon = R.drawable.album, text = stringResource(R.string.local_music_empty))
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { "${it.id}_${it.title}" }) { album ->
            val fallbackColor = remember(album.title) { accentColorFromString(album.title) }
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clickable { onAlbumClick(album) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(fallbackColor.copy(alpha = 0.18f))
                    ) {
                        if (!album.albumArtUri.isNullOrBlank()) {
                            AsyncImage(
                                model = album.albumArtUri,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = album.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = fallbackColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.local_album_meta, album.artist, album.trackCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Songs list (reused in tabs + detail views)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SongsTabContent(
    tracks: List<LocalAudioTrack>,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
    onShuffleSongs: (() -> Unit)? = null,
) {
    if (tracks.isEmpty()) {
        EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.local_music_empty))
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (onShuffleSongs != null) 88.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(tracks, key = { _, t -> "${t.id}_${t.path}" }) { index, track ->
                val isCurrentTrack = track.contentUriString == nowPlayingId
                val bgColor by animateColorAsState(
                    targetValue = if (isCurrentTrack)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        Color.Transparent,
                    animationSpec = tween(300),
                    label = "row_bg"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onPlaySong(index, tracks) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Thumbnail / album art
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (!track.albumArtUri.isNullOrBlank()) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        // Playing overlay
                        if (isCurrentTrack) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.38f))
                            )
                            PlayingBarsIndicator(
                                isPlaying = isPlaying,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrentTrack) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = makeTimeString(track.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Shuffle FAB — appears only when list is non-empty and callback provided
        if (onShuffleSongs != null) {
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "fab_scale"
            )
            androidx.compose.material3.FloatingActionButton(
                onClick = onShuffleSongs,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .scale(scale)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.local_shuffle),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.local_shuffle),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Shared detail header (Folder / Album / Artist)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailHeader(
    title: String,
    subtitle: String,
    heroContent: (@Composable () -> Unit)? = null,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column {
        // Hero image (albums show artwork banner)
        if (heroContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                heroContent()
                // Gradient scrim at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
                // Back button in top-left corner
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
                // Title + subtitle pinned to bottom of hero
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // No hero — compact bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Atrás"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                }
            }
        }

        // Action row: Play all + Shuffle
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onPlayAll,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.local_play_all))
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onShuffle,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.local_shuffle))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Detail views
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FolderDetailView(
    folder: LocalFolder,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
    onShuffle: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = folder.name,
            subtitle = stringResource(R.string.local_track_count, folder.trackCount),
            heroContent = null,
            onBack = onBack,
            onPlayAll = { onPlaySong(0, folder.tracks) },
            onShuffle = onShuffle
        )
        SongsTabContent(
            tracks = folder.tracks,
            nowPlayingId = nowPlayingId,
            isPlaying = isPlaying,
            onPlaySong = onPlaySong,
            onShuffleSongs = null // header already has shuffle
        )
    }
}

@Composable
private fun AlbumDetailView(
    album: LocalAlbum,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
    onShuffle: () -> Unit,
) {
    val fallbackColor = remember(album.title) { accentColorFromString(album.title) }
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = album.title,
            subtitle = stringResource(R.string.local_album_meta, album.artist, album.trackCount),
            heroContent = {
                if (!album.albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = album.albumArtUri,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fallbackColor.copy(alpha = 0.5f),
                                        fallbackColor.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = album.title.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = fallbackColor,
                        )
                    }
                }
            },
            onBack = onBack,
            onPlayAll = { onPlaySong(0, album.tracks) },
            onShuffle = onShuffle
        )
        SongsTabContent(
            tracks = album.tracks,
            nowPlayingId = nowPlayingId,
            isPlaying = isPlaying,
            onPlaySong = onPlaySong,
            onShuffleSongs = null
        )
    }
}

@Composable
private fun ArtistDetailView(
    artist: LocalArtist,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
    onShuffle: () -> Unit,
) {
    val accent = remember(artist.name) { accentColorFromString(artist.name) }
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = artist.name,
            subtitle = stringResource(R.string.local_artist_meta, artist.trackCount, artist.albums.size),
            heroContent = {
                // Artist hero: gradient with large initial letter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                    )
                }
            },
            onBack = onBack,
            onPlayAll = { onPlaySong(0, artist.tracks) },
            onShuffle = onShuffle
        )
        SongsTabContent(
            tracks = artist.tracks,
            nowPlayingId = nowPlayingId,
            isPlaying = isPlaying,
            onPlaySong = onPlaySong,
            onShuffleSongs = null
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Permission card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionRequiredCard(onRequest: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.folder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.filter_local),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.local_permission_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRequest,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.local_grant_permission),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
