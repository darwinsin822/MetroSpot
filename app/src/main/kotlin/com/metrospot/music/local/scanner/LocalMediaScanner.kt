/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.local.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.metrospot.music.local.model.LocalAlbum
import com.metrospot.music.local.model.LocalArtist
import com.metrospot.music.local.model.LocalAudioTrack
import com.metrospot.music.local.model.LocalFolder
import com.metrospot.music.local.model.LocalScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

object LocalMediaScanner {

    private val _scanState = MutableStateFlow(LocalScanResult())
    val scanState: StateFlow<LocalScanResult> = _scanState.asStateFlow()

    private val supportedExtensions = setOf(
        "mp3", "flac", "wav", "ogg", "m4a", "aac", "opus", "wma"
    )

    suspend fun scanDeviceAudio(context: Context): LocalScanResult = withContext(Dispatchers.IO) {
        _scanState.value = _scanState.value.copy(isScanning = true, error = null)

        val tracks = mutableListOf<LocalAudioTrack>()
        val seenPaths = HashSet<String>()
        val resolver: ContentResolver = context.contentResolver

        // 1. Escaneo mediante MediaStore
        try {
            val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_MODIFIED
            )

            // Filtrar audios de más de 3 segundos para ignorar tonos de llamada breves
            val selection = "${MediaStore.Audio.Media.DURATION} > 3000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            resolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dateModCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else -1L
                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                    if (path.isNullOrBlank()) continue

                    val file = File(path)
                    val parentFolder = file.parentFile
                    val folderName = parentFolder?.name ?: "Almacenamiento"
                    val folderPath = parentFolder?.absolutePath ?: "/storage/emulated/0"

                    val titleRaw = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val artistRaw = if (artistCol >= 0) cursor.getString(artistCol) else null
                    val albumRaw = if (albumCol >= 0) cursor.getString(albumCol) else null
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val dateModified = if (dateModCol >= 0) cursor.getLong(dateModCol) else 0L

                    val cleanTitle = if (!titleRaw.isNullOrBlank() && titleRaw != "<unknown>") {
                        titleRaw.trim()
                    } else {
                        file.nameWithoutExtension
                    }

                    val cleanArtist = if (!artistRaw.isNullOrBlank() && artistRaw != "<unknown>") {
                        artistRaw.trim()
                    } else {
                        "Artista Desconocido"
                    }

                    val cleanAlbum = if (!albumRaw.isNullOrBlank() && albumRaw != "<unknown>") {
                        albumRaw.trim()
                    } else {
                        folderName
                    }

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    seenPaths.add(path)
                    tracks.add(
                        LocalAudioTrack(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            albumId = albumId,
                            durationMs = duration,
                            path = path,
                            folderName = folderName,
                            folderPath = folderPath,
                            dateModified = dateModified,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Manejo silencioso con log / continuar a carpetas
        }

        // 2. Escaneo complementario de carpetas estándar (para archivos nuevos o no indexados)
        val candidateDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            File("/storage/emulated/0/Music"),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Audio"),
            File("/storage/emulated/0/Recordings"),
            File("/storage/emulated/0/Documents/MetroSpot")
        )

        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryRecursively(dir, tracks, seenPaths, depth = 0)
            }
        }

        // 3. Agrupación y Estructuración de Modelos
        val sortedTracks = tracks.sortedBy { it.title.lowercase() }

        val folderMap = mutableMapOf<String, MutableList<LocalAudioTrack>>()
        for (track in sortedTracks) {
            folderMap.getOrPut(track.folderPath) { mutableListOf() }.add(track)
        }
        val folders = folderMap.map { (path, folderTracks) ->
            LocalFolder(
                name = folderTracks.firstOrNull()?.folderName ?: File(path).name,
                path = path,
                tracks = folderTracks
            )
        }.sortedBy { it.name.lowercase() }

        val albumMap = mutableMapOf<String, MutableList<LocalAudioTrack>>()
        for (track in sortedTracks) {
            val key = "${track.album}_${track.artist}"
            albumMap.getOrPut(key) { mutableListOf() }.add(track)
        }
        val albums = albumMap.map { (_, albumTracks) ->
            val first = albumTracks.first()
            LocalAlbum(
                id = first.albumId,
                title = first.album,
                artist = first.artist,
                albumArtUri = first.albumArtUri,
                tracks = albumTracks
            )
        }.sortedBy { it.title.lowercase() }

        val artistMap = mutableMapOf<String, MutableList<LocalAudioTrack>>()
        for (track in sortedTracks) {
            artistMap.getOrPut(track.artist) { mutableListOf() }.add(track)
        }
        val artists = artistMap.map { (artistName, artistTracks) ->
            val artistAlbumList = albums.filter { it.artist.equals(artistName, ignoreCase = true) }
            LocalArtist(
                name = artistName,
                tracks = artistTracks,
                albums = artistAlbumList
            )
        }.sortedBy { it.name.lowercase() }

        val result = LocalScanResult(
            tracks = sortedTracks,
            folders = folders,
            artists = artists,
            albums = albums,
            isScanning = false,
            error = null
        )

        _scanState.value = result
        return@withContext result
    }

    private fun scanDirectoryRecursively(
        dir: File,
        tracks: MutableList<LocalAudioTrack>,
        seenPaths: MutableSet<String>,
        depth: Int
    ) {
        if (depth > 4) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryRecursively(file, tracks, seenPaths, depth + 1)
            } else if (file.isFile) {
                val path = file.absolutePath
                if (seenPaths.contains(path)) continue

                val ext = file.extension.lowercase()
                if (ext in supportedExtensions) {
                    seenPaths.add(path)
                    var title = file.nameWithoutExtension
                    var artist = "Artista Local"
                    var album = dir.name
                    var durationMs = 0L

                    try {
                        MediaMetadataRetriever().use { mmr ->
                            mmr.setDataSource(path)
                            val metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            val metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            val metaAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            val metaDur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                            if (!metaTitle.isNullOrBlank()) title = metaTitle.trim()
                            if (!metaArtist.isNullOrBlank()) artist = metaArtist.trim()
                            if (!metaAlbum.isNullOrBlank()) album = metaAlbum.trim()
                            if (!metaDur.isNullOrBlank()) durationMs = metaDur.toLongOrNull() ?: 0L
                        }
                    } catch (_: Exception) {}

                    tracks.add(
                        LocalAudioTrack(
                            id = file.hashCode().toLong(),
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = -1L,
                            durationMs = durationMs,
                            path = path,
                            folderName = dir.name,
                            folderPath = dir.absolutePath,
                            dateModified = file.lastModified() / 1000L,
                            albumArtUri = null
                        )
                    )
                }
            }
        }
    }
}
