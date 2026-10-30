/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.spotify

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.metrospot.innertube.YouTube
import com.metrospot.innertube.models.SongItem
import com.metrospot.music.models.toMediaMetadata
import com.metrospot.music.db.MusicDatabase
import com.metrospot.music.db.entities.PlaylistEntity
import com.metrospot.music.db.entities.PlaylistSongMap
import com.metrospot.music.playback.DownloadUtil
import com.metrospot.music.playback.ExoDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.abs

enum class TrackMigrateStatus {
    PENDING,
    SEARCHING,
    DOWNLOADING,
    COMPLETED,
    NOT_FOUND,
}

data class TrackMigrateItem(
    val spotifyTrack: SpotifyTrack,
    val status: TrackMigrateStatus = TrackMigrateStatus.PENDING,
    val matchedSong: SongItem? = null,
)

sealed interface MigrateUiState {
    data object Idle : MigrateUiState
    data object Extracting : MigrateUiState
    data class Ready(
        val playlist: SpotifyPlaylist,
        val items: List<TrackMigrateItem>,
        val isMigrating: Boolean = false,
        val processedCount: Int = 0,
        val totalCount: Int = 0,
        val error: String? = null,
    ) : MigrateUiState
    data class Error(val message: String) : MigrateUiState
}

@HiltViewModel
class SpotifyMigrateViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MigrateUiState>(MigrateUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val searchSemaphore = Semaphore(3)

    fun analyzePlaylistUrl(url: String) {
        if (url.isBlank()) {
            _uiState.value = MigrateUiState.Error("Por favor, ingresa una URL válida de Spotify.")
            return
        }

        viewModelScope.launch {
            _uiState.value = MigrateUiState.Extracting
            try {
                val playlistId = SpotifyPlaylistExtractor.resolvePlaylistId(url)
                    ?: throw IllegalArgumentException("No se encontró un ID de playlist válido en el enlace proporcionado.")

                val playlist = SpotifyPlaylistExtractor.fetchFromEmbed(playlistId)
                if (playlist.tracks.isEmpty()) {
                    throw IllegalStateException("La playlist no contiene pistas o no es pública.")
                }

                val items = playlist.tracks.map { TrackMigrateItem(spotifyTrack = it) }
                _uiState.value = MigrateUiState.Ready(
                    playlist = playlist,
                    items = items,
                    totalCount = items.size,
                )
            } catch (e: Exception) {
                _uiState.value = MigrateUiState.Error(e.message ?: "Error al obtener la lista de Spotify.")
            }
        }
    }

    fun startMigrationAndDownload() {
        val currentState = _uiState.value as? MigrateUiState.Ready ?: return
        if (currentState.isMigrating) return

        _uiState.update {
            currentState.copy(isMigrating = true, processedCount = 0)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val playlist = currentState.playlist
            val playlistId = PlaylistEntity.generatePlaylistId()
            val playlistEntity = PlaylistEntity(
                id = playlistId,
                name = playlist.title,
                createdAt = LocalDateTime.now(),
                lastUpdateTime = LocalDateTime.now(),
                thumbnailUrl = playlist.coverUrl,
            )
            database.insert(playlistEntity)

            val currentItems = currentState.items.toMutableList()
            var currentPosition = 0

            val deferredResults = currentItems.mapIndexed { index, item ->
                async {
                    searchSemaphore.withPermit {
                        updateItemStatus(index, TrackMigrateStatus.SEARCHING)

                        val query = "${item.spotifyTrack.title} ${item.spotifyTrack.artists}".trim()
                        val targetSeconds = (item.spotifyTrack.durationMs / 1000).toInt()

                        var matched: SongItem? = null

                        try {
                            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            val songItems = result?.items?.filterIsInstance<SongItem>() ?: emptyList()

                            if (songItems.isNotEmpty()) {
                                matched = songItems.minByOrNull { song ->
                                    val dur = song.duration ?: 0
                                    if (dur > 0 && targetSeconds > 0) {
                                        abs(dur - targetSeconds)
                                    } else {
                                        0
                                    }
                                } ?: songItems.first()
                            }
                        } catch (_: Exception) {
                        }

                        if (matched != null) {
                            val mediaMetadata = matched.toMediaMetadata()
                            database.insert(mediaMetadata)

                            synchronized(playlistEntity) {
                                database.insert(
                                    PlaylistSongMap(
                                        playlistId = playlistId,
                                        songId = matched.id,
                                        position = currentPosition++,
                                    ),
                                )
                            }

                            updateItemStatus(index, TrackMigrateStatus.DOWNLOADING, matched)

                            try {
                                val downloadRequest = DownloadRequest.Builder(matched.id, matched.id.toUri())
                                    .setCustomCacheKey(matched.id)
                                    .setData(matched.title.toByteArray())
                                    .build()

                                DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false,
                                )
                                updateItemStatus(index, TrackMigrateStatus.COMPLETED, matched)
                            } catch (_: Exception) {
                                updateItemStatus(index, TrackMigrateStatus.COMPLETED, matched)
                            }
                        } else {
                            updateItemStatus(index, TrackMigrateStatus.NOT_FOUND)
                        }

                        incrementProcessedCount()
                    }
                }
            }

            deferredResults.awaitAll()

            _uiState.update { state ->
                if (state is MigrateUiState.Ready) {
                    state.copy(isMigrating = false)
                } else state
            }
        }
    }

    private fun updateItemStatus(index: Int, status: TrackMigrateStatus, matched: SongItem? = null) {
        _uiState.update { state ->
            if (state is MigrateUiState.Ready) {
                val updatedItems = state.items.toMutableList()
                if (index in updatedItems.indices) {
                    val current = updatedItems[index]
                    updatedItems[index] = current.copy(
                        status = status,
                        matchedSong = matched ?: current.matchedSong,
                    )
                }
                state.copy(items = updatedItems)
            } else state
        }
    }

    private fun incrementProcessedCount() {
        _uiState.update { state ->
            if (state is MigrateUiState.Ready) {
                state.copy(processedCount = state.processedCount + 1)
            } else state
        }
    }

    fun reset() {
        _uiState.value = MigrateUiState.Idle
    }
}
