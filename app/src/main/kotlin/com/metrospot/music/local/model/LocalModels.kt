/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.local.model

import java.io.Serializable

data class LocalFolder(
    val name: String,
    val path: String,
    val tracks: List<LocalAudioTrack>,
) : Serializable {
    val trackCount: Int get() = tracks.size
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class LocalAlbum(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val tracks: List<LocalAudioTrack>,
) : Serializable {
    val trackCount: Int get() = tracks.size
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class LocalArtist(
    val name: String,
    val tracks: List<LocalAudioTrack>,
    val albums: List<LocalAlbum>,
) : Serializable {
    val trackCount: Int get() = tracks.size
}

data class LocalScanResult(
    val tracks: List<LocalAudioTrack> = emptyList(),
    val folders: List<LocalFolder> = emptyList(),
    val artists: List<LocalArtist> = emptyList(),
    val albums: List<LocalAlbum> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null,
) : Serializable

enum class LocalSortOrder {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    DURATION_DESC,
    DATE_MODIFIED_DESC,
}
