package com.commit451.drebin451.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.commit451.drebin451.api.createHttpClient
import com.commit451.drebin451.model.App
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.palette.graphics.Palette
import com.kmpalette.rememberPaletteState
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.Url
import kotlin.math.max
import kotlin.math.min

private const val AppThemeTransitionDurationMillis = 650

@Composable
internal fun AppIconTheme(
    app: App?,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val iconUrl = remember(app?.imageUrl) { app?.imageUrl?.toUrlOrNull() }
    val httpClient = remember { createHttpClient() }
    val requestBuilder = remember { HttpRequestBuilder() }
    val networkLoader = rememberNetworkLoader(
        httpClient = httpClient,
        requestBuilder = requestBuilder,
    )
    val paletteState = rememberPaletteState(loader = networkLoader)

    DisposableEffect(httpClient) {
        onDispose { httpClient.close() }
    }

    LaunchedEffect(iconUrl) {
        if (iconUrl == null) {
            paletteState.reset()
        } else {
            paletteState.generate(iconUrl)
        }
    }

    val targetColorScheme = remember(baseColorScheme, iconUrl, paletteState.palette) {
        if (iconUrl == null) {
            baseColorScheme
        } else {
            paletteState.palette?.toAppColorScheme(baseColorScheme) ?: baseColorScheme
        }
    }
    val appColorScheme = animateColorScheme(targetColorScheme)

    MaterialTheme(
        colorScheme = appColorScheme,
        typography = typography,
        content = content,
    )
}

private fun String.toUrlOrNull(): Url? =
    takeIf { it.isNotBlank() }?.let { value -> runCatching { Url(value) }.getOrNull() }

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val transition = updateTransition(
        targetState = target,
        label = "app icon color scheme",
    )

    @Composable
    fun animatedColor(
        label: String,
        targetValue: ColorScheme.() -> Color,
    ): Color = transition.animateColor(
        transitionSpec = {
            tween(
                durationMillis = AppThemeTransitionDurationMillis,
                easing = FastOutSlowInEasing,
            )
        },
        label = label,
        targetValueByState = { colorScheme -> colorScheme.targetValue() },
    ).value

    return target.copy(
        primary = animatedColor("primary") { primary },
        onPrimary = animatedColor("onPrimary") { onPrimary },
        primaryContainer = animatedColor("primaryContainer") { primaryContainer },
        onPrimaryContainer = animatedColor("onPrimaryContainer") { onPrimaryContainer },
        inversePrimary = animatedColor("inversePrimary") { inversePrimary },
        primaryFixed = animatedColor("primaryFixed") { primaryFixed },
        primaryFixedDim = animatedColor("primaryFixedDim") { primaryFixedDim },
        onPrimaryFixed = animatedColor("onPrimaryFixed") { onPrimaryFixed },
        onPrimaryFixedVariant = animatedColor("onPrimaryFixedVariant") { onPrimaryFixedVariant },

        secondary = animatedColor("secondary") { secondary },
        onSecondary = animatedColor("onSecondary") { onSecondary },
        secondaryContainer = animatedColor("secondaryContainer") { secondaryContainer },
        onSecondaryContainer = animatedColor("onSecondaryContainer") { onSecondaryContainer },
        secondaryFixed = animatedColor("secondaryFixed") { secondaryFixed },
        secondaryFixedDim = animatedColor("secondaryFixedDim") { secondaryFixedDim },
        onSecondaryFixed = animatedColor("onSecondaryFixed") { onSecondaryFixed },
        onSecondaryFixedVariant = animatedColor("onSecondaryFixedVariant") { onSecondaryFixedVariant },

        tertiary = animatedColor("tertiary") { tertiary },
        onTertiary = animatedColor("onTertiary") { onTertiary },
        tertiaryContainer = animatedColor("tertiaryContainer") { tertiaryContainer },
        onTertiaryContainer = animatedColor("onTertiaryContainer") { onTertiaryContainer },
        tertiaryFixed = animatedColor("tertiaryFixed") { tertiaryFixed },
        tertiaryFixedDim = animatedColor("tertiaryFixedDim") { tertiaryFixedDim },
        onTertiaryFixed = animatedColor("onTertiaryFixed") { onTertiaryFixed },
        onTertiaryFixedVariant = animatedColor("onTertiaryFixedVariant") { onTertiaryFixedVariant },

        background = animatedColor("background") { background },
        onBackground = animatedColor("onBackground") { onBackground },
        surface = animatedColor("surface") { surface },
        onSurface = animatedColor("onSurface") { onSurface },
        surfaceDim = animatedColor("surfaceDim") { surfaceDim },
        surfaceBright = animatedColor("surfaceBright") { surfaceBright },
        surfaceContainerLowest = animatedColor("surfaceContainerLowest") { surfaceContainerLowest },
        surfaceContainerLow = animatedColor("surfaceContainerLow") { surfaceContainerLow },
        surfaceContainer = animatedColor("surfaceContainer") { surfaceContainer },
        surfaceContainerHigh = animatedColor("surfaceContainerHigh") { surfaceContainerHigh },
        surfaceContainerHighest = animatedColor("surfaceContainerHighest") { surfaceContainerHighest },
        surfaceVariant = animatedColor("surfaceVariant") { surfaceVariant },
        onSurfaceVariant = animatedColor("onSurfaceVariant") { onSurfaceVariant },
        surfaceTint = animatedColor("surfaceTint") { surfaceTint },
        inverseSurface = animatedColor("inverseSurface") { inverseSurface },
        inverseOnSurface = animatedColor("inverseOnSurface") { inverseOnSurface },

        error = animatedColor("error") { error },
        onError = animatedColor("onError") { onError },
        errorContainer = animatedColor("errorContainer") { errorContainer },
        onErrorContainer = animatedColor("onErrorContainer") { onErrorContainer },
        outline = animatedColor("outline") { outline },
        outlineVariant = animatedColor("outlineVariant") { outlineVariant },
        scrim = animatedColor("scrim") { scrim },
    )
}

