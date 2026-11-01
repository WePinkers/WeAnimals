package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.core.location.repository.UserLocationRepository
import com.example.weanimals.user.map.overview.domain.PublicOccurrence
import com.example.weanimals.user.map.overview.repository.PublicOccurrenceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetNearbyOccurrencesInteractorTest {
    @Test
    fun negativeCoordinateUsesFloorForPrivacyCell() {
        assertEquals(-2356, PublicOccurrence.cell(-23.5505))
        assertEquals(-4664, PublicOccurrence.cell(-46.6333))
    }

    @Test
    fun returnsOnlyNearbyPointsSortedByDistance() = runTest {
        val location = Coordinates(-23.5505, -46.6333)
        val repository = FakeRepository(listOf(
            point("far", -2350, -4650),
            point("near", -2356, -4664),
            point("closer", -2355, -4664)
        ))
        val interactor = GetNearbyOccurrencesInteractor(
            repository,
            GetUserLocationInteractor(FakeLocation(location)),
            CalculateDistanceInteractor()
        )

        val result = interactor().getOrThrow()

        assertEquals(listOf("near", "closer"), result.occurrences.map { it.id })
        assertEquals(location, result.userLocation)
    }

    @Test
    fun noLocationDoesNotExposeUnfilteredPublicPoints() = runTest {
        val repository = FakeRepository(listOf(point("near", -2356, -4664)))
        val result = GetNearbyOccurrencesInteractor(
            repository,
            GetUserLocationInteractor(FakeLocation(null)),
            CalculateDistanceInteractor()
        )().getOrThrow()

        assertNull(result.userLocation)
        assertEquals(emptyList<PublicOccurrence>(), result.occurrences)
        assertEquals(0, repository.calls)
    }

    @Test
    fun mapLoadsPublicPointsWithoutGpsAndOutsideTheHomeRadius() = runTest {
        val points = listOf(point("far", -2350, -4650), point("near", -2356, -4664))
        val noGps = GetNearbyOccurrencesInteractor(FakeRepository(points),
            GetUserLocationInteractor(FakeLocation(null)), CalculateDistanceInteractor(),
            restrictToUserRadius = false)().getOrThrow()
        assertEquals(2, noGps.occurrences.size)
        assertNull(noGps.occurrences.first().distanceKm)
        val withGps = GetNearbyOccurrencesInteractor(FakeRepository(points),
            GetUserLocationInteractor(FakeLocation(Coordinates(-23.5505, -46.6333))),
            CalculateDistanceInteractor(), restrictToUserRadius = false)().getOrThrow()
        assertEquals(2, withGps.occurrences.size)
    }

    @Test
    fun mapKeepsGpsWhenTheReportServerFails() = runTest {
        val location = Coordinates(-23.55, -46.63)
        val failing = object : PublicOccurrenceRepository {
            override suspend fun getOccurrences() = Result.failure<List<PublicOccurrence>>(IllegalStateException("offline"))
        }
        val result = GetNearbyOccurrencesInteractor(failing,
            GetUserLocationInteractor(FakeLocation(location)), CalculateDistanceInteractor(),
            restrictToUserRadius = false)().getOrThrow()
        assertEquals(location, result.userLocation)
        assertEquals(true, result.sourceError)
        assertEquals(emptyList<PublicOccurrence>(), result.occurrences)
    }

    private fun point(id: String, latitudeCell: Int, longitudeCell: Int) = PublicOccurrence(
        id, "dog", "high", latitudeCell, longitudeCell, 1L
    )

    private class FakeRepository(private val points: List<PublicOccurrence>) : PublicOccurrenceRepository {
        var calls = 0
        override suspend fun getOccurrences(): Result<List<PublicOccurrence>> {
            calls++
            return Result.success(points)
        }
    }

    private class FakeLocation(private val coordinates: Coordinates?) : UserLocationRepository {
        override suspend fun getUserLocation() = coordinates
    }
}
