/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.local.model

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.metrospot.music.models.MediaMetadata
import java.io.File
import java.io.Serializable

data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = -1L,
    val durationMs: Long,
    val path: String,
    val folderName: String,
    val folderPath: String,
    val dateModified: Long = 0L,
    val albumArtUri: String? = null,
) : Serializable {

    val contentUriString: String
        get() = if (id > 0) {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
        } else {
            Uri.fromFile(File(path)).toString()
        }

    fun toMediaMetadata(): MediaMetadata {
        return MediaMetadata(
            id = contentUriString,
            title = title,
            artists = listOf(
                MediaMetadata.Artist(
                    id = "local_artist_${artist.hashCode()}",
                    name = artist,
                )
            ),
            duration = (durationMs / 1000).toInt().coerceAtLeast(0),
            thumbnailUrl = albumArtUri,
            album = MediaMetadata.Album(
                id = "local_album_${if (albumId > 0) albumId else album.hashCode()}",
                title = album,
            ),
        )
    }

    fun toMediaItem(): MediaItem {
        val metadata = toMediaMetadata()
        return MediaItem.Builder()
            .setMediaId(metadata.id)
            .setUri(metadata.id)
            .setCustomCacheKey(metadata.id)
            .setTag(metadata)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setDisplayTitle(title)
                    .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
                    .setMediaType(MEDIA_TYPE_MUSIC)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setExtras(Bundle().apply {
                        albumArtUri?.let { putString("artwork_uri", it) }
                        putString("folder_name", folderName)
                        putString("folder_path", folderPath)
                    })
                    .build()
            )
            .build()
    }
}
