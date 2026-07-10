package com.commit451.drebin451.firebase

import com.commit451.drebin451.apikey.ApiKeys
import com.commit451.drebin451.firebase.Firebasis.CollectionApps
import com.commit451.drebin451.firebase.Firebasis.CollectionVersions
import com.commit451.drebin451.firebase.Firebasis.appDocId
import com.commit451.drebin451.firebase.Firebasis.deleteApp
import com.commit451.drebin451.model.ApiKey
import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.Config
import com.commit451.drebin451.model.PaginatedResponse
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.model.User
import com.commit451.drebin451.storage.B2ObjectStorage
import com.commit451.drebin451.storage.StoredObject
import com.commit451.drebin451.storage.StoredObjectInfo
import com.commit451.drebin451.stripe.StripeBilling
import com.commit451.drebin451.stripe.StripeSubscriptionUpdate
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.Transaction
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64

internal fun storageUsedBytesAfterReserve(plan: String, usedBytes: Long, bytes: Long): Long {
    val normalizedUsedBytes = usedBytes.coerceAtLeast(0)
    if (bytes <= 0) return normalizedUsedBytes
    val normalizedPlan = PlanLimits.normalized(plan)
    val limitBytes = PlanLimits.storageQuotaBytes(normalizedPlan)
    if (bytes > limitBytes - normalizedUsedBytes) {
        throw StorageQuotaExceededException(
            plan = normalizedPlan,
            usedBytes = normalizedUsedBytes,
            attemptedBytes = bytes,
            limitBytes = limitBytes,
        )
    }
    return normalizedUsedBytes + bytes
}

internal fun storageUsedBytesAfterRelease(usedBytes: Long, bytes: Long): Long {
    val normalizedUsedBytes = usedBytes.coerceAtLeast(0)
    if (bytes <= 0) return normalizedUsedBytes
    return (normalizedUsedBytes - bytes).coerceAtLeast(0)
}

internal fun App.withRecomputedVersionAggregate(remaining: List<AppVersion>): App {
    val latest = remaining.maxByOrNull { it.versionCode }
    return copy(
        latestVersionName = latest?.versionName ?: "",
        latestVersionCode = latest?.versionCode ?: 0,
        versionCount = remaining.size,
        updatedAt = remaining.maxOfOrNull { it.updatedAt } ?: updatedAt,
    )
}

private const val ApkStoragePrefix = "apks/"
private const val IconStoragePrefix = "icons/"

internal fun apkStoragePath(
    ownerUserId: String,
    applicationId: String,
    versionId: String,
    fileName: String
): String =
    "$ApkStoragePrefix$ownerUserId/$applicationId/$versionId/${fileName.ifBlank { "app.apk" }}"

internal fun parseApkStoragePath(path: String): ApkStoragePath? {
    if (!path.startsWith(ApkStoragePrefix)) return null
    val parts = path.removePrefix(ApkStoragePrefix).split('/', limit = 4)
    if (parts.size != 4 || parts.any { it.isBlank() }) return null
    return ApkStoragePath(
        ownerUserId = parts[0],
        applicationId = parts[1],
        versionId = parts[2],
        fileName = parts[3],
    )
}

internal fun managedStorageKind(path: String): String = when {
    path.startsWith(ApkStoragePrefix) -> if (parseApkStoragePath(path) == null) "malformed-apk" else "apk"
    path.startsWith(IconStoragePrefix) -> "icon"
    else -> "unknown"
}

/**
 * Single entry point to Firebase on the server. Initializes the Admin SDK and wraps
 * Firestore. APK and icon bytes are stored in Backblaze B2 through [B2ObjectStorage].
 * Data is modelled as apps with nested version rows:
 *  - [CollectionApps] — one [App] per (owner, applicationId), keyed by [appDocId].
 *  - [CollectionVersions] — one subcollection under each app, with one [AppVersion] per uploaded APK.
 *
 * App-scoped version lists read the app's `versions` subcollection directly; cross-app version
 * lookups use a collection-group query on [CollectionVersions]. App and version list endpoints are
 * ordered and cursor-paginated in Firestore by `updatedAt desc, id asc`. Patterned after askarasu's `Firebasis.kt`.
 *
 * Firebase Admin credentials are loaded from a base64 environment value in production, with raw
 * JSON or an ignored local file available as development fallbacks. They are never bundled in the
 * server jar or container image.
 */
