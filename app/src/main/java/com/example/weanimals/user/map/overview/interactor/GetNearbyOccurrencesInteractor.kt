package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.user.map.overview.domain.PublicOccurrence
import com.example.weanimals.user.map.overview.repository.PublicOccurrenceRepository
import kotlinx.coroutines.CancellationException

data class NearbyOccurrences(val userLocation: Coordinates?, val occurrences: List<PublicOccurrence>, val sourceError: Boolean = false)

class GetNearbyOccurrencesInteractor(
    private val repository: PublicOccurrenceRepository,
    private val getUserLocation: GetUserLocationInteractor,
    private val calculateDistance: CalculateDistanceInteractor,
    private val restrictToUserRadius: Boolean = true
) {
    suspend operator fun invoke(): Result<NearbyOccurrences> = try {
        val location = getUserLocation()
        if (location == null && restrictToUserRadius) Result.success(NearbyOccurrences(null, emptyList()))
        else {
            val response = repository.getOccurrences()
            val error = response.exceptionOrNull()
            if (error != null) {
                if (error is CancellationException || restrictToUserRadius) throw error
                // GPS remains usable when the public report source is temporarily unavailable.
                Result.success(NearbyOccurrences(location, emptyList(), sourceError = true))
            } else {
                val nearby = response.getOrThrow()
                    .map { it.copy(distanceKm = location?.let { user -> calculateDistance(user, it.coordinates) }) }
                    .filter { !restrictToUserRadius || (it.distanceKm ?: Double.MAX_VALUE) <= RADIUS_KM }
                    .sortedBy { it.distanceKm }
                Result.success(NearbyOccurrences(location, nearby))
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object { const val RADIUS_KM = 2.0 }
}
