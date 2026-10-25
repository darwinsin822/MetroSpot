/**
 * MetroSpot Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrospot.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

// Verde característico de Spotify
val MetroSpotGreen = Color(0xFF1ED760)
val DefaultThemeColor = MetroSpotGreen

val MetroSpotDarkColorScheme = darkColorScheme(
    primary = MetroSpotGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF14833B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF1DB954),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color.White,
    tertiary = MetroSpotGreen,
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerLow = Color(0xFF151515),
    surfaceContainerLowest = Color(0xFF0C0C0C),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
)

@Composable
fun MetroSpotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDefaultMetroSpot = (themeColor == DefaultThemeColor || themeColor == Color(0xFF1DB954))
    val isSystemDynamic = (themeColor == Color.Transparent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val baseColorScheme = when {
        isSystemDynamic -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDefaultMetroSpot && darkTheme -> {
            MetroSpotDarkColorScheme
        }
        else -> {
            val seed = if (isDefaultMetroSpot) DefaultThemeColor else themeColor
            rememberDynamicColorScheme(
                seedColor = seed,
                isDark = darkTheme,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                style = PaletteStyle.TonalSpot
            )
        }
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
