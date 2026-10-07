package com.example.weanimals.reporting.report.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.weanimals.reporting.report.domain.NewReport
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.domain.ReportProtocol
import com.example.weanimals.reporting.report.domain.ReportStatus
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.notification.ReportStatusNotifier
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
import java.util.concurrent.CopyOnWriteArrayList

class FirebaseReportRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ReportRepository {

    private val localSessionReports = CopyOnWriteArrayList<Report>()

    override fun observeCurrentUserReports(): Flow<Result<List<Report>>> = callbackFlow {
        try {
            ensureSignedIn()
        } catch (exception: Exception) {
            Log.e(TAG, "Error signing in for observeCurrentUserReports", exception)
            trySend(Result.success(getCombinedReports(emptyList(), emptyList())))
            close()
            return@callbackFlow
        }

        var currentFirestoreReports = emptyList<Report>()
        var currentPublicOccurrences = emptyList<PublicOccurrence>()

        fun emitCombined() {
            val publicReports = currentPublicOccurrences.map { it.toReport() }
            val combined = getCombinedReports(currentFirestoreReports, publicReports)
            trySend(Result.success(combined))
        }

        val reportsRegistration = firestore.collection(REPORTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore error in reports collection listener", error)
                } else if (snapshot != null) {
                    currentFirestoreReports = snapshot.documents.mapNotNull { it.toReportOrNull() }
                }
                emitCombined()
            }

        val publicRegistration = firestore.collection(PUBLIC_OCCURRENCES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore error in public_occurrences collection listener", error)
                } else if (snapshot != null) {
                    currentPublicOccurrences = snapshot.documents.mapNotNull { doc ->
                        val latitudeCell = doc.getLong("latitudeCell")?.toInt() ?: return@mapNotNull null
                        val longitudeCell = doc.getLong("longitudeCell")?.toInt() ?: return@mapNotNull null
                        PublicOccurrence(
                            id = doc.id,
                            animalType = doc.getString("animalType").orEmpty(),
                            urgency = doc.getString("urgency").orEmpty(),
                            latitudeCell = latitudeCell,
                            longitudeCell = longitudeCell,
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        )
                    }
                }
                emitCombined()
            }

        awaitClose {
            reportsRegistration.remove()
            publicRegistration.remove()
        }
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

        try {
            ensureSignedIn()
        } catch (exception: Exception) {
            Log.e(TAG, "Error signing in for observeReport", exception)
            trySend(Result.failure(exception))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> {
                        Log.e(TAG, "Firestore error in observeReport", error)
                        trySend(Result.failure(error))
                    }
                    snapshot == null || !snapshot.exists() -> {
                        val localReport = localSessionReports.firstOrNull { it.id == reportId }
                        if (localReport != null) {
                            trySend(Result.success(localReport))
                        } else {
                            trySend(Result.failure(NoSuchElementException("Report not found.")))
                        }
                    }
                    else -> {
                        val report = snapshot.toReportOrNull(includePhotoData)
                        if (report == null) {
                            trySend(Result.failure(SecurityException("Report not found.")))
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
        localSessionReports.indexOfFirst { it.id == reportId }.takeIf { it >= 0 }?.let { index ->
            localSessionReports[index] = localSessionReports[index].copy(isViewed = true)
        }
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
        val savedReport = saveReport(report, user, protocolNumber, photoBytes, photoFileName)
        localSessionReports.add(0, savedReport)
        Log.d("DEBUG_DENUNCIAS", "submitReport adicionou denúncia local. Protocolo=#${savedReport.protocolNumber}, Endereço=${savedReport.address}")

        val animalTypeLabel = if (savedReport.animalType == "cat") "Gato" else "Cão"
        val animalAndLocation = "$animalTypeLabel — ${savedReport.address.ifBlank { "Localização registrada" }}"

        // Emit EXCLUSIVELY ONE initial notification: "Denúncia recebida"
        ReportStatusNotifier.notifyReportCreated(
            context = context,
            reportId = savedReport.id,
            protocolNumber = savedReport.protocolNumber,
            animalTypeAndLocation = animalAndLocation
        )

        savedReport
    }

    private fun getCombinedReports(
        firestoreReports: List<Report>,
        publicReports: List<Report>
    ): List<Report> {
        val combinedMap = mutableMapOf<String, Report>()

        // 1. Add Public Occurrences mapped to Reports (same occurrences that feed the map!)
        publicReports.forEach { combinedMap[it.id] = it }

        // 2. Add Firestore reports (overwrites with detailed report info if document exists in reports)
        firestoreReports.forEach { combinedMap[it.id] = it }

        // 3. Add locally created session reports
        localSessionReports.forEach { combinedMap[it.id] = it }

        val resultList = combinedMap.values.sortedByDescending { it.createdAtMillis }

        Log.d("DEBUG_DENUNCIAS", "Repositório emitindo denúncias unificadas. Total de itens: ${resultList.size}")
        resultList.forEachIndexed { index, item ->
            Log.d("DEBUG_DENUNCIAS", "Item [$index]: ID=${item.id}, Endereço=${item.address}, Protocolo=#${item.protocolNumber}")
        }

        return resultList
    }

    private fun PublicOccurrence.toReport(): Report {
        val numericProtocol = id.filter { it.isDigit() }.toIntOrNull()
            ?: (Math.abs(id.hashCode()) % 8000 + 1000)

        val animalLabel = when (animalType) {
            "cat" -> "Gato"
            "dog" -> "Cão"
            else -> "Animal"
        }

        return Report(
            id = id,
            protocolNumber = numericProtocol,
            userId = "user_demo",
            animalType = animalType,
            urgency = urgency,
            description = "Ocorrência registrada no mapa.",
            address = "$animalLabel — Perto de você",
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            status = ReportStatus.NEW,
            createdAtMillis = if (createdAtMillis > 0L) createdAtMillis else System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            isViewed = false
        )
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
        const val TAG = "WeAnimalsReports"
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
