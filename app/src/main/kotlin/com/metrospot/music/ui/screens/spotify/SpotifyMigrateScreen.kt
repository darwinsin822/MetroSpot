/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.ui.screens.spotify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrospot.music.LocalPlayerAwareWindowInsets
import com.metrospot.music.R
import com.metrospot.music.spotify.MigrateUiState
import com.metrospot.music.spotify.SpotifyMigrateViewModel
import com.metrospot.music.spotify.TrackMigrateItem
import com.metrospot.music.spotify.TrackMigrateStatus

private val SpotifyBgDark = Color(0xFF0A0D0C)
private val SpotifyCardSurface = Color(0xFF141817)
private val SpotifyCardBorder = Color(0xFF232A27)
private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyGreenDark = Color(0xFF134625)
private val SpotifyTextWhite = Color(0xFFFFFFFF)
private val SpotifyTextGray = Color(0xFFAAAAAA)

@Composable
fun SpotifyMigrateScreen(
    navController: NavController,
    viewModel: SpotifyMigrateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var urlInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBgDark)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Header TopAppBar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SpotifyCardSurface)
                        .border(1.dp, SpotifyCardBorder, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Volver",
                        tint = SpotifyTextWhite,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Migrar Playlist",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextWhite,
                )
            }

            // Tarjeta Superior de Instrucciones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SpotifyCardSurface)
                    .border(1.dp, SpotifyCardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreenDark),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pega el enlace de tu playlist",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = SpotifyTextWhite,
                        )
                    }

                    // Única insignia destacada Spotify
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpotifyGreenDark.copy(alpha = 0.5f))
                            .border(1.dp, SpotifyGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Spotify Playlists",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SpotifyGreen,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Copia el enlace de cualquier playlist pública de Spotify para importar sus canciones a MetroSpot y descargarlas para reproducción offline.",
                    fontSize = 13.sp,
                    color = SpotifyTextGray,
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de URL + Botón Pegar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(SpotifyBgDark)
                        .border(1.dp, SpotifyCardBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.link),
                        contentDescription = null,
                        tint = SpotifyTextGray,
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = {
                            Text(
                                "https://open.spotify.com/playlist/...",
                                fontSize = 13.sp,
                                color = SpotifyTextGray.copy(alpha = 0.6f),
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = SpotifyTextWhite,
                            unfocusedTextColor = SpotifyTextWhite,
                            cursorColor = SpotifyGreen,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )

                    Button(
                        onClick = {
                            clipboardManager.getText()?.text?.let { text ->
                                urlInput = text.trim()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyCardSurface,
                            contentColor = SpotifyGreen,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text("Pegar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Acción Principal: Analizar lista
                val isExtracting = uiState is MigrateUiState.Extracting
                Button(
                    onClick = { viewModel.analyzePlaylistUrl(urlInput) },
                    enabled = !isExtracting && urlInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF196837),
                        contentColor = SpotifyTextWhite,
                        disabledContainerColor = Color(0xFF196837).copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            color = SpotifyTextWhite,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Analizando lista...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = SpotifyTextWhite,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Analizar lista", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido de la Playlist Analizada
            when (val state = uiState) {
                is MigrateUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF3B1515))
                            .border(1.dp, Color(0xFF6B2222), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = state.message,
                            color = Color(0xFFFF8888),
                            fontSize = 14.sp,
                        )
                    }
                }

                is MigrateUiState.Ready -> {
                    PlaylistPreviewCard(
                        state = state,
                        onDownloadClick = { viewModel.startMigrationAndDownload() },
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun PlaylistPreviewCard(
    state: MigrateUiState.Ready,
    onDownloadClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SpotifyCardSurface)
            .border(1.dp, SpotifyCardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        // Encabezado de la Playlist
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = state.playlist.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpotifyBgDark),
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.playlist.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SpotifyTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.playlist.creator} • ${state.totalCount} canciones",
                    fontSize = 13.sp,
                    color = SpotifyTextGray,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Barra de progreso y contador si está migrando
        AnimatedVisibility(visible = state.isMigrating || state.processedCount > 0) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Procesadas: ${state.processedCount} / ${state.totalCount}",
                        fontSize = 12.sp,
                        color = SpotifyGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${if (state.totalCount > 0) (state.processedCount * 100 / state.totalCount) else 0}%",
                        fontSize = 12.sp,
                        color = SpotifyTextGray,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (state.totalCount > 0) state.processedCount.toFloat() / state.totalCount else 0f },
                    color = SpotifyGreen,
                    trackColor = SpotifyCardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }

        // Botón Descargar
        Button(
            onClick = onDownloadClick,
            enabled = !state.isMigrating,
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyGreen,
                contentColor = Color.Black,
                disabledContainerColor = SpotifyGreen.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isMigrating) "Migrando y Descargando..." else "Descargar en MetroSpot",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Lista de canciones con estados
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.items) { index, item ->
                TrackItemRow(index = index + 1, item = item)
            }
        }
    }
}

@Composable
private fun TrackItemRow(
    index: Int,
    item: TrackMigrateItem,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpotifyBgDark.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "$index",
            fontSize = 12.sp,
            color = SpotifyTextGray,
            modifier = Modifier.width(24.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.spotifyTrack.title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = SpotifyTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.spotifyTrack.artists,
                fontSize = 12.sp,
                color = SpotifyTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge de estado
        when (item.status) {
            TrackMigrateStatus.PENDING -> {
                Text(
                    text = "Pendiente",
                    fontSize = 11.sp,
                    color = SpotifyTextGray,
                )
            }
            TrackMigrateStatus.SEARCHING -> {
                CircularProgressIndicator(
                    color = SpotifyGreen,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            }
            TrackMigrateStatus.DOWNLOADING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = SpotifyGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Descargando",
                        fontSize = 11.sp,
                        color = SpotifyGreen,
                    )
                }
            }
            TrackMigrateStatus.COMPLETED -> {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
            TrackMigrateStatus.NOT_FOUND -> {
                Text(
                    text = "No hallada",
                    fontSize = 11.sp,
                    color = Color(0xFFFF6666),
                )
            }
        }
    }
}
