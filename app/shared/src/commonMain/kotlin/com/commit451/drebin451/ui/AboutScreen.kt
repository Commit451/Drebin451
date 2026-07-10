package com.commit451.drebin451.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commit451.drebin451.download.DREBIN451_LATEST_APK_DOWNLOAD_URL
import com.commit451.drebin451.download.shouldShowDrebin451ApkDownload
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.navigation.AboutRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.navigation.LoginRoute
import com.commit451.drebin451.navigation.PricingRoute

private val LandingFallbackColors = LandingColors(
    background = Color(0xFF0D1517),
    surface = Color(0xFF223033),
    surfaceAlt = Color(0xFF2C3B3F),
    stroke = Color(0xFF3F4D50),
    muted = Color(0xFFC4D1D3),
    text = Color(0xFFEDF5F6),
    accent = Color(0xFFA8C8D4),
    onAccent = Color(0xFF0B2932),
    orange = Color(0xFFF0F0EF),
    blue = Color(0xFFC1C9C8),
    green = Color(0xFFA8C8D4),
)
private val LocalLandingColors = staticCompositionLocalOf { LandingFallbackColors }

private val LandingBackground: Color
    @Composable get() = LocalLandingColors.current.background
private val LandingSurface: Color
    @Composable get() = LocalLandingColors.current.surface
private val LandingSurfaceAlt: Color
    @Composable get() = LocalLandingColors.current.surfaceAlt
private val LandingStroke: Color
    @Composable get() = LocalLandingColors.current.stroke
private val LandingMuted: Color
    @Composable get() = LocalLandingColors.current.muted
private val LandingText: Color
    @Composable get() = LocalLandingColors.current.text
private val LandingAccent: Color
    @Composable get() = LocalLandingColors.current.accent
private val LandingOnAccent: Color
    @Composable get() = LocalLandingColors.current.onAccent
private val LandingOrange: Color
    @Composable get() = LocalLandingColors.current.orange
private val LandingBlue: Color
    @Composable get() = LocalLandingColors.current.blue
private val LandingGreen: Color
    @Composable get() = LocalLandingColors.current.green

@Composable
private fun landingColors(): LandingColors {
    val scheme = MaterialTheme.colorScheme
    return LandingColors(
        background = scheme.background,
        surface = scheme.surfaceContainerHigh.copy(alpha = 0.84f),
        surfaceAlt = scheme.surfaceContainerHighest.copy(alpha = 0.90f),
        stroke = scheme.outlineVariant.copy(alpha = 0.78f),
        muted = scheme.onSurfaceVariant,
        text = scheme.onBackground,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        orange = scheme.tertiary,
        blue = scheme.secondary,
        green = scheme.primary,
    )
}

@Composable
fun AboutScreen() {
    val navigator = LocalAppNavigator.current
    val uriHandler = LocalUriHandler.current
    val onDownloadApk: (() -> Unit)? = if (shouldShowDrebin451ApkDownload()) {
        { uriHandler.openUri(DREBIN451_LATEST_APK_DOWNLOAD_URL) }
    } else {
        null
    }

    LandingPageChrome(
        currentPage = PublicPage.About,
        onHome = { },
        onPricing = { navigator.push(PricingRoute) },
        onLogIn = { navigator.push(LoginRoute()) },
        onSignUp = { navigator.push(LoginRoute(startOnSignUp = true)) },
    ) { compact ->
        HeroSection(
            compact = compact,
            onSignUp = { navigator.push(LoginRoute(startOnSignUp = true)) },
            onDownloadApk = onDownloadApk,
        )
        SectionSpacer()
        WorkflowSection(compact = compact)
        SectionSpacer()
        FeatureGrid(compact = compact)
    }
}

@Composable
fun PricingScreen() {
    val navigator = LocalAppNavigator.current

    LandingPageChrome(
        currentPage = PublicPage.Pricing,
        onHome = { navigator.replaceAll(AboutRoute) },
        onPricing = { },
        onLogIn = { navigator.push(LoginRoute()) },
        onSignUp = { navigator.push(LoginRoute(startOnSignUp = true)) },
    ) { compact ->
        PricingHero()
        Spacer(Modifier.height(28.dp))
        PricingPlans(
            compact = compact,
            onSignUp = { navigator.push(LoginRoute(startOnSignUp = true)) },
        )
        Spacer(Modifier.height(28.dp))
        PricingNotes()
    }
}

