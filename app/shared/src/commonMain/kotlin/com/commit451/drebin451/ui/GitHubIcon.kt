package com.commit451.drebin451.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import drebin451.app.shared.generated.resources.Res
import drebin451.app.shared.generated.resources.github_mark
import org.jetbrains.compose.resources.painterResource

@Composable
fun GitHubIcon(
    modifier: Modifier = Modifier,
    url: String = "https://github.com/Commit451/Drebin451",
) {
    val uriHandler = LocalUriHandler.current
    Image(
        painter = painterResource(Res.drawable.github_mark),
        contentDescription = "View on GitHub",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clickable { uriHandler.openUri(url) },
    )
}