object Firebasis {
    private val log = LoggerFactory.getLogger(Firebasis::class.java)

    private const val CollectionUsers = "users"
    private const val CollectionApps = "apps"
    private const val CollectionVersions = "versions"
    private const val CollectionSharedApps = "sharedApps"
    private const val CollectionConfig = "config"
    private const val CollectionApiKeys = "apiKeys"
    private const val ConfigDocId = "app"
    private const val DefaultPageSize = 10
    private const val MaxPageSize = 50

    private val secureRandom = SecureRandom()

    private val objectStorage: B2ObjectStorage by lazy { B2ObjectStorage() }

    private val credentials: GoogleCredentials by lazy {
        GoogleCredentials.fromStream(firebaseServiceAccountJson().inputStream())
    }

    private val firestore: Firestore by lazy { FirestoreClient.getFirestore() }

    fun initialize() {
        log.info("🔥 Initializing Firebase")
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        FirebaseApp.initializeApp(options)
    }

    // --- Auth ---

    data class AuthUser(val uid: String, val email: String, val name: String)

    /** Verifies a Firebase ID token and extracts the caller's identity. Throws on invalid token. */
    fun userIdFromAuth(idToken: String): AuthUser {
        val decoded = FirebaseAuth.getInstance().verifyIdToken(idToken)
        return AuthUser(
            uid = decoded.uid,
            email = decoded.email ?: "",
            name = decoded.name ?: "",
        )
    }

    /** Sends a Firebase Auth password-reset email without revealing whether the account exists. */
    suspend fun sendPasswordResetEmail(email: String) {
        PasswordResetEmailSender.send(email)
    }

    // --- Users ---

    private fun userDocument(uid: String) =
        firestore.collection(CollectionUsers).document(uid)

    /** Returns the existing user doc, or lazily creates it on first sign-in. */
    suspend fun getOrCreateUser(uid: String, email: String, displayName: String): User {
        val docRef = userDocument(uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            snapshot.toObject(User::class.java)?.let { return it }
        }
        val now = System.currentTimeMillis()
        val user = User(
            uid = uid,
            email = email,
            displayName = displayName,
            createdAt = now,
            plan = PlanIds.FREE,
            planUpdatedAt = now,
        )
        docRef.set(user).await()
        return user
    }

    /** The existing user doc, or null. Used to resolve an API key's owner (who already has one). */
    suspend fun getUser(uid: String): User? {
        val snapshot = userDocument(uid).get().await()
        return if (snapshot.exists()) snapshot.toObject(User::class.java) else null
    }

    suspend fun updateStripeCustomerId(uid: String, customerId: String): User {
        val ref = userDocument(uid)
        return firestore.runTransaction { txn ->
            val user = txn.get(ref).get().toObject(User::class.java)
                ?: throw IllegalArgumentException("User not found")
            val updated = user.copy(stripeCustomerId = customerId)
            txn.update(ref, "stripeCustomerId", updated.stripeCustomerId)
            updated
        }.await()
    }

    suspend fun refreshStripePlan(uid: String): User {
        val user = getUser(uid) ?: throw IllegalArgumentException("User not found")
        if (!StripeBilling.isConfigured || user.stripeCustomerId.isBlank()) return user
        val now = System.currentTimeMillis()
        val update = StripeBilling.currentSubscriptionFor(user.stripeCustomerId)
            ?: StripeSubscriptionUpdate(
                uid = uid,
                customerId = user.stripeCustomerId,
                status = "none",
                plan = PlanIds.FREE,
            )
        return applyStripeSubscriptionUpdate(update.copy(uid = uid), syncedAt = now) ?: user
    }

    suspend fun refreshPlanIfStale(user: User): User {
        if (!StripeBilling.isConfigured || user.stripeCustomerId.isBlank()) return user
        val now = System.currentTimeMillis()
        if (user.planSyncedAt > 0 && now - user.planSyncedAt < StripeBilling.syncStaleMs) return user
        return try {
            refreshStripePlan(user.uid)
        } catch (t: Throwable) {
            log.warn("Failed to refresh Stripe plan for ${user.uid}", t)
            user
        }
    }

