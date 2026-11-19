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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.metrospot.music.playback.queues.ListQueue
import com.metrospot.music.ui.component.EmptyPlaceholder
import com.metrospot.music.ui.component.NavigationTitle
import com.metrospot.music.ui.utils.backToMain

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

    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val filteredFolders by viewModel.filteredFolders.collectAsState()
    val filteredArtists by viewModel.filteredArtists.collectAsState()
    val filteredAlbums by viewModel.filteredAlbums.collectAsState()

    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()

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

    // Interceptar atrás en subniveles (carpeta, álbum, artista seleccionado)
    BackHandler(enabled = selectedFolder != null || selectedAlbum != null || selectedArtist != null) {
        when {
            selectedFolder != null -> viewModel.selectFolder(null)
            selectedAlbum != null -> viewModel.selectAlbum(null)
            selectedArtist != null -> viewModel.selectArtist(null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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

            // Permisos no concedidos
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

            // Barra de búsqueda rápida y botón de rescan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
                        if (searchQuery.isNotEmpty()) {
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

                IconButton(
                    onClick = { viewModel.rescan() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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

            // Vista de detalle si hay una selección activa
            when {
                selectedFolder != null -> {
                    FolderDetailView(
                        folder = selectedFolder!!,
                        onBack = { viewModel.selectFolder(null) },
                        onPlaySong = { index, songs ->
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = selectedFolder!!.name,
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index
                                )
                            )
                        }
                    )
                }
                selectedAlbum != null -> {
                    AlbumDetailView(
                        album = selectedAlbum!!,
                        onBack = { viewModel.selectAlbum(null) },
                        onPlaySong = { index, songs ->
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = selectedAlbum!!.title,
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index
                                )
                            )
                        }
                    )
                }
                selectedArtist != null -> {
                    ArtistDetailView(
                        artist = selectedArtist!!,
                        onBack = { viewModel.selectArtist(null) },
                        onPlaySong = { index, songs ->
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = selectedArtist!!.name,
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index
                                )
                            )
                        }
                    )
                }
                else -> {
                    // Pestañas Metro principales
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
                                LocalMusicTab.ALBUMS -> filteredAlbums.size
                                LocalMusicTab.SONGS -> filteredTracks.size
                            }
                            val tabTitle = when (tab) {
                                LocalMusicTab.FOLDERS -> stringResource(R.string.local_folders)
                                LocalMusicTab.ARTISTS -> stringResource(R.string.local_artists)
                                LocalMusicTab.ALBUMS -> stringResource(R.string.local_albums)
                                LocalMusicTab.SONGS -> stringResource(R.string.local_songs)
                            }
                            Tab(
                                selected = activeTab == tab,
                                onClick = { viewModel.setTab(tab) },
                                text = {
                                    Text(
                                        text = "$tabTitle ($count)",
                                        fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }

                    // Contenido según pestaña activa
                    when (activeTab) {
                        LocalMusicTab.FOLDERS -> {
                            FoldersTabContent(
                                folders = filteredFolders,
                                onFolderClick = { viewModel.selectFolder(it) },
                                onPlayFolder = { folder ->
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = folder.name,
                                            items = folder.tracks.map { it.toMediaItem() },
                                            startIndex = 0
                                        )
                                    )
                                }
                            )
                        }
                        LocalMusicTab.ARTISTS -> {
                            ArtistsTabContent(
                                artists = filteredArtists,
                                onArtistClick = { viewModel.selectArtist(it) },
                                onPlayArtist = { artist ->
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = artist.name,
                                            items = artist.tracks.map { it.toMediaItem() },
                                            startIndex = 0
                                        )
                                    )
                                }
                            )
                        }
                        LocalMusicTab.ALBUMS -> {
                            AlbumsTabContent(
                                albums = filteredAlbums,
                                onAlbumClick = { viewModel.selectAlbum(it) }
                            )
                        }
                        LocalMusicTab.SONGS -> {
                            SongsTabContent(
                                tracks = filteredTracks,
                                onPlaySong = { index, songs ->
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.filter_local),
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    folders: List<LocalFolder>,
    onFolderClick: (LocalFolder) -> Unit,
    onPlayFolder: (LocalFolder) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyPlaceholder(
            icon = R.drawable.folder,
            text = stringResource(R.string.local_music_empty)
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.path }) { folder ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.folder),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
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
                            text = "${folder.trackCount} canciones • ${folder.path}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onPlayFolder(folder) }) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = stringResource(R.string.local_play_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsTabContent(
    artists: List<LocalArtist>,
    onArtistClick: (LocalArtist) -> Unit,
    onPlayArtist: (LocalArtist) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyPlaceholder(
            icon = R.drawable.artist,
            text = stringResource(R.string.local_music_empty)
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists, key = { it.name }) { artist ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
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
                            text = "${artist.trackCount} canciones • ${artist.albums.size} álbumes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onPlayArtist(artist) }) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsTabContent(
    albums: List<LocalAlbum>,
    onAlbumClick: (LocalAlbum) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyPlaceholder(
            icon = R.drawable.album,
            text = stringResource(R.string.local_music_empty)
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { "${it.id}_${it.title}" }) { album ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (!album.albumArtUri.isNullOrBlank()) {
                            AsyncImage(
                                model = album.albumArtUri,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.album),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
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
                        text = "${album.artist} • ${album.trackCount} pistas",
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

@Composable
private fun SongsTabContent(
    tracks: List<LocalAudioTrack>,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
) {
    if (tracks.isEmpty()) {
        EmptyPlaceholder(
            icon = R.drawable.music_note,
            text = stringResource(R.string.local_music_empty)
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(tracks, key = { _, t -> "${t.id}_${t.path}" }) { index, track ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaySong(index, tracks) }
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
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

                val durSec = (track.durationMs / 1000).toInt()
                val min = durSec / 60
                val sec = durSec % 60
                Text(
                    text = String.format("%d:%02d", min, sec),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderDetailView(
    folder: LocalFolder,
    onBack: () -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Atrás"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.trackCount} canciones en ${folder.path}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onPlaySong(0, folder.tracks) }) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        SongsTabContent(tracks = folder.tracks, onPlaySong = onPlaySong)
    }
}

@Composable
private fun AlbumDetailView(
    album: LocalAlbum,
    onBack: () -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Atrás"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.artist} • ${album.trackCount} canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onPlaySong(0, album.tracks) }) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        SongsTabContent(tracks = album.tracks, onPlaySong = onPlaySong)
    }
}

@Composable
private fun ArtistDetailView(
    artist: LocalArtist,
    onBack: () -> Unit,
    onPlaySong: (Int, List<LocalAudioTrack>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Atrás"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.trackCount} canciones • ${artist.albums.size} álbumes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onPlaySong(0, artist.tracks) }) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        SongsTabContent(tracks = artist.tracks, onPlaySong = onPlaySong)
    }
}

@Composable
private fun PermissionRequiredCard(onRequest: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.folder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.filter_local),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.local_permission_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequest,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.local_grant_permission))
                }
            }
        }
    }
}
