package com.example.weanimals.user.adoption.applications.repository

import com.example.weanimals.user.adoption.applications.domain.MyAdoptionApplication
import com.example.weanimals.user.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.user.adoption.sent.domain.AdoptionSentSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAdoptionApplicationsRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AdoptionApplicationsRepository {

    override fun observe(): Flow<Result<List<MyAdoptionApplication>>> = callbackFlow {
        val userId = try {
            if (auth.currentUser == null) auth.signInAnonymously().await()
            requireNotNull(auth.currentUser?.uid)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            trySend(Result.failure(error))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("adoption_applications")
            .document(userId)
            .collection("animals")
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot != null -> trySend(Result.success(snapshot.toApplications()))
                }
            }

        awaitClose { registration.remove() }
    }

    private fun QuerySnapshot.toApplications(): List<MyAdoptionApplication> = documents
        .filter { it.getString("status") != AdoptionApplicationStatus.CANCELLED.storageValue }
        .mapNotNull { document ->
            val animalId = document.getString("animalId")?.takeIf(String::isNotBlank)
                ?: document.id.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val animalName = document.getString("animalName")?.trim().orEmpty()
                .ifBlank { "Animal" }
            val status = AdoptionApplicationStatus.entries.firstOrNull {
                it.storageValue == document.getString("status")
            } ?: AdoptionApplicationStatus.PENDING
            MyAdoptionApplication(
                summary = AdoptionSentSummary(
                    animalId = animalId,
                    animalName = animalName,
                    shelterName = document.getString("shelterName").orEmpty(),
                    matchPercentage = document.getLong("matchPercentage")?.toInt() ?: 0,
                    breed = document.getString("breed").orEmpty(),
                    ageText = document.getString("ageText").orEmpty(),
                    photoUrl = document.getString("photoUrl")?.takeIf(String::isNotBlank),
                ),
                status = status,
                submittedAtMillis = document.getTimestamp("submittedAt")?.toDate()?.time ?: 0L,
            )
        }
        .sortedWith(compareByDescending<MyAdoptionApplication> { it.submittedAtMillis }
            .thenBy { it.summary.animalName.lowercase() })
}
