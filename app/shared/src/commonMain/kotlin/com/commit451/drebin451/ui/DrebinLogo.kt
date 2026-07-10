package com.commit451.drebin451.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import drebin451.app.shared.generated.resources.Res
import drebin451.app.shared.generated.resources.drebin451_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun DrebinLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Drebin451 logo",
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painterResource(Res.drawable.drebin451_logo),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
