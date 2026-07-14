package com.commit451.drebin451.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.sdk.kotlin.services.s3.model.NoSuchKey
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.url.Url
import org.slf4j.LoggerFactory
import java.io.File

/** Thin wrapper around the AWS S3 Kotlin SDK for Backblaze B2 object storage. */
class B2ObjectStorage(
    private val config: B2StorageConfig = B2StorageConfig.fromEnv(),
    private val client: S3Client = S3Client {
        region = config.region
        endpointUrl = Url.parse(config.endpoint)
        forcePathStyle = true
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.keyId
            secretAccessKey = config.applicationKey
        }
    },
) {
    private val log = LoggerFactory.getLogger(B2ObjectStorage::class.java)

    suspend fun put(path: String, bytes: ByteArray, contentType: String) {
        client.putObject(
            PutObjectRequest {
                bucket = config.bucket
                key = path
                this.contentType = contentType
                contentLength = bytes.size.toLong()
                body = ByteStream.fromBytes(bytes)
            },
        )
    }

    /** Uploads from disk without materializing the complete object in the JVM heap. */
    suspend fun put(path: String, file: File, contentType: String) {
        client.putObject(
            PutObjectRequest {
                bucket = config.bucket
                key = path
                this.contentType = contentType
                contentLength = file.length()
                body = ByteStream.fromFile(file)
            },
        )
    }

    suspend fun get(path: String): StoredObject? = try {
        client.getObject(
            GetObjectRequest {
                bucket = config.bucket
                key = path
            },
        ) { response ->
            val body = response.body ?: return@getObject null
            StoredObject(bytes = body.toByteArray(), contentType = response.contentType)
        }
    } catch (_: NoSuchKey) {
        null
    }

    suspend fun list(prefix: String): List<StoredObjectInfo> {
        val objects = mutableListOf<StoredObjectInfo>()
        var continuationToken: String? = null
        do {
            val response = client.listObjectsV2(
                ListObjectsV2Request {
                    bucket = config.bucket
                    this.prefix = prefix
                    this.continuationToken = continuationToken
                },
            )
            response.contents.orEmpty().forEach { item ->
                val key = item.key?.takeIf { it.isNotBlank() } ?: return@forEach
                objects += StoredObjectInfo(path = key, sizeBytes = item.size ?: 0)
            }
            continuationToken = response.nextContinuationToken
        } while (response.isTruncated == true && continuationToken != null)
        return objects
    }

    /** Best-effort delete of a stored object. No-op when the path is blank or already gone. */
    suspend fun delete(path: String): Boolean {
        if (path.isBlank()) return true
        return try {
            client.deleteObject(
                DeleteObjectRequest {
                    bucket = config.bucket
                    key = path
                },
            )
            true
        } catch (t: Throwable) {
            log.warn("Failed to delete storage object $path", t)
            false
        }
    }
}
