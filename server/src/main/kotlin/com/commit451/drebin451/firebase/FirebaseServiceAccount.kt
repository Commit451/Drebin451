package com.commit451.drebin451.firebase

import java.io.File
import java.util.Base64

internal const val FirebaseServiceAccountBase64Env = "FIREBASE_SERVICE_ACCOUNT_BASE64"
internal const val FirebaseServiceAccountRawEnv = "FIREBASE_SERVICE_ACCOUNT"
internal const val FirebaseServiceAccountPathEnv = "FIREBASE_SERVICE_ACCOUNT_PATH"
internal const val DefaultFirebaseServiceAccountPath =
    "server/src/main/resources/drebin451-firebase-adminsdk.json"

private const val ServerWorkingDirectoryServiceAccountPath =
    "src/main/resources/drebin451-firebase-adminsdk.json"

/**
 * Resolves Firebase Admin credentials without ever requiring them in a compiled resource.
 *
 * Cloud Run receives base64 through [FirebaseServiceAccountBase64Env]. Local development can use
 * raw JSON, an explicit path, or the ignored historical source-tree path.
 */
internal fun firebaseServiceAccountJson(
    env: Map<String, String> = System.getenv(),
    readFile: (String) -> ByteArray = { File(it).readBytes() },
): ByteArray {
    env[FirebaseServiceAccountBase64Env]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { encoded ->
            return try {
                Base64.getDecoder().decode(encoded).also {
                    check(it.isNotEmpty()) { "$FirebaseServiceAccountBase64Env decoded to an empty value" }
                }
            } catch (error: IllegalArgumentException) {
                throw IllegalStateException("$FirebaseServiceAccountBase64Env is not valid base64", error)
            }
        }

    env[FirebaseServiceAccountRawEnv]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it.toByteArray() }

    env[FirebaseServiceAccountPathEnv]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { configuredPath ->
            return runCatching { readFile(configuredPath) }
                .getOrElse { error ->
                    throw IllegalStateException(
                        "Could not read Firebase service account from $FirebaseServiceAccountPathEnv",
                        error,
                    )
                }
        }

    listOf(DefaultFirebaseServiceAccountPath, ServerWorkingDirectoryServiceAccountPath)
        .forEach { path ->
            runCatching { readFile(path) }.getOrNull()?.let { return it }
        }

    error(
        "No Firebase service account found. Set $FirebaseServiceAccountBase64Env, " +
                "$FirebaseServiceAccountRawEnv, or $FirebaseServiceAccountPathEnv; local fallback: " +
                DefaultFirebaseServiceAccountPath,
    )
}