    suspend fun applyStripeSubscriptionUpdate(
        update: StripeSubscriptionUpdate,
        syncedAt: Long = System.currentTimeMillis(),
    ): User? {
        val user = update.uid?.takeIf { it.isNotBlank() }?.let { getUser(it) }
            ?: userForStripeCustomerId(update.customerId)
            ?: update.subscriptionId.takeIf { it.isNotBlank() }
                ?.let { userForStripeSubscriptionId(it) }
            ?: return null
        return updateUserStripeSubscription(user, update, syncedAt)
    }

    private suspend fun updateUserStripeSubscription(
        user: User,
        update: StripeSubscriptionUpdate,
        syncedAt: Long,
    ): User {
        val ref = userDocument(user.uid)
        return firestore.runTransaction { txn ->
            val current = txn.get(ref).get().toObject(User::class.java)
                ?: throw IllegalArgumentException("User not found")
            val normalizedPlan = PlanLimits.normalized(update.plan)
            val currentPlan = PlanLimits.normalized(current.plan)
            val updated = current.copy(
                stripeCustomerId = update.customerId.ifBlank { current.stripeCustomerId },
                stripeSubscriptionId = update.subscriptionId.ifBlank { current.stripeSubscriptionId },
                stripeSubscriptionStatus = update.status.ifBlank { current.stripeSubscriptionStatus },
                stripePriceId = update.priceId.ifBlank { current.stripePriceId },
                stripeCurrentPeriodEnd = if (update.currentPeriodEnd > 0) {
                    update.currentPeriodEnd
                } else {
                    current.stripeCurrentPeriodEnd
                },
                plan = normalizedPlan,
                planUpdatedAt = if (normalizedPlan != currentPlan || current.planUpdatedAt == 0L) {
                    syncedAt
                } else {
                    current.planUpdatedAt
                },
                planSyncedAt = syncedAt,
            )
            txn.update(
                ref,
                mapOf(
                    "stripeCustomerId" to updated.stripeCustomerId,
                    "stripeSubscriptionId" to updated.stripeSubscriptionId,
                    "stripeSubscriptionStatus" to updated.stripeSubscriptionStatus,
                    "stripePriceId" to updated.stripePriceId,
                    "stripeCurrentPeriodEnd" to updated.stripeCurrentPeriodEnd,
                    "plan" to updated.plan,
                    "planUpdatedAt" to updated.planUpdatedAt,
                    "planSyncedAt" to updated.planSyncedAt,
                ),
            )
            updated
        }.await()
    }

    private suspend fun userForStripeCustomerId(customerId: String): User? {
        if (customerId.isBlank()) return null
        val snapshot = firestore.collection(CollectionUsers)
            .whereEqualTo("stripeCustomerId", customerId)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }

    private suspend fun userForStripeSubscriptionId(subscriptionId: String): User? {
        if (subscriptionId.isBlank()) return null
        val snapshot = firestore.collection(CollectionUsers)
            .whereEqualTo("stripeSubscriptionId", subscriptionId)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }

    suspend fun reserveStorage(uid: String, bytes: Long) {
        if (bytes <= 0) {
            getUser(uid) ?: throw IllegalArgumentException("User not found")
            return
        }
        val ref = userDocument(uid)
        firestore.runTransaction { txn ->
            val user = txn.get(ref).get().toObject(User::class.java)
                ?: throw IllegalArgumentException("User not found")
            val plan = PlanLimits.normalized(user.plan)
            val nextUsedBytes = storageUsedBytesAfterReserve(
                plan = plan,
                usedBytes = user.storageUsedBytes,
                bytes = bytes,
            )
            txn.update(
                ref,
                mapOf(
                    "plan" to plan,
                    "storageUsedBytes" to nextUsedBytes,
                ),
            )
            Unit
        }.await()
    }

    suspend fun releaseStorage(uid: String, bytes: Long) {
        if (bytes <= 0) return
        val ref = userDocument(uid)
        firestore.runTransaction { txn ->
            releaseStorageInTransaction(txn, ref, bytes)
            Unit
        }.await()
    }

    private fun releaseStorageInTransaction(txn: Transaction, ref: DocumentReference, bytes: Long) {
        if (bytes <= 0) return
        val user = txn.get(ref).get().toObject(User::class.java) ?: return
        val nextUsedBytes = storageUsedBytesAfterRelease(user.storageUsedBytes, bytes)
        txn.update(ref, "storageUsedBytes", nextUsedBytes)
    }