@Composable
private fun LandingPageChrome(
    currentPage: PublicPage,
    onHome: () -> Unit,
    onPricing: () -> Unit,
    onLogIn: () -> Unit,
    onSignUp: () -> Unit,
    content: @Composable (compact: Boolean) -> Unit,
) {
    CompositionLocalProvider(LocalLandingColors provides landingColors()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(drebinGradientBrush()),
        ) {
            val compact = maxWidth < 760.dp
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth, minimum = 24.dp)

            Column(Modifier.fillMaxSize()) {
                LandingTopBar(
                    compact = compact,
                    currentPage = currentPage,
                    horizontalMargin = horizontalMargin,
                    onHome = onHome,
                    onPricing = onPricing,
                    onLogIn = onLogIn,
                    onSignUp = onSignUp,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .safeContentPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalMargin, vertical = 48.dp)
                            .widthIn(max = 1120.dp),
                    ) {
                        content(compact)
                    }
                    LandingFooter(horizontalMargin = horizontalMargin)
                }
            }
        }
    }
}

@Composable
private fun LandingTopBar(
    compact: Boolean,
    currentPage: PublicPage,
    horizontalMargin: Dp,
    onHome: () -> Unit,
    onPricing: () -> Unit,
    onLogIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    Surface(
        color = LandingSurface.copy(alpha = 0.72f),
        contentColor = LandingText,
        border = BorderStroke(1.dp, LandingStroke),
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeContentPadding()
                    .padding(horizontal = horizontalMargin, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Brand(onClick = onHome)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavButton(
                        text = "Pricing",
                        selected = currentPage == PublicPage.Pricing,
                        onClick = onPricing,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onLogIn) { Text("Log In", color = LandingText) }
                    Button(
                        onClick = onSignUp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LandingAccent,
                            contentColor = LandingOnAccent,
                        ),
                    ) { Text("Sign Up") }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeContentPadding()
                    .padding(horizontal = horizontalMargin, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Brand(onClick = onHome)
                Spacer(Modifier.weight(1f))
                NavButton(
                    text = "Pricing",
                    selected = currentPage == PublicPage.Pricing,
                    onClick = onPricing,
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onLogIn) { Text("Log In", color = LandingText) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSignUp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LandingAccent,
                        contentColor = LandingOnAccent,
                    ),
                ) { Text("Sign Up") }
            }
        }
    }
}

@Composable
private fun Brand(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DrebinLogo(
                modifier = Modifier.size(36.dp),
                contentDescription = null,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Drebin451",
                style = MaterialTheme.typography.titleLarge,
                color = LandingText,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun NavButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Surface(
            color = LandingAccent.copy(alpha = 0.12f),
            contentColor = LandingAccent,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, LandingAccent.copy(alpha = 0.45f)),
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        TextButton(onClick = onClick) { Text(text, color = LandingMuted) }
    }
}

@Composable
private fun HeroSection(
    compact: Boolean,
    onSignUp: () -> Unit,
    onDownloadApk: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth(),
    ) {
        HeroCopy(
            compact = compact,
            onSignUp = onSignUp,
            onDownloadApk = onDownloadApk,
        )
    }
}

