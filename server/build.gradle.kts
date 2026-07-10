plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.shadow)
}

group = "com.commit451.drebin451"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.commit451.drebin451.ApplicationKt"
}

tasks {
    processResources {
        // Keep the ignored local development key out of every jar/container image.
        exclude("*-firebase-adminsdk.json", "*service-account*.json")
    }
    shadowJar {
        // Firebase Admin bundles many META-INF/services files (gRPC, Google API
        // client, etc.). They must be merged into the fat jar or the SDK fails at
        // runtime. INCLUDE must be set BEFORE mergeServiceFiles() so the duplicates
        // aren't dropped before they can be merged.
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}

dependencies {
    api(projects.core)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.firebase.admin)
    implementation(libs.aws.s3)
    implementation(libs.apk.parser)
    testImplementation(libs.kotlin.testJunit)
}
