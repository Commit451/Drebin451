package com.commit451.drebin451

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.commit451.drebin451.navigation.AppNavHost
import com.commit451.drebin451.navigation.rememberAppBackStack
import com.commit451.drebin451.navigation.rememberAppNavigator
import com.commit451.drebin451.ui.drebinColorScheme
import drebin451.app.shared.generated.resources.Res
import drebin451.app.shared.generated.resources.asimovian_regular
import org.jetbrains.compose.resources.Font

@Composable
fun App() {
    // App icons are loaded over the network (App.imageUrl), so the singleton ImageLoader needs a
    // network fetcher. The Ktor one works on every target and reuses each platform's Ktor engine.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = drebinColorScheme(darkTheme),
        typography = appTypography(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // The back stack starts on the splash, which fetches config + resolves auth and
            // then replaces itself with either the login or home route.
            val backStack = rememberAppBackStack()
            val navigator = rememberAppNavigator(backStack)
            AppNavHost(backStack = backStack, navigator = navigator)
        }
    }
}

@Composable
private fun appTypography(): Typography {
    val base = Typography()
    val appBarTitleFontFamily = FontFamily(Font(Res.font.asimovian_regular))
    return base.copy(
        titleLarge = base.titleLarge.copy(fontFamily = appBarTitleFontFamily),
    )
}