@Composable
private fun HeroCopy(
    compact: Boolean,
    onSignUp: () -> Unit,
    onDownloadApk: (() -> Unit)?,
) {
    Column {
        Eyebrow("PRIVATE ANDROID DISTRIBUTION")
        Spacer(Modifier.height(16.dp))
        Text(
            "Ship private Android builds without a public store.",
            style = MaterialTheme.typography.displayMedium.copy(lineHeight = 60.sp),
            color = LandingText,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Drebin451 gives your APKs a polished home: upload releases, keep version history, share app links, and let trusted testers install updates from one place.",
            style = MaterialTheme.typography.titleMedium,
            color = LandingMuted,
        )
        Spacer(Modifier.height(28.dp))
        HeroActions(
            compact = compact,
            onSignUp = onSignUp,
            onDownloadApk = onDownloadApk,
        )
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(LandingGreen)
            Spacer(Modifier.width(8.dp))
            Text(
                "Free plan included · upgrade when your APK storage grows",
                color = LandingMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HeroActions(
    compact: Boolean,
    onSignUp: () -> Unit,
    onDownloadApk: (() -> Unit)?,
) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SignUpButton(onSignUp = onSignUp, modifier = Modifier.fillMaxWidth())
            onDownloadApk?.let {
                DownloadApkButton(
                    onDownloadApk = it,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SignUpButton(onSignUp = onSignUp)
            onDownloadApk?.let { DownloadApkButton(onDownloadApk = it) }
        }
    }
}

@Composable
private fun SignUpButton(
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onSignUp,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = LandingAccent,
            contentColor = LandingOnAccent,
        ),
    ) { Text("Start sharing builds") }
}

@Composable
private fun DownloadApkButton(
    onDownloadApk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onDownloadApk,
        modifier = modifier,
        border = BorderStroke(1.dp, LandingStroke),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LandingText),
    ) {
        Icon(
            Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Download latest Drebin451 APK")
    }
}


