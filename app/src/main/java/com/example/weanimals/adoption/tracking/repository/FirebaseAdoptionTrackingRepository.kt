package com.example.weanimals.adoption.tracking.repository

import com.example.weanimals.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.adoption.tracking.domain.AdoptionApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAdoptionTrackingRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AdoptionTrackingRepository {

    override fun observe(summary: AdoptionSentSummary): Flow<Result<AdoptionApplication>> = callbackFlow {
        val reference = try {
            applicationReference(summary.animalId)
        } catch (error: Exception) {
            trySend(Result.failure(error))
            close()
            return@callbackFlow
        }
        val registration = reference.addSnapshotListener { document, error ->
            when {
                error != null -> trySend(Result.failure(error))
                document != null -> trySend(runCatching { document.toApplication(summary) })
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun withdraw(animalId: String): Result<Unit> = try {
        val reference = applicationReference(animalId)
        reference.update(mapOf(
            "status" to AdoptionApplicationStatus.CANCELLED.storageValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun applicationReference(animalId: String): DocumentReference {
        require(animalId.isNotBlank()) { "Animal id is required." }
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val userId = requireNotNull(auth.currentUser?.uid)
        return firestore.collection("adoption_applications")
            .document(userId).collection("animals").document(animalId)
    }

    private fun DocumentSnapshot.toApplication(fallback: AdoptionSentSummary): AdoptionApplication {
        check(exists()) { "Application not found." }
        val status = AdoptionApplicationStatus.entries.firstOrNull {
            it.storageValue == getString("status")
        } ?: error("Unknown application status.")
        return AdoptionApplication(
            summary = fallback.copy(
                animalName = getString("animalName")?.takeIf(String::isNotBlank) ?: fallback.animalName,
                shelterName = getString("shelterName")?.takeIf(String::isNotBlank) ?: fallback.shelterName,
                matchPercentage = getLong("matchPercentage")?.toInt() ?: fallback.matchPercentage,
                breed = getString("breed") ?: fallback.breed,
                ageText = getString("ageText") ?: fallback.ageText,
                photoUrl = getString("photoUrl")?.takeIf(String::isNotBlank) ?: fallback.photoUrl
            ),
            status = status,
            submittedAtMillis = getTimestamp("submittedAt")?.toDate()?.time ?: 0L,
            updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        )
    }
}
