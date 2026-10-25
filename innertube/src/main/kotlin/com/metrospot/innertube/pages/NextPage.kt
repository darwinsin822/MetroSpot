package com.metrospot.innertube.pages

import com.metrospot.innertube.models.Album
import com.metrospot.innertube.models.Artist
import com.metrospot.innertube.models.BrowseEndpoint
import com.metrospot.innertube.models.PlaylistPanelVideoRenderer
import com.metrospot.innertube.models.SongItem
import com.metrospot.innertube.models.WatchEndpoint
import com.metrospot.innertube.models.oddElements
import com.metrospot.innertube.models.splitBySeparator
import com.metrospot.innertube.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = renderer.longBylineText?.runs?.splitBySeparator() ?: return null

        // Extract library tokens using the new method that properly handles multiple toggle items
        val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

        return SongItem(
            id = renderer.videoId
                ?: renderer.navigationEndpoint.watchEndpoint?.videoId
                ?: return null,
            title =
                renderer.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists =
                longByLineRuns.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                } ?: return null,
            album =
                longByLineRuns
                    .getOrNull(1)
                    ?.firstOrNull()
                    ?.takeIf {
                        it.navigationEndpoint?.browseEndpoint != null
                    }?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                        )
                    },
            duration =
                renderer.lengthText
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                    ?.parseTime() ?: return null,
            musicVideoType = renderer.navigationEndpoint.musicVideoType,
            thumbnail =
                renderer.thumbnail.thumbnails
                    .lastOrNull()
                    ?.url ?: return null,
            explicit =
                renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
            libraryAddToken = libraryTokens.addToken,
            libraryRemoveToken = libraryTokens.removeToken
        )
    }
}
