package com.commit451.drebin451.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val DrebinIconSteel = Color(0xff6f8792)
private val DrebinIconInk = Color(0xff050505)

internal fun drebinColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
    darkColorScheme(
        primary = Color(0xffa8c8d4),
        onPrimary = Color(0xff0b2932),
        primaryContainer = Color(0xff203942),
        onPrimaryContainer = Color(0xffd5f1fb),
        secondary = Color(0xffc1c9c8),
        onSecondary = Color(0xff253031),
        secondaryContainer = Color(0xff333d3d),
        onSecondaryContainer = Color(0xffe1e8e7),
        tertiary = Color(0xfff0f0ef),
        onTertiary = DrebinIconInk,
        tertiaryContainer = Color(0xff242928),
        onTertiaryContainer = Color(0xfff4f4f3),
        background = Color(0xff0d1517),
        onBackground = Color(0xffedf5f6),
        surface = Color(0xff10191b),
        onSurface = Color(0xffedf5f6),
        surfaceVariant = Color(0xff3c494b),
        onSurfaceVariant = Color(0xffc4d1d3),
        surfaceContainerLowest = Color(0xff080f10),
        surfaceContainerLow = Color(0xff111c1e),
        surfaceContainer = Color(0xff172426),
        surfaceContainerHigh = Color(0xff223033),
        surfaceContainerHighest = Color(0xff2c3b3f),
        outline = Color(0xff8b9a9d),
        outlineVariant = Color(0xff3f4d50),
    )
} else {
    lightColorScheme(
        primary = DrebinIconSteel,
        onPrimary = Color.White,
        primaryContainer = Color(0xffbfd8e1),
        onPrimaryContainer = Color(0xff082831),
        secondary = Color(0xff747f7f),
        onSecondary = Color.White,
        secondaryContainer = Color(0xffdce2e1),
        onSecondaryContainer = Color(0xff202a2b),
        tertiary = DrebinIconInk,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xffd6d8d8),
        onTertiaryContainer = Color.Black,
        background = Color(0xffeff6f7),
        onBackground = Color(0xff101819),
        surface = Color(0xfff8fcfc),
        onSurface = Color(0xff101819),
        surfaceVariant = Color(0xffd5e0e0),
        onSurfaceVariant = Color(0xff415052),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xffeff5f5),
        surfaceContainer = Color(0xffe5eeee),
        surfaceContainerHigh = Color(0xffdbe6e7),
        surfaceContainerHighest = Color(0xffd0dcde),
        outline = Color(0xff657477),
        outlineVariant = Color(0xffb7c4c6),
    )
}
