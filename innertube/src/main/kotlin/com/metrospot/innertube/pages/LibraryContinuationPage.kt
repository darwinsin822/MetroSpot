package com.metrospot.innertube.pages

import com.metrospot.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
