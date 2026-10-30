/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.regex.Pattern

data class SpotifyTrack(
    val spotifyId: String,
    val title: String,
    val artists: String,
    val durationMs: Long,
    val isrc: String? = null,
)

data class SpotifyPlaylist(
    val id: String,
    val title: String,
    val creator: String,
    val coverUrl: String?,
    val tracks: List<SpotifyTrack>,
)

object SpotifyPlaylistExtractor {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            engine {
                config {
                    followRedirects(true)
                    followSslRedirects(true)
                }
            }
        }
    }

    private val jsonParser by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private val PLAYLIST_URL_REGEX = Pattern.compile(
        """(?:https?://(?:open\.)?spotify\.com/(?:intl-[a-z]{2}/)?playlist/|spotify:playlist:)([a-zA-Z0-9]{22})""",
    )

    suspend fun resolvePlaylistId(rawUrl: String): String? = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        val matcher = PLAYLIST_URL_REGEX.matcher(trimmed)
        if (matcher.find()) {
            return@withContext matcher.group(1)
        }

        if (trimmed.contains("spotify.link/") || trimmed.contains("spoti.fi/")) {
            try {
                val response = client.get(trimmed) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }
                val finalUrl = response.call.request.url.toString()
                val redirectedMatcher = PLAYLIST_URL_REGEX.matcher(finalUrl)
                if (redirectedMatcher.find()) {
                    return@withContext redirectedMatcher.group(1)
                }
            } catch (_: Exception) {
            }
        }

        null
    }

    suspend fun fetchFromEmbed(playlistId: String): SpotifyPlaylist = withContext(Dispatchers.IO) {
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        val responseText = client.get(embedUrl) {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header("Accept-Language", "en-US,en;q=0.9,es;q=0.8")
        }.bodyAsText()

        val nextDataPattern = Pattern.compile(
            """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""",
            Pattern.DOTALL,
        )
        val matcher = nextDataPattern.matcher(responseText)
        if (!matcher.find()) {
            throw IllegalStateException("No se pudo obtener la estructura de la playlist de Spotify.")
        }

        val jsonString = matcher.group(1)
        val rootElement = jsonParser.parseToJsonElement(jsonString).jsonObject
        val entity = rootElement["props"]?.jsonObject
            ?.get("pageProps")?.jsonObject
            ?.get("state")?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("entity")?.jsonObject
            ?: throw IllegalStateException("Estructura de datos de Spotify no reconocida.")

        val title = entity["title"]?.jsonPrimitive?.contentOrNull
            ?: entity["name"]?.jsonPrimitive?.contentOrNull
            ?: "Spotify Playlist"
        val creator = entity["subtitle"]?.jsonPrimitive?.contentOrNull
            ?: "Spotify"

        var coverUrl: String? = null
        val visualIdentity = entity["visualIdentity"]?.jsonObject
        val images = visualIdentity?.get("image")?.jsonArray
        if (images != null && images.isNotEmpty()) {
            coverUrl = images.last().jsonObject["url"]?.jsonPrimitive?.contentOrNull
        }
        if (coverUrl == null) {
            val coverArt = entity["coverArt"]?.jsonObject
            val sources = coverArt?.get("sources")?.jsonArray
            if (sources != null && sources.isNotEmpty()) {
                coverUrl = sources.last().jsonObject["url"]?.jsonPrimitive?.contentOrNull
            }
        }

        val trackListArray = entity["trackList"]?.jsonArray ?: emptyList()
        val tracks = mutableListOf<SpotifyTrack>()

        for (item in trackListArray) {
            val trackObj = item.jsonObject
            val trackTitle = trackObj["title"]?.jsonPrimitive?.contentOrNull ?: continue
            val trackArtists = trackObj["subtitle"]?.jsonPrimitive?.contentOrNull ?: ""
            val duration = trackObj["duration"]?.jsonPrimitive?.longOrNull ?: 0L
            val uri = trackObj["uri"]?.jsonPrimitive?.contentOrNull ?: ""
            val spotifyId = uri.substringAfterLast(":", "")

            tracks.add(
                SpotifyTrack(
                    spotifyId = spotifyId,
                    title = trackTitle,
                    artists = trackArtists,
                    durationMs = duration,
                ),
            )
        }

        SpotifyPlaylist(
            id = playlistId,
            title = title,
            creator = creator,
            coverUrl = coverUrl,
            tracks = tracks,
        )
    }
}
