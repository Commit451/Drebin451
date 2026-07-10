package com.commit451.drebin451.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.navigation.HomeRoute
import com.commit451.drebin451.navigation.LocalAppNavigator

@Composable
fun SplashScreen() {
    val navigator = LocalAppNavigator.current
    val viewModel: SplashViewModel = viewModel { SplashViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showSplashContent = shouldShowSplashScreenContent()

    LaunchedEffect(state.shouldNavigate, state.loggedOutRoute) {
        val loggedOutRoute = state.loggedOutRoute
        when {
            loggedOutRoute != null -> navigator.replaceAll(loggedOutRoute)
            state.shouldNavigate -> navigator.replaceAll(HomeRoute)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val error = state.error
        if (error != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    if (state.canRetry) "Something went wrong" else "Drebin451 unavailable",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (state.canRetry) {
                    Button(onClick = { viewModel.retry() }) { Text("Retry") }
                }
            }
        } else if (showSplashContent) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DrebinLogo(modifier = Modifier.size(112.dp))
                Spacer(Modifier.height(16.dp))
                Text("Drebin451", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }
    }
}
