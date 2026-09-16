package com.example.weanimals.profile.favorites.interactor

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.repository.AnimalRepository
import com.example.weanimals.profile.favorites.repository.FavoriteRepository
import kotlinx.coroutines.CancellationException

class GetFavoriteAnimalsInteractor(
    private val favorites: FavoriteRepository,
    private val animals: AnimalRepository
) {
    suspend operator fun invoke(): Result<List<Animal>> = try {
        val ids = favorites.getFavoriteAnimalIds().getOrThrow()
        val available = ids.mapNotNull { id -> animals.getAvailableAnimalById(id).getOrNull() }
        Result.success(available)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
