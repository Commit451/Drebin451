package com.commit451.drebin451.storage

/** Runtime configuration for Backblaze B2's S3-compatible API. */
data class B2StorageConfig(
    val keyId: String,
    val applicationKey: String,
    val bucket: String = DEFAULT_BUCKET,
    val endpoint: String = DEFAULT_ENDPOINT,
    val region: String = DEFAULT_REGION,
) {
    companion object {
        const val DEFAULT_BUCKET = "drebin451"
        const val DEFAULT_ENDPOINT = "https://s3.us-east-005.backblazeb2.com"
        const val DEFAULT_REGION = "us-east-005"

        fun fromEnv(env: Map<String, String> = System.getenv()): B2StorageConfig {
            val keyId = env["B2_KEY_ID"]?.trim().orEmpty()
                .ifBlank { env["AWS_ACCESS_KEY_ID"]?.trim().orEmpty() }
            val applicationKey = env["B2_APPLICATION_KEY"]?.trim().orEmpty()
                .ifBlank { env["AWS_SECRET_ACCESS_KEY"]?.trim().orEmpty() }
            if (keyId.isBlank() || applicationKey.isBlank()) {
                error("Missing Backblaze B2 credentials: set B2_KEY_ID and B2_APPLICATION_KEY")
            }

            return B2StorageConfig(
                keyId = keyId,
                applicationKey = applicationKey,
                bucket = env["B2_BUCKET"]?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_BUCKET,
                endpoint = env["B2_ENDPOINT"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_ENDPOINT,
                region = env["B2_REGION"]?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_REGION,
            )
        }
    }
}
