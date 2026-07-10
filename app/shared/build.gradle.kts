import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

abstract class GenerateAppBuildInfoTask : DefaultTask() {
    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<String>

    @get:Internal
    abstract val repositoryDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val commitHash = currentCommitHash()
        val outputFile = outputDir.get()
            .file("com/commit451/drebin451/appinfo/GeneratedAppBuildInfo.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        val content =
            """
            package com.commit451.drebin451.appinfo

            internal object GeneratedAppBuildInfo {
                const val VERSION_NAME: String = "${versionName.get().toKotlinStringLiteral()}"
                const val VERSION_CODE: Int = ${versionCode.get().toInt()}
                const val COMMIT_HASH: String = "${commitHash.toKotlinStringLiteral()}"
            }
            """.trimIndent() + "\n"
        if (!outputFile.exists() || outputFile.readText() != content) {
            outputFile.writeText(content)
        }
    }

    private fun currentCommitHash(): String {
        val githubSha = System.getenv("GITHUB_SHA")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (githubSha != null) return githubSha

        return runCatching {
            val process = ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(repositoryDir.get().asFile)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else "unknown"
        }.getOrDefault("unknown")
    }

    private fun String.toKotlinStringLiteral(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")
}

abstract class GenerateFirebaseWebConfigTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val properties = Properties().apply {
            configFile.get().asFile.inputStream().use(::load)
        }
        val requiredKeys = listOf(
            "applicationId",
            "apiKey",
            "projectId",
            "storageBucket",
            "gcmSenderId",
            "authDomain",
            "webClientId",
        )
        val values = requiredKeys.associateWith { key ->
            properties.getProperty(key)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Missing Firebase web config property: $key")
        }
        val outputFile = outputDir.get()
            .file("com/commit451/drebin451/auth/FirebaseWebConfig.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        val content =
            """
            package com.commit451.drebin451.auth

            public object FirebaseWebConfig {
                public const val APPLICATION_ID: String = "${values.getValue("applicationId").toKotlinStringLiteral()}"
                public const val API_KEY: String = "${values.getValue("apiKey").toKotlinStringLiteral()}"
                public const val PROJECT_ID: String = "${values.getValue("projectId").toKotlinStringLiteral()}"
                public const val STORAGE_BUCKET: String = "${values.getValue("storageBucket").toKotlinStringLiteral()}"
                public const val GCM_SENDER_ID: String = "${values.getValue("gcmSenderId").toKotlinStringLiteral()}"
                public const val AUTH_DOMAIN: String = "${values.getValue("authDomain").toKotlinStringLiteral()}"
                public const val WEB_CLIENT_ID: String = "${values.getValue("webClientId").toKotlinStringLiteral()}"
            }
            """.trimIndent() + "\n"
        if (!outputFile.exists() || outputFile.readText() != content) {
            outputFile.writeText(content)
        }
    }

    private fun String.toKotlinStringLiteral(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")
}

val generatedAppBuildInfoDir = layout.buildDirectory.dir("generated/source/appBuildInfo/commonMain/kotlin")
val generatedFirebaseWebConfigDir = layout.buildDirectory.dir("generated/source/firebaseWebConfig/webMain/kotlin")
val drebinVersionName = providers.gradleProperty("drebin451.versionName").orElse("1.0")
val drebinVersionCode = providers.gradleProperty("drebin451.versionCode").orElse("1")
val localFirebaseWebConfig = layout.projectDirectory.file("firebase-web-config.properties")
val exampleFirebaseWebConfig = layout.projectDirectory.file("firebase-web-config.example.properties")
val selectedFirebaseWebConfig = if (localFirebaseWebConfig.asFile.isFile) {
    localFirebaseWebConfig
} else {
    exampleFirebaseWebConfig
}

val generateAppBuildInfo by tasks.registering(GenerateAppBuildInfoTask::class) {
    versionName.set(drebinVersionName)
    versionCode.set(drebinVersionCode)
    repositoryDir.set(rootProject.layout.projectDirectory)
    outputDir.set(generatedAppBuildInfoDir)
}

val generateFirebaseWebConfig by tasks.registering(GenerateFirebaseWebConfigTask::class) {
    configFile.set(selectedFirebaseWebConfig)
    outputDir.set(generatedFirebaseWebConfigDir)
}

kotlin {
    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "com.commit451.drebin451.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedAppBuildInfoDir)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)

            // Ktor client engine (Android); the shared plugins live in commonMain.
            implementation(libs.ktor.client.cio)

            // Firebase + Google auth. The gitlive Firebase Auth fork is in webMain too;
            // kmpauth is Android-only since its publication has no web targets.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth.gitlive)
            // FCM: subscribe/unsubscribe to an app's update topic when following.
            implementation(libs.firebase.messaging)
            // AndroidX Preferences DataStore in the shared KMP Android target. Local FCM topic status
            // is stored in noBackupFilesDir so app reinstall/restore doesn't resurrect old toggles.
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kmpauth.google)
            implementation(libs.kmpauth.firebase)
            implementation(libs.kmpauth.uihelper)
        }
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.androidx.navigation3.ui)

            // Ktor client — core + shared plugins are platform-agnostic; only the engine is
            // provided per-platform via expect/actual createHttpClient() (CIO on Android, Js on web).
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Coil — loads app launcher icons (App.imageUrl) over the network on every platform.
            // The Ktor fetcher reuses each target's existing engine (CIO on Android, Js on web).
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.kmpalette.core)
            implementation(libs.kmpalette.extensions.network)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // The web target (js + wasmJs) shares the gitlive Firebase Auth actuals,
        // since the fork at com.jawnnypoo:firebase-auth publishes both variants.
        webMain {
            kotlin.srcDir(generatedFirebaseWebConfigDir)
            dependencies {
                implementation(libs.firebase.auth.gitlive)
            }
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

tasks.matching { it.name.startsWith("compile") || it.name.endsWith("SourcesJar") }.configureEach {
    dependsOn(generateAppBuildInfo, generateFirebaseWebConfig)
}

// kmpauth-firebase still depends transitively on upstream dev.gitlive:firebase-auth,
// whose classes are identical to (and would collide with) our com.jawnnypoo fork.
// The fork is what we want everywhere, so kick the upstream off every configuration.
configurations.configureEach {
    exclude(group = "dev.gitlive")
}
