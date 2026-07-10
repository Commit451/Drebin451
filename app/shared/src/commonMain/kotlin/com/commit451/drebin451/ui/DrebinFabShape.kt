package com.commit451.drebin451.ui

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.unit.LayoutDirection

internal val DrebinFabShape = GenericShape { size, layoutDirection ->
    val radius = size.height * 0.42f

    if (layoutDirection == LayoutDirection.Ltr) {
        moveTo(0f, 0f)
        lineTo(size.width - radius, 0f)
        quadraticTo(size.width, 0f, size.width, radius)
        lineTo(size.width, size.height - radius)
        quadraticTo(size.width, size.height, size.width - radius, size.height)
        lineTo(radius, size.height)
        quadraticTo(0f, size.height, 0f, size.height - radius)
        lineTo(0f, 0f)
    } else {
        moveTo(radius, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height - radius)
        quadraticTo(size.width, size.height, size.width - radius, size.height)
        lineTo(radius, size.height)
        quadraticTo(0f, size.height, 0f, size.height - radius)
        lineTo(0f, radius)
        quadraticTo(0f, 0f, radius, 0f)
    }
    close()
}