private fun Palette.toAppColorScheme(base: ColorScheme): ColorScheme {
    val dominant = dominantSwatch?.toColor()?.copy(alpha = 1f) ?: return base
    val darkTheme = base.background.luminance() < 0.5f
    val primarySource = firstColor(
        vibrantSwatch,
        dominantSwatch,
        darkVibrantSwatch,
        mutedSwatch,
    )?.copy(alpha = 1f) ?: dominant
    val secondarySource = firstColor(
        mutedSwatch,
        lightMutedSwatch,
        darkMutedSwatch,
        dominantSwatch,
    )?.copy(alpha = 1f) ?: dominant
    val tertiarySource = firstColor(
        lightVibrantSwatch,
        darkVibrantSwatch,
        darkMutedSwatch,
        dominantSwatch,
    )?.copy(alpha = 1f) ?: primarySource
    val primary = primarySource.asAccentOn(base.surface)
    val secondary = secondarySource.asAccentOn(base.surface)
    val tertiary = tertiarySource.asAccentOn(base.surface)
    val primaryContainer = primarySource.gradientStop(base.primaryContainer, darkTheme)
    val secondaryContainer = secondarySource.gradientStop(base.secondaryContainer, darkTheme)
    val tertiaryContainer = tertiarySource.gradientStop(base.tertiaryContainer, darkTheme)
    val highestSurface = secondarySource.cardSurface(base.surfaceContainerHighest, darkTheme)

    return base.copy(
        primary = primary,
        onPrimary = primary.contentColor(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contentColor(),
        inversePrimary = primary.asAccentOn(base.inverseSurface),
        primaryFixed = primary,
        primaryFixedDim = primaryContainer,
        onPrimaryFixed = primary.contentColor(),
        onPrimaryFixedVariant = primary.contentColor().copy(alpha = 0.82f),

        secondary = secondary,
        onSecondary = secondary.contentColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contentColor(),
        secondaryFixed = secondary,
        secondaryFixedDim = secondaryContainer,
        onSecondaryFixed = secondary.contentColor(),
        onSecondaryFixedVariant = secondary.contentColor().copy(alpha = 0.82f),

        tertiary = tertiary,
        onTertiary = tertiary.contentColor(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.contentColor(),
        tertiaryFixed = tertiary,
        tertiaryFixedDim = tertiaryContainer,
        onTertiaryFixed = tertiary.contentColor(),
        onTertiaryFixedVariant = tertiary.contentColor().copy(alpha = 0.82f),

        background = dominant.backgroundTint(base.background, darkTheme),
        surface = dominant.backgroundTint(base.surface, darkTheme),
        surfaceDim = dominant.subtleSurface(base.surfaceDim, darkTheme),
        surfaceBright = primarySource.subtleSurface(base.surfaceBright, darkTheme),
        surfaceContainerLowest = dominant.subtleSurface(base.surfaceContainerLowest, darkTheme),
        surfaceContainerLow = primarySource.subtleSurface(base.surfaceContainerLow, darkTheme),
        surfaceContainer = primarySource.cardSurface(base.surfaceContainer, darkTheme),
        surfaceContainerHigh = secondarySource.cardSurface(base.surfaceContainerHigh, darkTheme),
        surfaceContainerHighest = highestSurface,
        surfaceVariant = tertiarySource.cardSurface(base.surfaceVariant, darkTheme),
        surfaceTint = primary,
        outline = primary.tint(base.outline, if (darkTheme) 0.28f else 0.38f),
        outlineVariant = tertiary.tint(base.outlineVariant, if (darkTheme) 0.18f else 0.24f),
    )
}

private fun firstColor(vararg swatches: Palette.Swatch?): Color? =
    swatches.firstOrNull { it != null }?.toColor()

private fun Palette.Swatch.toColor(): Color = Color(rgb)

private fun Color.asAccentOn(background: Color): Color =
    copy(alpha = 1f)
        .coerceLuminance(min = 0.22f, max = 0.78f)
        .ensureContrast(background = background, minRatio = 3.0f)

private fun Color.gradientStop(base: Color, darkTheme: Boolean): Color = if (darkTheme) {
    tint(base, 0.68f).coerceLuminance(min = 0.05f, max = 0.23f)
} else {
    tint(base, 0.72f).coerceLuminance(min = 0.50f, max = 0.88f)
}

private fun Color.backgroundTint(base: Color, darkTheme: Boolean): Color = if (darkTheme) {
    tint(base, 0.22f).coerceLuminance(min = 0.03f, max = 0.16f)
} else {
    tint(base, 0.10f).coerceLuminance(min = 0.86f, max = 0.98f)
}

private fun Color.subtleSurface(base: Color, darkTheme: Boolean): Color = if (darkTheme) {
    tint(base, 0.16f).coerceLuminance(min = 0.04f, max = 0.22f)
} else {
    tint(base, 0.12f).coerceLuminance(min = 0.78f, max = 0.98f)
}

private fun Color.cardSurface(base: Color, darkTheme: Boolean): Color = if (darkTheme) {
    tint(base, 0.32f).coerceLuminance(min = 0.16f, max = 0.36f)
} else {
    tint(base, 0.30f).coerceLuminance(min = 0.68f, max = 0.94f)
}

private fun Color.tint(base: Color, amount: Float): Color =
    lerp(base.copy(alpha = 1f), copy(alpha = 1f), amount)

private fun Color.coerceLuminance(min: Float, max: Float): Color {
    var color = this
    repeat(8) {
        val luminance = color.luminance()
        color = when {
            luminance < min -> lerp(color, Color.White, 0.12f)
            luminance > max -> lerp(color, Color.Black, 0.10f)
            else -> return color
        }
    }
    return color
}

private fun Color.ensureContrast(background: Color, minRatio: Float): Color {
    var color = this
    repeat(8) {
        if (color.contrastAgainst(background) >= minRatio) return color
        color = if (background.luminance() < 0.5f) {
            lerp(color, Color.White, 0.14f)
        } else {
            lerp(color, Color.Black, 0.14f)
        }
    }
    return color
}

private fun Color.contentColor(): Color {
    val whiteContrast = Color.White.contrastAgainst(this)
    val blackContrast = Color.Black.contrastAgainst(this)
    return if (whiteContrast >= blackContrast) Color.White else Color.Black
}

private fun Color.contrastAgainst(background: Color): Float {
    val foregroundLuminance = luminance()
    val backgroundLuminance = background.luminance()
    val lighter = max(foregroundLuminance, backgroundLuminance)
    val darker = min(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}
