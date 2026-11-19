/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.local.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrospot.music.local.model.LocalAlbum
import com.metrospot.music.local.model.LocalArtist
import com.metrospot.music.local.model.LocalAudioTrack
import com.metrospot.music.local.model.LocalFolder
import com.metrospot.music.local.model.LocalScanResult
import com.metrospot.music.local.model.LocalSortOrder
import com.metrospot.music.local.scanner.LocalMediaScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LocalMusicTab {
    FOLDERS,
    ARTISTS,
    ALBUMS,
    SONGS,
}

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val scanResult: StateFlow<LocalScanResult> = LocalMediaScanner.scanState

    private val _activeTab = MutableStateFlow(LocalMusicTab.FOLDERS)
    val activeTab: StateFlow<LocalMusicTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(LocalSortOrder.TITLE_ASC)
    val sortOrder: StateFlow<LocalSortOrder> = _sortOrder.asStateFlow()

    private val _selectedFolder = MutableStateFlow<LocalFolder?>(null)
    val selectedFolder: StateFlow<LocalFolder?> = _selectedFolder.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<LocalAlbum?>(null)
    val selectedAlbum: StateFlow<LocalAlbum?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<LocalArtist?>(null)
    val selectedArtist: StateFlow<LocalArtist?> = _selectedArtist.asStateFlow()

    val filteredTracks: StateFlow<List<LocalAudioTrack>> =
        combine(scanResult, searchQuery, sortOrder) { res, query, sort ->
            val list = if (query.isBlank()) {
                res.tracks
            } else {
                res.tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
            }
            when (sort) {
                LocalSortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                LocalSortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
                LocalSortOrder.ARTIST_ASC -> list.sortedBy { it.artist.lowercase() }
                LocalSortOrder.DURATION_DESC -> list.sortedByDescending { it.durationMs }
                LocalSortOrder.DATE_MODIFIED_DESC -> list.sortedByDescending { it.dateModified }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFolders: StateFlow<List<LocalFolder>> =
        combine(scanResult, searchQuery) { res, query ->
            if (query.isBlank()) res.folders
            else res.folders.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.tracks.any { t -> t.title.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtists: StateFlow<List<LocalArtist>> =
        combine(scanResult, searchQuery) { res, query ->
            if (query.isBlank()) res.artists
            else res.artists.filter { it.name.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAlbums: StateFlow<List<LocalAlbum>> =
        combine(scanResult, searchQuery) { res, query ->
            if (query.isBlank()) res.albums
            else res.albums.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (hasStoragePermission(context)) {
            rescan()
        }
    }

    fun rescan() {
        viewModelScope.launch {
            LocalMediaScanner.scanDeviceAudio(context)
        }
    }

    fun setTab(tab: LocalMusicTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: LocalSortOrder) {
        _sortOrder.value = order
    }

    fun selectFolder(folder: LocalFolder?) {
        _selectedFolder.value = folder
    }

    fun selectAlbum(album: LocalAlbum?) {
        _selectedAlbum.value = album
    }

    fun selectArtist(artist: LocalArtist?) {
        _selectedArtist.value = artist
    }

    companion object {
        fun hasStoragePermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
