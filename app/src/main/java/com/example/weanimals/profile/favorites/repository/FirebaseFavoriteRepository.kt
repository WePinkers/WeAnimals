package com.example.weanimals.profile.favorites.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseFavoriteRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FavoriteRepository {
    private suspend fun userId(): String = auth.currentUser?.uid
        ?: auth.signInAnonymously().await().user?.uid
        ?: error("Firebase user was not created.")

    override suspend fun getFavoriteAnimalIds(): Result<List<String>> = try {
        val documents = firestore.collection("user_favorites").document(userId())
            .collection("animals").get(Source.SERVER).await().documents
        Result.success(documents.map { it.id })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun isFavorite(animalId: String): Result<Boolean> = try {
        require(animalId.isNotBlank())
        val exists = firestore.collection("user_favorites").document(userId())
            .collection("animals").document(animalId).get(Source.SERVER).await().exists()
        Result.success(exists)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun setFavorite(animalId: String, favorite: Boolean): Result<Unit> = try {
        require(animalId.isNotBlank())
        val document = firestore.collection("user_favorites").document(userId())
            .collection("animals").document(animalId)
        if (favorite) document.set(mapOf("animalId" to animalId, "createdAt" to FieldValue.serverTimestamp())).await()
        else document.delete().await()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
