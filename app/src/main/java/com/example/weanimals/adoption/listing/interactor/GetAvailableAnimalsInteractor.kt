package com.example.weanimals.adoption.listing.interactor

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.example.weanimals.adoption.listing.repository.AnimalRepository
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.domain.UserLocationUnavailableException
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAvailableAnimalsInteractor(
    private val repository: AnimalRepository,
    private val calculateDistance: CalculateDistanceInteractor = CalculateDistanceInteractor()
) {
    suspend operator fun invoke(
        filter: SpeciesFilter,
        currentPage: AnimalPageCursor? = null,
        location: Coordinates? = null,
        limit: Int = 10
    ): Result<AnimalPage> = try {
        require(limit in 1..100)
        val page = if (filter == SpeciesFilter.NEAREST) {
            val origin = location ?: throw UserLocationUnavailableException()
            nearestPage(currentPage, origin, limit)
        } else {
            val page = repository.getAvailableAnimals(filter, currentPage, limit).getOrThrow()
            page.copy(animals = page.animals.map { withDistance(it, location) })
        }
        Result.success(page)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun nearestPage(
        cursor: AnimalPageCursor?,
        origin: Coordinates,
        limit: Int
    ): AnimalPage {
        require(cursor == null || cursor is DistanceCursor)
        val state = cursor as? DistanceCursor ?: run {
            val animals = mutableListOf<Animal>()
            var next: AnimalPageCursor? = null
            do {
                val page = repository.getAvailableAnimals(
                    filter = null, lastDocument = next, limit = limit
                ).getOrThrow()
                animals.addAll(page.animals)
                next = page.nextPage
            } while (next != null)
            // Sort the entire available catalog, not each Firestore page separately.
            // Subsequent scroll pages reuse this snapshot without re-reading Firestore.
            val sorted = withContext(Dispatchers.Default) {
                animals.distinctBy { it.id }.map { withDistance(it, origin) }
                    .sortedWith(
                        compareBy<Animal> { it.distanceKm ?: Double.POSITIVE_INFINITY }
                            .thenByDescending { it.createdAtMillis }.thenBy { it.id }
                    )
            }
            DistanceCursor(sorted, 0)
        }
        val end = (state.offset + limit).coerceAtMost(state.animals.size)
        return AnimalPage(
            animals = state.animals.subList(state.offset, end),
            nextPage = if (end < state.animals.size) state.copy(offset = end) else null
        )
    }

    private fun withDistance(animal: Animal, origin: Coordinates?): Animal {
        val shelter = Coordinates.fromOrNull(animal.latitude, animal.longitude)
        return animal.copy(
            distanceKm = if (origin != null && shelter != null) {
                calculateDistance(origin, shelter)
            } else {
                animal.distanceKm
            }
        )
    }

    private data class DistanceCursor(val animals: List<Animal>, val offset: Int) : AnimalPageCursor
}
