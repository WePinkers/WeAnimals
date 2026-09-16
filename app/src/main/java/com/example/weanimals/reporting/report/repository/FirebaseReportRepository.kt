package com.example.weanimals.reporting.report.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.weanimals.reporting.report.domain.NewReport
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.domain.ReportProtocol
import com.example.weanimals.reporting.report.domain.ReportStatus
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.map.overview.domain.PublicOccurrence
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class FirebaseReportRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ReportRepository {

    override fun observeCurrentUserReports(): Flow<Result<List<Report>>> = callbackFlow {
        val user = try {
            ensureSignedIn()
        } catch (exception: Exception) {
            trySend(Result.failure(exception))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(REPORTS_COLLECTION)
            .whereEqualTo(FIELD_USER_ID, user.uid)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot != null -> {
                        val reports = snapshot.documents
                            .mapNotNull { document -> document.toReportOrNull() }
                            .sortedByDescending(Report::createdAtMillis)
                        trySend(Result.success(reports))
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    override fun observeReport(
        reportId: String,
        includePhotoData: Boolean
    ): Flow<Result<Report>> = callbackFlow {
        if (reportId.isBlank()) {
            trySend(Result.failure(IllegalArgumentException("Report id is required.")))
            close()
            return@callbackFlow
        }

        val user = try {
            ensureSignedIn()
        } catch (exception: Exception) {
            trySend(Result.failure(exception))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null || !snapshot.exists() -> {
                        trySend(Result.failure(NoSuchElementException("Report not found.")))
                    }
                    else -> {
                        val report = snapshot.toReportOrNull(includePhotoData)
                        if (report == null || report.userId != user.uid) {
                            trySend(Result.failure(SecurityException("Report does not belong to the current user.")))
                        } else {
                            trySend(Result.success(report))
                        }
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    override suspend fun markReportAsViewed(reportId: String): Result<Unit> = runCatching {
        require(reportId.isNotBlank()) { "Report id is required." }
        ensureSignedIn()
        firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .update(FIELD_IS_VIEWED, true)
            .await()
    }

    override suspend fun submitReport(report: NewReport): Result<Report> = runCatching {
        val user = ensureSignedIn()
        val photoBytes = report.photoUri?.let { photoUri ->
            withContext(Dispatchers.IO) { compressPhoto(photoUri) }
        }
        val photoFileName = report.photoUri?.let { photoUri ->
            withContext(Dispatchers.IO) { resolvePhotoFileName(photoUri) }
        }
        val protocolNumber = issueProtocol()
        saveReport(report, user, protocolNumber, photoBytes, photoFileName)
    }

    private suspend fun ensureSignedIn(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Firebase user was not created.")
    }

    private fun compressPhoto(photoUri: String): ByteArray {
        val resolver = context.contentResolver
        val sourceUri = Uri.parse(photoUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "The selected file is not a valid image."
        }

        var sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        var bitmap = decodeBitmap(resolver, sourceUri, sampleSize)
        try {
            while (true) {
                val compressed = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, compressed)) {
                    "The selected image could not be compressed."
                }
                val bytes = compressed.toByteArray()
                if (bytes.size <= MAX_PHOTO_BYTES) return bytes

                bitmap.recycle()
                sampleSize *= 2
                require(sampleSize <= MAX_SAMPLE_SIZE) { "The selected image is too large." }
                bitmap = decodeBitmap(resolver, sourceUri, sampleSize)
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun resolvePhotoFileName(photoUri: String): String {
        val uri = Uri.parse(photoUri)
        val displayName = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
        return displayName?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: DEFAULT_PHOTO_FILE_NAME
    }

    private fun decodeBitmap(
        resolver: android.content.ContentResolver,
        sourceUri: Uri,
        sampleSize: Int
    ): Bitmap {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return resolver.openInputStream(sourceUri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: error("The selected image could not be read.")
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_PHOTO_DIMENSION
            || height / sampleSize > MAX_PHOTO_DIMENSION
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private suspend fun issueProtocol(): Int {
        val counterReference = firestore.collection(METADATA_COLLECTION)
            .document(REPORT_COUNTER_DOCUMENT)
        return firestore.runTransaction { transaction ->
            val counter = transaction.get(counterReference)
            val nextProtocol = counter.getLong(FIELD_NEXT_PROTOCOL)?.toInt()
                ?: ReportProtocol.FIRST_NUMBER
            transaction.set(
                counterReference,
                mapOf(FIELD_NEXT_PROTOCOL to nextProtocol + 1),
                SetOptions.merge()
            )
            nextProtocol
        }.await()
    }

    private suspend fun saveReport(
        report: NewReport,
        user: FirebaseUser,
        protocolNumber: Int,
        photoBytes: ByteArray?,
        photoFileName: String?
    ): Report {
        val documentReference = firestore.collection(REPORTS_COLLECTION).document()
        val now = Timestamp.now()
        val data = hashMapOf<String, Any?>(
            FIELD_PROTOCOL to protocolNumber,
            FIELD_USER_ID to user.uid,
            FIELD_ANIMAL_TYPE to report.animalType,
            FIELD_URGENCY to report.urgency,
            FIELD_DESCRIPTION to report.description,
            FIELD_ADDRESS to report.address,
            FIELD_LATITUDE to report.latitude,
            FIELD_LONGITUDE to report.longitude,
            FIELD_STATUS to ReportStatus.NEW,
            FIELD_TRIAGE_CLASSIFICATION to report.triageClassification,
            FIELD_TRIAGE_ANSWERS to report.triageAnswers,
            FIELD_IS_VIEWED to false,
            FIELD_CREATED_AT to now,
            FIELD_UPDATED_AT to now,
            FIELD_ASSIGNED_TEAM_ID to null
        )
        photoBytes?.let {
            data[FIELD_PHOTO_DATA] = Blob.fromBytes(it)
            data[FIELD_PHOTO_NAME] = photoFileName ?: DEFAULT_PHOTO_FILE_NAME
        }
        val batch = firestore.batch()
        batch.set(documentReference, data)
        Coordinates.fromOrNull(report.latitude, report.longitude)
            ?.takeIf { it.latitude < 90.0 && it.longitude < 180.0 }
            ?.let { coordinates ->
            batch.set(
                firestore.collection(PUBLIC_OCCURRENCES_COLLECTION).document(documentReference.id),
                mapOf(
                    "animalType" to report.animalType,
                    "urgency" to report.urgency,
                    "latitudeCell" to PublicOccurrence.cell(coordinates.latitude),
                    "longitudeCell" to PublicOccurrence.cell(coordinates.longitude),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()

        return Report(
            id = documentReference.id,
            protocolNumber = protocolNumber,
            userId = user.uid,
            animalType = report.animalType,
            urgency = report.urgency,
            description = report.description,
            photoFileName = photoFileName,
            photoData = null,
            hasPhoto = photoBytes != null,
            address = report.address,
            latitude = report.latitude,
            longitude = report.longitude,
            status = ReportStatus.NEW,
            triageClassification = report.triageClassification,
            triageAnswers = report.triageAnswers,
            createdAtMillis = now.toDate().time,
            updatedAtMillis = now.toDate().time,
            isViewed = false
        )
    }

    private fun DocumentSnapshot.toReportOrNull(includePhotoData: Boolean = false): Report? {
        return try {
            val photoBlob = get(FIELD_PHOTO_DATA) as? Blob
            Report(
                id = id,
                protocolNumber = getLong(FIELD_PROTOCOL)?.toInt() ?: return null,
                userId = getString(FIELD_USER_ID).orEmpty(),
                animalType = getString(FIELD_ANIMAL_TYPE) ?: Report.ANIMAL_OTHER,
                urgency = getString(FIELD_URGENCY) ?: Report.URGENCY_MEDIUM,
                description = getString(FIELD_DESCRIPTION).orEmpty(),
                photoFileName = getString(FIELD_PHOTO_NAME),
                photoData = if (includePhotoData) photoBlob?.toBytes() else null,
                hasPhoto = photoBlob != null
                    || !getString(FIELD_PHOTO_URL).isNullOrBlank(),
                photoUrl = getString(FIELD_PHOTO_URL),
                address = getString(FIELD_ADDRESS).orEmpty(),
                latitude = getDouble(FIELD_LATITUDE),
                longitude = getDouble(FIELD_LONGITUDE),
                status = getString(FIELD_STATUS) ?: ReportStatus.NEW,
                triageClassification = getString(FIELD_TRIAGE_CLASSIFICATION),
                triageAnswers = readTriageAnswers(),
                createdAtMillis = getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
                updatedAtMillis = getTimestamp(FIELD_UPDATED_AT)?.toDate()?.time ?: 0L,
                isViewed = getBoolean(FIELD_IS_VIEWED) ?: false
            )
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun DocumentSnapshot.readTriageAnswers(): Map<String, Boolean> {
        return (get(FIELD_TRIAGE_ANSWERS) as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                if (key is String && value is Boolean) key to value else null
            }
            ?.toMap()
            ?: emptyMap()
    }

    private companion object {
        const val REPORTS_COLLECTION = "reports"
        const val PUBLIC_OCCURRENCES_COLLECTION = "public_occurrences"
        const val METADATA_COLLECTION = "metadata"
        const val REPORT_COUNTER_DOCUMENT = "reports_counter"
        const val FIELD_PROTOCOL = "protocolNumber"
        const val FIELD_USER_ID = "userId"
        const val FIELD_ANIMAL_TYPE = "animalType"
        const val FIELD_URGENCY = "urgency"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_PHOTO_DATA = "photoData"
        const val FIELD_PHOTO_NAME = "photoFileName"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_ADDRESS = "address"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_STATUS = "status"
        const val FIELD_TRIAGE_CLASSIFICATION = "triageClassification"
        const val FIELD_TRIAGE_ANSWERS = "triageAnswers"
        const val FIELD_IS_VIEWED = "isViewed"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_ASSIGNED_TEAM_ID = "assignedTeamId"
        const val FIELD_NEXT_PROTOCOL = "nextProtocol"
        const val MAX_PHOTO_BYTES = 700 * 1024
        const val MAX_PHOTO_DIMENSION = 1280
        const val MAX_SAMPLE_SIZE = 64
        const val PHOTO_QUALITY = 75
        const val DEFAULT_PHOTO_FILE_NAME = "foto_enviada.jpg"
    }
}
