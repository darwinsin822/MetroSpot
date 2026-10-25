package com.metrospot.innertube.pages

import com.metrospot.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