@Composable
private fun WorkflowSection(compact: Boolean) {
    CenteredSectionHeader(
        eyebrow = "HOW IT WORKS",
        title = "A private app store in three steps",
        body = "Keep the release workflow simple for developers and obvious for testers.",
    )
    Spacer(Modifier.height(28.dp))
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepCard(
                "1",
                "Upload an APK",
                "Drebin451 reads package metadata, version codes, labels, icons, and release notes from each build."
            )
            StepCard(
                "2",
                "Share the app",
                "Create an app-level link for trusted users while downloads stay authenticated and access checked."
            )
            StepCard(
                "3",
                "Install updates",
                "Open links in the Android app, download releases, install APKs, and follow apps for new-build notifications."
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StepCard(
                "1",
                "Upload an APK",
                "Drebin451 reads package metadata, version codes, labels, icons, and release notes from each build.",
                Modifier.weight(1f)
            )
            StepCard(
                "2",
                "Share the app",
                "Create an app-level link for trusted users while downloads stay authenticated and access checked.",
                Modifier.weight(1f)
            )
            StepCard(
                "3",
                "Install updates",
                "Open links in the Android app, download releases, install APKs, and follow apps for new-build notifications.",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureGrid(compact: Boolean) {
    CenteredSectionHeader(
        eyebrow = "BUILT FOR TEAMS",
        title = "Everything your unreleased apps need",
        body = "Simple enough for side projects, organized enough for internal tools and client demos.",
    )
    Spacer(Modifier.height(28.dp))
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureTile(
                "Version history",
                "Every release keeps its file name, version code, timestamp, and notes."
            )
            FeatureTile(
                "Private downloads",
                "APK files are never exposed as raw public storage URLs."
            )
            FeatureTile("API uploads", "Automate publishing from CI with account API keys.")
            FeatureTile(
                "Subscriptions",
                "Start free, then move to Pro when the build archive grows."
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile(
                    "Version history",
                    "Every release keeps its file name, version code, timestamp, and notes.",
                    Modifier.weight(1f)
                )
                FeatureTile(
                    "Private downloads",
                    "APK files are never exposed as raw public storage URLs.",
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile(
                    "API uploads",
                    "Automate publishing from CI with account API keys.",
                    Modifier.weight(1f)
                )
                FeatureTile(
                    "Subscriptions",
                    "Start free, then move to Pro when the build archive grows.",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PricingHero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Eyebrow("PRICING")
        Spacer(Modifier.height(16.dp))
        Text(
            "Start free. Upgrade when your APK archive needs more room.",
            modifier = Modifier.widthIn(max = 820.dp),
            style = MaterialTheme.typography.displaySmall.copy(lineHeight = 52.sp),
            color = LandingText,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Free and Pro accounts share the same private distribution workflow. Pro simply gives you more storage for larger teams, longer histories, and heavier APKs.",
            modifier = Modifier.widthIn(max = 760.dp),
            style = MaterialTheme.typography.titleMedium,
            color = LandingMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PricingPlans(
    compact: Boolean,
    onSignUp: () -> Unit,
) {
    val freeFeatures = listOf(
        "${formatSize(PlanLimits.FREE_STORAGE_BYTES)} APK storage",
        "Private app pages and version history",
        "Authenticated APK downloads",
        "App-level share links",
        "Manual and API uploads",
    )
    val proFeatures = listOf(
        "${formatSize(PlanLimits.PRO_STORAGE_BYTES)} APK storage",
        "Everything in Free",
        "Longer release history for active apps",
        "More room for team and client builds",
        "Stripe billing and billing portal access",
    )

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PricingCard(
                "Free",
                "$0",
                "For experiments, demos, and small private apps.",
                freeFeatures,
                false,
                onSignUp
            )
            PricingCard(
                "Pro",
                "Subscription",
                "For teams shipping larger APKs or keeping more history.",
                proFeatures,
                true,
                onSignUp
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PricingCard(
                "Free",
                "$0",
                "For experiments, demos, and small private apps.",
                freeFeatures,
                false,
                onSignUp,
                Modifier.weight(1f)
            )
            PricingCard(
                "Pro",
                "Subscription",
                "For teams shipping larger APKs or keeping more history.",
                proFeatures,
                true,
                onSignUp,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PricingCard(
    name: String,
    price: String,
    description: String,
    features: List<String>,
    highlighted: Boolean,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) LandingSurfaceAlt else LandingSurface,
        ),
        border = BorderStroke(
            width = if (highlighted) 2.dp else 1.dp,
            color = if (highlighted) LandingAccent else LandingStroke,
        ),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = LandingText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        price,
                        color = if (highlighted) LandingAccent else LandingMuted,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (highlighted) {
                    Surface(
                        color = LandingAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "BEST FOR TEAMS",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = LandingAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(description, color = LandingMuted, style = MaterialTheme.typography.bodyLarge)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (highlighted) LandingAccent else LandingGreen,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            feature,
                            color = LandingText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Button(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highlighted) LandingAccent else LandingSurfaceAlt,
                    contentColor = if (highlighted) LandingOnAccent else LandingText,
                ),
            ) {
                Text(if (highlighted) "Sign up for Pro" else "Sign up free")
            }
        }
    }
}

@Composable
private fun PricingNotes() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LandingSurface.copy(alpha = 0.65f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, LandingStroke),
    ) {
        Text(
            "You can create an account on Free first and upgrade to Pro from Settings → Plan whenever you need the larger storage bucket.",
            modifier = Modifier.padding(20.dp),
            color = LandingMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LandingSurface),
        border = BorderStroke(1.dp, LandingStroke),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(LandingAccent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(number, color = LandingAccent, fontWeight = FontWeight.Black)
            }
            Text(
                title,
                color = LandingText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(body, color = LandingMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FeatureTile(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = LandingSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LandingStroke),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(LandingAccent)
            Text(
                title,
                color = LandingText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(body, color = LandingMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CenteredSectionHeader(
    eyebrow: String,
    title: String,
    body: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Eyebrow(eyebrow)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            modifier = Modifier.widthIn(max = 760.dp),
            style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 38.sp),
            color = LandingText,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            modifier = Modifier.widthIn(max = 720.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = LandingMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        color = LandingAccent,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
    )
}

@Composable
private fun SectionSpacer() {
    Spacer(Modifier.height(72.dp))
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .background(color.copy(alpha = 0.22f), CircleShape)
            .padding(2.dp)
            .background(color, CircleShape),
    )
}


@Composable
private fun LandingFooter(horizontalMargin: Dp) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LandingSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, LandingStroke),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrebinLogo(
                modifier = Modifier.size(28.dp),
                contentDescription = null,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "© 2026 Commit 451",
                color = LandingMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
