package com.commit451.drebin451.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

private val DrebinGradientCardShape = RoundedCornerShape(28.dp)

@Composable
private fun drebinGradientColors(): DrebinGradientColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val card = if (dark) {
        lerp(scheme.surfaceContainerHighest, scheme.primaryContainer, 0.18f)
    } else {
        lerp(Color.White, scheme.surfaceContainerHighest, 0.74f)
    }
    return DrebinGradientColors(
        dark = dark,
        backgroundTop = scheme.primaryContainer,
        backgroundBottom = scheme.secondaryContainer,
        card = card,
        onCard = scheme.onSurface,
        fabContainer = if (dark) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.92f),
        fabContent = if (dark) scheme.background else scheme.primary,
        navigationContainer = if (dark) {
            scheme.surfaceContainer.copy(alpha = 0.72f)
        } else {
            Color.White.copy(alpha = 0.62f)
        },
    )
}

@Composable
internal fun drebinGradientBrush(): Brush {
    val colors = drebinGradientColors()
    return Brush.verticalGradient(
        colors = listOf(
            colors.backgroundTop,
            colors.backgroundBottom,
        ),
    )
}

@Composable
internal fun DrebinGradientScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    contentUnderlapsNavigationBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(drebinGradientBrush()),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            content = { padding ->
                content(
                    if (contentUnderlapsNavigationBar) {
                        padding.withoutBottom()
                    } else {
                        padding
                    },
                )
            },
        )
    }
}

@Composable
private fun PaddingValues.withoutBottom(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = 0.dp,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DrebinGradientCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = drebinGradientColors()
    val cardColors = CardDefaults.cardColors(
        containerColor = colors.card,
        contentColor = colors.onCard,
    )
    val cardElevation =
        CardDefaults.cardElevation(defaultElevation = if (colors.dark) 8.dp else 6.dp)
    if (onLongClick != null) {
        Card(
            modifier = modifier.combinedClickable(
                enabled = enabled,
                onClick = onClick ?: {},
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
            ),
            shape = DrebinGradientCardShape,
            colors = cardColors,
            elevation = cardElevation,
            content = content,
        )
    } else if (onClick == null) {
        Card(
            modifier = modifier,
            shape = DrebinGradientCardShape,
            colors = cardColors,
            elevation = cardElevation,
            content = content,
        )
    } else {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = DrebinGradientCardShape,
            colors = cardColors,
            elevation = cardElevation,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrebinTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
)

@Composable
internal fun DrebinNavigationBarContainerColor(): Color = drebinGradientColors().navigationContainer

@Composable
internal fun DrebinFabContainerColor(): Color = drebinGradientColors().fabContainer

@Composable
internal fun DrebinFabContentColor(): Color = drebinGradientColors().fabContent
