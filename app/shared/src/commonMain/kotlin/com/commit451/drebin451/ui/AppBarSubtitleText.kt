package com.commit451.drebin451.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import drebin451.app.shared.generated.resources.Res
import drebin451.app.shared.generated.resources.geo_regular
import org.jetbrains.compose.resources.Font

@Composable
internal fun AppBarTitleText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
internal fun AppBarSubtitleText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
        fontFamily = FontFamily(Font(Res.font.geo_regular)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = overflow,
    )
}