    // --- API keys ---

    /** The caller's API keys, newest first (sorted in memory — no composite index). */
    suspend fun listApiKeys(uid: String): List<ApiKey> =
        firestore.collection(CollectionApiKeys)
            .whereEqualTo("ownerUserId", uid)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ApiKey::class.java) }
            .sortedByDescending { it.createdAt }

    suspend fun createApiKey(key: ApiKey) {
        firestore.collection(CollectionApiKeys).document(key.id).set(key).await()
    }

    suspend fun getApiKey(id: String): ApiKey? {
        val snapshot = firestore.collection(CollectionApiKeys).document(id).get().await()
        return if (snapshot.exists()) snapshot.toObject(ApiKey::class.java) else null
    }

    /** Deletes (revokes) a key. Callers perform any required existence/ownership checks first. */
    suspend fun deleteApiKey(id: String) {
        firestore.collection(CollectionApiKeys).document(id).delete().await()
    }

    /**
     * Resolves the owner of a presented API key by hashing the token and matching the stored
     * [ApiKey.tokenHash] (single equality query — no composite index). Returns null when no key
     * matches (caller maps that to 401). On a hit, [ApiKey.lastUsedAt] is bumped best-effort: a
     * failed write is logged but never blocks the API request the key was presented for.
     */
    suspend fun userForApiKey(token: String): User? {
        val snapshot = firestore.collection(CollectionApiKeys)
            .whereEqualTo("tokenHash", ApiKeys.hash(token))
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        val key = doc.toObject(ApiKey::class.java)
        try {
            doc.reference.update("lastUsedAt", System.currentTimeMillis()).await()
        } catch (t: Throwable) {
            log.warn("Failed to bump lastUsedAt for api key ${key.id}", t)
        }
        return getUser(key.ownerUserId)
    }

    // --- Config ---

    /** App-wide client config from `config/app`, or defaults when the doc is absent. */
    suspend fun getConfig(): Config {
        val snapshot = firestore.collection(CollectionConfig).document(ConfigDocId).get().await()
        return if (snapshot.exists()) snapshot.toObject(Config::class.java)
            ?: Config() else Config()
    }

    // --- Apps ---

    /** Deterministic `apps` doc id so each applicationId maps to exactly one app per owner. */
    fun appDocId(ownerUserId: String, applicationId: String): String = "$ownerUserId:$applicationId"

    private fun appDocument(appId: String) =
        firestore.collection(CollectionApps).document(appId)

    private fun sharedAppDocument(uid: String, appId: String) =
        userDocument(uid).collection(CollectionSharedApps).document(appId)

    private fun sharedAppsCollection(uid: String) =
        userDocument(uid).collection(CollectionSharedApps)

    private fun versionsCollection(appId: String) =
        appDocument(appId).collection(CollectionVersions)

    private fun versionDocument(appId: String, versionId: String) =
        versionsCollection(appId).document(versionId)

    /** Deterministic Storage path for an app's launcher icon — one icon per (owner, applicationId). */
    fun iconStoragePath(ownerUserId: String, applicationId: String): String =
        "icons/$ownerUserId/$applicationId/icon"

    /**
     * The caller's own apps — most-recently-updated first. Shared apps live in each user's
     * `users/{uid}/sharedApps` subcollection and are listed separately.
     */
    suspend fun listApps(
        uid: String,
        pageSize: Int? = null,
        pageToken: String? = null
    ): PaginatedResponse<App> {
        val limit = sanitizePageSize(pageSize)
        val query = firestore.collection(CollectionApps)
            .whereEqualTo("ownerUserId", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("id", Query.Direction.ASCENDING)
            .withCursor(pageToken)
            .limit(limit + 1)

        val apps = query.get()
            .await()
            .documents
            .mapNotNull { it.toObject(App::class.java) }
        return paginated(apps, limit) { app -> app.updatedAt to app.id }
    }

    /** Apps this user explicitly added by opening a random public share URL. */
    suspend fun listSharedApps(
        uid: String,
        pageSize: Int? = null,
        pageToken: String? = null
    ): PaginatedResponse<App> {
        val limit = sanitizePageSize(pageSize)
        val apps = sharedAppsCollection(uid)
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.getString("appId") }
            .distinct()
            .mapNotNull { appId -> getApp(appId) }
            .filterNot { app -> app.ownerUserId == uid }
            .sortedWith(
                compareByDescending<App> { it.updatedAt }
                    .thenBy { it.id },
            )
        return paginated(apps.afterCursor(pageToken), limit) { app -> app.updatedAt to app.id }
    }

    suspend fun getApp(appId: String): App? {
        val snapshot = appDocument(appId).get().await()
        return if (snapshot.exists()) snapshot.toObject(App::class.java) else null
    }

    suspend fun getAppByShareId(shareId: String): App? {
        if (shareId.isBlank()) return null
        val snapshot = firestore.collection(CollectionApps)
            .whereEqualTo("shareId", shareId)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(App::class.java)
    }

    /**
     * Revokes the current public share URL by replacing the app's random share id. The old URL stops
     * resolving immediately because share-link resolution reads the current `apps/{id}.shareId`
     * field directly. Existing Shared rows are intentionally left alone; this only invalidates the
     * previous URL for future opens.
     */
    suspend fun refreshAppShareId(appId: String): App? {
        val appRef = appDocument(appId)
        return firestore.runTransaction { txn ->
            val existing =
                txn.get(appRef).get().toObject(App::class.java) ?: return@runTransaction null
            val updated = existing.copy(shareId = newShareId())
            txn.set(appRef, updated)
            updated
        }.await()
    }

    suspend fun canAccessApp(uid: String, app: App): Boolean =
        app.ownerUserId == uid || sharedAppDocument(uid, app.id).get().await().exists()

    /**
     * Adds the app behind [shareId] to [uid]'s Shared collection. Owners do not get a redundant
     * shared row; re-opening a link is idempotent and bumps the row timestamp.
     */
    suspend fun addSharedApp(shareId: String, uid: String): App? {
        val app = getAppByShareId(shareId) ?: return null
        if (app.ownerUserId == uid) return app
        val now = System.currentTimeMillis()
        sharedAppDocument(uid, app.id)
            .set(
                mapOf(
                    "id" to app.id,
                    "appId" to app.id,
                    "ownerUserId" to app.ownerUserId,
                    "addedAt" to now,
                    "updatedAt" to now,
                ),
            )
            .await()
        return app
    }

    /** Removes [appId] from [uid]'s Shared collection. The owner's app and uploads are untouched. */
    suspend fun removeSharedApp(uid: String, appId: String) {
        sharedAppDocument(uid, appId).delete().await()
    }

    sealed class DeleteAppResult {
        object NotFound : DeleteAppResult()
        object Forbidden : DeleteAppResult()
        data class Deleted(val app: App, val versions: List<AppVersion>) : DeleteAppResult()
    }

    sealed class DeleteVersionResult {
        object NotFound : DeleteVersionResult()
        object Forbidden : DeleteVersionResult()
        data class Deleted(val version: AppVersion) : DeleteVersionResult()
    }

    data class DeleteAccountResult(
        val deletedAppCount: Int,
        val deletedApiKeyCount: Int,
        val deletedOwnSharedAppCount: Int,
        val deletedSharedReferenceCount: Int,
    )

    /**
     * Permanently removes the server-side footprint for [uid], then deletes the Firebase Auth user.
     *
     * Firestore does not cascade subcollection deletes, so this explicitly cancels any active Stripe
     * subscription, removes owned app/version rows (and their B2 blobs), API keys, the user's own
     * Shared rows, other users' Shared rows that point at this user's apps, and finally the user
     * document. The Auth user is deleted last so a transient cleanup failure leaves the caller able
     * to retry with the same account.
     */
    suspend fun deleteAccount(uid: String): DeleteAccountResult {
        getUser(uid)?.let { StripeBilling.cancelSubscriptionForAccountDeletion(it) }

        val ownedApps = ownedAppsForAccountDeletion(uid)
        var deletedAppCount = 0
        for (app in ownedApps) {
            if (deleteApp(app.id, uid) is DeleteAppResult.Deleted) {
                deletedAppCount++
            }
        }

        val deletedSharedReferences = deleteSharedAppReferencesForOwner(uid)
        val deletedOwnSharedApps = deleteOwnSharedApps(uid)
        val deletedApiKeys = deleteApiKeysForOwner(uid)
        userDocument(uid).delete().await()

        FirebaseAuth.getInstance().deleteUser(uid)

        return DeleteAccountResult(
            deletedAppCount = deletedAppCount,
            deletedApiKeyCount = deletedApiKeys,
            deletedOwnSharedAppCount = deletedOwnSharedApps,
            deletedSharedReferenceCount = deletedSharedReferences,
        )
    }

    private suspend fun ownedAppsForAccountDeletion(uid: String): List<App> =
        firestore.collection(CollectionApps)
            .whereEqualTo("ownerUserId", uid)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(App::class.java) }

    private suspend fun deleteApiKeysForOwner(uid: String): Int {
        val refs = firestore.collection(CollectionApiKeys)
            .whereEqualTo("ownerUserId", uid)
            .get()
            .await()
            .documents
            .map { it.reference }
        return deleteDocuments(refs)
    }

    private suspend fun deleteOwnSharedApps(uid: String): Int {
        val refs = sharedAppsCollection(uid)
            .get()
            .await()
            .documents
            .map { it.reference }
        return deleteDocuments(refs)
    }

    private suspend fun deleteSharedAppReferencesForOwner(uid: String): Int {
        val refs = firestore.collectionGroup(CollectionSharedApps)
            .whereEqualTo("ownerUserId", uid)
            .get()
            .await()
            .documents
            .map { it.reference }
        return deleteDocuments(refs)
    }

    private suspend fun deleteDocuments(refs: List<DocumentReference>): Int {
        for (chunk in refs.chunked(450)) {
            val batch = firestore.batch()
            for (ref in chunk) {
                batch.delete(ref)
            }
            batch.commit().await()
        }
        return refs.size
    }

    /**
     * Deletes [appId] and its versions exactly once. Firestore app/version deletes and the
     * storage counter decrement happen in one transaction; Backblaze B2 object cleanup runs
     * afterwards and remains best-effort.
     */
    suspend fun deleteApp(appId: String, requesterUid: String): DeleteAppResult {
        val appRef = appDocument(appId)
        val result = firestore.runTransaction { txn ->
            val app = txn.get(appRef).get().toObject(App::class.java)
                ?: return@runTransaction DeleteAppResult.NotFound
            if (app.ownerUserId != requesterUid) return@runTransaction DeleteAppResult.Forbidden

            val versions = txn.get(versionsCollection(appId))
                .get()
                .documents
                .mapNotNull { it.toObject(AppVersion::class.java) }
            val releasedBytes = versions.sumOf { it.fileSizeBytes }

            releaseStorageInTransaction(txn, userDocument(app.ownerUserId), releasedBytes)
            txn.delete(appRef)
            for (version in versions) {
                txn.delete(versionDocument(app.id, version.id))
            }
            DeleteAppResult.Deleted(app, versions)
        }.await()

        if (result is DeleteAppResult.Deleted) {
            for (version in result.versions) {
                deleteBlob(version.storagePath)
            }
            deleteBlob(iconStoragePath(result.app.ownerUserId, result.app.applicationId))
        }
        return result
    }

    // --- Versions ---

    /** All versions of an app, most-recently-updated first. */
    suspend fun listVersions(
        appId: String,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): PaginatedResponse<AppVersion> {
        val limit = sanitizePageSize(pageSize)
        val query = versionsCollection(appId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("id", Query.Direction.ASCENDING)
            .withCursor(pageToken)
            .limit(limit + 1)

        val versions = query.get()
            .await()
            .documents
            .mapNotNull { it.toObject(AppVersion::class.java) }
        return paginated(versions, limit) { version -> version.versionCursor() }
    }

    suspend fun getVersion(appId: String, versionId: String): AppVersion? {
        val snapshot = versionDocument(appId, versionId).get().await()
        return if (snapshot.exists()) snapshot.toObject(AppVersion::class.java) else null
    }

    /** Updates only the free-text note on a version. Callers check existence/ownership first. */
    suspend fun updateVersionNote(version: AppVersion) {
        versionDocument(version.appId, version.id)
            .update("note", version.note)
            .await()
    }

    /**
     * Deletes a single version exactly once. The Firestore version delete, storage counter
     * decrement, and parent aggregate recompute happen in one transaction. The parent app is
     * kept even when its last version is removed; use [deleteApp] to remove the app itself.
     * Backblaze B2 object cleanup runs afterwards and remains best-effort.
     */
    suspend fun deleteVersion(
        appId: String,
        versionId: String,
        requesterUid: String
    ): DeleteVersionResult {
        val appRef = appDocument(appId)
        val versionRef = versionDocument(appId, versionId)
        val result = firestore.runTransaction { txn ->
            val version = txn.get(versionRef).get().toObject(AppVersion::class.java)
                ?: return@runTransaction DeleteVersionResult.NotFound
            if (version.ownerUserId != requesterUid) return@runTransaction DeleteVersionResult.Forbidden

            val app = txn.get(appRef).get().toObject(App::class.java)
            val versions = txn.get(versionsCollection(appId))
                .get()
                .documents
                .mapNotNull { it.toObject(AppVersion::class.java) }
            val remaining = versions.filterNot { it.id == version.id }

            releaseStorageInTransaction(
                txn,
                userDocument(version.ownerUserId),
                version.fileSizeBytes
            )
            txn.delete(versionRef)
            if (app != null) {
                txn.set(appRef, app.withRecomputedVersionAggregate(remaining))
            }
            DeleteVersionResult.Deleted(version)
        }.await()

        if (result is DeleteVersionResult.Deleted) {
            deleteBlob(result.version.storagePath)
        }
        return result
    }

    internal fun AppVersion.versionCursor(): Pair<Long, String> = updatedAt to id

    /**
     * Atomically records a new upload: upserts the parent [App] (created on first upload of
     * its applicationId, otherwise its versionCount/updatedAt — and, when this build is the
     * newest seen, its label/latest* fields — are bumped) and writes the [version] doc.
     * Duplicate version names/codes are intentionally allowed; each upload is its own row.
     */
    suspend fun addVersion(
        version: AppVersion,
        appLabel: String,
        ownerName: String,
        imageUrl: String,
    ): App {
        val appRef = appDocument(version.appId)
        val versionRef = versionDocument(version.appId, version.id)
        return firestore.runTransaction { txn ->
            val existing = txn.get(appRef).get().toObject(App::class.java)
            val isCurrent = existing == null || version.versionCode >= existing.latestVersionCode
            val shareId = existing?.shareId?.takeIf { it.isNotBlank() } ?: newShareId()
            val app = App(
                id = version.appId,
                applicationId = version.applicationId,
                ownerUserId = version.ownerUserId,
                ownerName = ownerName,
                label = if (isCurrent) appLabel else existing.label,
                latestVersionName = if (isCurrent) version.versionName else existing.latestVersionName,
                latestVersionCode = if (isCurrent) version.versionCode else existing.latestVersionCode,
                versionCount = (existing?.versionCount ?: 0) + 1,
                // The icon is captured once; keep whatever we already have if this upload didn't add one.
                imageUrl = imageUrl.ifBlank { existing?.imageUrl ?: "" },
                shareId = shareId,
                createdAt = existing?.createdAt ?: version.createdAt,
                updatedAt = version.updatedAt,
            )
            txn.set(appRef, app)
            txn.set(versionRef, version)
            app
        }.await()
    }

    private fun newShareId(): String {
        val bytes = ByteArray(18)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private data class PageCursor(val updatedAt: Long, val id: String)

    private fun sanitizePageSize(pageSize: Int?): Int =
        pageSize?.coerceIn(1, MaxPageSize) ?: DefaultPageSize

    private fun Query.withCursor(pageToken: String?): Query {
        val cursor = pageToken?.let(::decodePageToken) ?: return this
        return startAfter(cursor.updatedAt, cursor.id)
    }

    private fun List<App>.afterCursor(pageToken: String?): List<App> {
        val cursor = pageToken?.let(::decodePageToken) ?: return this
        return dropWhile { app ->
            app.updatedAt > cursor.updatedAt || (app.updatedAt == cursor.updatedAt && app.id <= cursor.id)
        }
    }

    private fun <T> paginated(
        rows: List<T>,
        pageSize: Int,
        cursorValue: (T) -> Pair<Long, String>,
    ): PaginatedResponse<T> {
        val items = rows.take(pageSize)
        val nextPageToken = if (rows.size > pageSize) {
            items.lastOrNull()?.let { item ->
                val (updatedAt, id) = cursorValue(item)
                encodePageToken(updatedAt, id)
            }
        } else {
            null
        }
        return PaginatedResponse(items = items, nextPageToken = nextPageToken)
    }

    private fun encodePageToken(updatedAt: Long, id: String): String {
        val raw = "$updatedAt\n$id"
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.toByteArray(Charsets.UTF_8))
    }

    private fun decodePageToken(token: String): PageCursor {
        val raw = try {
            String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid pageToken")
        }
        val separator = raw.indexOf('\n')
        if (separator <= 0 || separator == raw.lastIndex) throw IllegalArgumentException("Invalid pageToken")
        val updatedAt = raw.substring(0, separator).toLongOrNull()
            ?: throw IllegalArgumentException("Invalid pageToken")
        val id = raw.substring(separator + 1)
        return PageCursor(updatedAt = updatedAt, id = id)
    }

    // --- Storage ---

    suspend fun reconcileObjectStorage(
        deleteOrphans: Boolean,
        sampleLimit: Int = 100
    ): StorageReconciliationReport {
        val boundedSampleLimit = sampleLimit.coerceIn(0, 500)
        val expectedPaths = expectedManagedStoragePaths()
        val objects = listManagedStorageObjects()
        val objectPaths = objects.map { it.path }.toSet()
        val orphanObjects = objects.filter { it.path !in expectedPaths }
        val missingPaths = (expectedPaths - objectPaths).sorted()

        val deletedPaths = mutableListOf<String>()
        val failedDeletePaths = mutableListOf<String>()
        if (deleteOrphans) {
            for (orphan in orphanObjects) {
                if (deleteBlob(orphan.path)) {
                    deletedPaths += orphan.path
                } else {
                    failedDeletePaths += orphan.path
                }
            }
        }

        val orphanSummaries = orphanObjects
            .sortedBy { it.path }
            .take(boundedSampleLimit)
            .map { it.toStorageObjectSummary() }
        val missingSample = missingPaths.take(boundedSampleLimit)
        return StorageReconciliationReport(
            dryRun = !deleteOrphans,
            scannedObjectCount = objects.size,
            scannedBytes = objects.sumOf { it.sizeBytes },
            expectedObjectCount = expectedPaths.size,
            orphanCount = orphanObjects.size,
            orphanBytes = orphanObjects.sumOf { it.sizeBytes },
            missingCount = missingPaths.size,
            deletedOrphanCount = deletedPaths.size,
            failedDeleteCount = failedDeletePaths.size,
            orphans = orphanSummaries,
            missing = missingSample,
            truncated = orphanObjects.size > boundedSampleLimit || missingPaths.size > boundedSampleLimit,
        )
    }

    private suspend fun expectedManagedStoragePaths(): Set<String> {
        val paths = mutableSetOf<String>()
        firestore.collection(CollectionApps)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(App::class.java) }
            .filter { it.imageUrl.isNotBlank() }
            .forEach { app -> paths += iconStoragePath(app.ownerUserId, app.applicationId) }

        firestore.collectionGroup(CollectionVersions)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(AppVersion::class.java) }
            .mapNotNull { it.expectedStoragePath() }
            .forEach { path -> paths += path }
        return paths
    }

    private fun AppVersion.expectedStoragePath(): String? {
        storagePath.takeIf { it.isNotBlank() }?.let { return it }
        if (ownerUserId.isBlank() || applicationId.isBlank() || id.isBlank()) return null
        return apkStoragePath(ownerUserId, applicationId, id, fileName)
    }

    private suspend fun listManagedStorageObjects(): List<StoredObjectInfo> =
        objectStorage.list(ApkStoragePrefix) + objectStorage.list(IconStoragePrefix)

    private fun StoredObjectInfo.toStorageObjectSummary(): StorageObjectSummary =
        StorageObjectSummary(path = path, sizeBytes = sizeBytes, kind = managedStorageKind(path))

    suspend fun uploadBytes(path: String, bytes: ByteArray, contentType: String) {
        objectStorage.put(path, bytes, contentType)
    }

    suspend fun getBlob(path: String): StoredObject? =
        objectStorage.get(path)

    suspend fun deleteBlob(path: String): Boolean =
        objectStorage.delete(path)
}
