package com.example.weanimals.profile.favorites.repository

interface FavoriteRepository {
    suspend fun getFavoriteAnimalIds(): Result<List<String>>
    suspend fun isFavorite(animalId: String): Result<Boolean>
    suspend fun setFavorite(animalId: String, favorite: Boolean): Result<Unit>
}
