package com.example.weanimals.map.overview.repository

import com.example.weanimals.map.overview.domain.PublicOccurrence
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebasePublicOccurrenceRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PublicOccurrenceRepository {
    override suspend fun getOccurrences(): Result<List<PublicOccurrence>> = try {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val documents = firestore.collection("public_occurrences")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .get(Source.SERVER).await().documents
        Result.success(documents.mapNotNull { document ->
            val latitudeCell = document.getLong("latitudeCell")?.toInt() ?: return@mapNotNull null
            val longitudeCell = document.getLong("longitudeCell")?.toInt() ?: return@mapNotNull null
            if (latitudeCell !in -9000..8999 || longitudeCell !in -18000..17999) return@mapNotNull null
            PublicOccurrence(
                id = document.id,
                animalType = document.getString("animalType").orEmpty(),
                urgency = document.getString("urgency").orEmpty(),
                latitudeCell = latitudeCell,
                longitudeCell = longitudeCell,
                createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            )
        })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
