import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}

fun configuredValue(name: String): String? =
    providers.environmentVariable(name).orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: providers.gradleProperty(name).orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: localProperties.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

val releaseKeystorePath = configuredValue("ANDROID_KEYSTORE_PATH")
    ?: "app/androidApp/keystore.jks"
val releaseKeystoreFile = rootProject.file(releaseKeystorePath)
val releaseKeystoreAlias = configuredValue("ANDROID_KEYSTORE_ALIAS")
val releaseKeystorePassword = configuredValue("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyPassword = configuredValue("ANDROID_KEY_PASSWORD")
val releaseSigningAvailable = releaseKeystoreFile.isFile &&
        releaseKeystoreAlias != null &&
        releaseKeystorePassword != null &&
        releaseKeyPassword != null

// Fork PRs and first-time local checkouts can compile without production Firebase configuration.
// CI/local production builds create this ignored file before Gradle starts.
val googleServicesConfigured = project.file("google-services.json").isFile
if (googleServicesConfigured) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn(
        "google-services.json is missing; Firebase/Google sign-in resources will not be generated. " +
                "See README.md for local setup.",
    )
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(projects.app.shared)

    implementation(libs.androidx.activity.compose)

    // FCM: the FirebaseMessagingService that receives pushes lives in this module.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}

android {
    namespace = "com.commit451.drebin451"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.commit451.drebin451"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("drebin451.versionCode").get().toInt()
        versionName = providers.gradleProperty("drebin451.versionName").get()
        if (!googleServicesConfigured) {
            // Keep R.string.default_web_client_id available to secret-free fork builds. Configured
            // builds get the real value from the Google Services Gradle plugin instead.
            resValue("string", "default_web_client_id", "")
        }
    }
    buildFeatures {
        buildConfig = true
        resValues = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeystoreAlias
                keyPassword = releaseKeyPassword
            }
        } else {
            logger.warn(
                "Release signing is not configured. Set ANDROID_KEYSTORE_PATH, " +
                        "ANDROID_KEYSTORE_ALIAS, ANDROID_KEYSTORE_PASSWORD, and " +
                        "ANDROID_KEY_PASSWORD via environment variables, Gradle properties, " +
                        "or ignored local.properties.",
            )
        }
    }
    buildTypes {
        getByName("debug") {
            // Production/local configured builds keep a stable Firebase SHA by signing debug with
            // the release key. Public/fork builds fall back to Android's normal debug key.
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
