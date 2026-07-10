package com.commit451.drebin451.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.commit451.drebin451.ui.ContentLayout.MaxContentWidth

/**
 * Responsive horizontal insets for screen content.
 *
 * Phones get a small fixed gutter on each side. Once the viewport is wider than [MaxContentWidth] —
 * tablets, and the web build in anything but a narrow window — the surplus width is split into equal
 * left/right margins instead of letting the content stretch, so it keeps a comfortable reading width
 * and sits centered.
 */
internal object ContentLayout {
    /** Gutter on each side at phone widths; also the floor for the margin at any width. */
    val DefaultHorizontalMargin: Dp = 16.dp

    /** Content stops widening past this; wider viewports turn the surplus into margin. */
    val MaxContentWidth: Dp = 640.dp

    /**
     * Horizontal margin to apply for a given available [width]. Stays at [minimum] until the
     * viewport grows past [MaxContentWidth] + 2·[minimum], then increases so the content stays
     * centered at [MaxContentWidth].
     */
    fun horizontalMargin(width: Dp, minimum: Dp = DefaultHorizontalMargin): Dp =
        ((width - MaxContentWidth) / 2).coerceAtLeast(minimum)
}
