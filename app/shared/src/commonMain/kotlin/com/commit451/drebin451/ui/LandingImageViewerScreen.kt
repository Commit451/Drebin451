package com.commit451.drebin451.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.commit451.drebin451.navigation.LandingImageViewerRoute
import com.commit451.drebin451.navigation.LandingScreenshot
import com.commit451.drebin451.navigation.LocalAppNavigator
import drebin451.app.shared.generated.resources.Res
import drebin451.app.shared.generated.resources.landing_app_releases_screen
import drebin451.app.shared.generated.resources.landing_home_screen
import drebin451.app.shared.generated.resources.landing_release_detail_screen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal val LandingScreenshot.resource: DrawableResource
    get() = when (this) {
        LandingScreenshot.AppsList -> Res.drawable.landing_home_screen
        LandingScreenshot.AppReleases -> Res.drawable.landing_app_releases_screen
        LandingScreenshot.ReleaseDetails -> Res.drawable.landing_release_detail_screen
    }

internal val LandingScreenshot.contentDescription: String
    get() = when (this) {
        LandingScreenshot.AppsList -> "Drebin451 apps list"
        LandingScreenshot.AppReleases -> "Drebin451 app release history"
        LandingScreenshot.ReleaseDetails -> "Drebin451 release details"
    }

@Composable
internal fun LandingImageViewerScreen(route: LandingImageViewerRoute) {
    val navigator = LocalAppNavigator.current
    var isVisible by remember { mutableStateOf(false) }
    val scrimProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "Landing image viewer scrim",
    )
    val imageProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "Landing image viewer image",
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f * scrimProgress))
            .safeContentPadding(),
    ) {
        Image(
            painter = painterResource(route.screenshot.resource),
            contentDescription = route.screenshot.contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .graphicsLayer {
                    alpha = imageProgress
                    scaleX = 0.88f + (0.12f * imageProgress)
                    scaleY = 0.88f + (0.12f * imageProgress)
                }
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .graphicsLayer { alpha = scrimProgress },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.68f),
            contentColor = Color.White,
        ) {
            IconButton(
                onClick = navigator::pop,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close fullscreen image viewer",
                )
            }
        }
    }
}
