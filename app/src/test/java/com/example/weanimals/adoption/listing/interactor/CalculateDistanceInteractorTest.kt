package com.example.weanimals.adoption.listing.interactor

import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import org.junit.Assert.*
import org.junit.Test

class CalculateDistanceInteractorTest {
    private val calculate = CalculateDistanceInteractor()

    @Test fun sameCoordinatesHaveZeroDistance() {
        assertEquals(0.0, calculate(Coordinates(0.0, 0.0), Coordinates(0.0, 0.0)), 0.000001)
    }

    @Test fun saoPauloToRioIsAbout360Kilometers() {
        val distance = calculate(Coordinates(-23.5505, -46.6333), Coordinates(-22.9068, -43.1729))
        assertEquals(360.75, distance, 1.0)
    }

    @Test fun crossesAntimeridianUsingShortestDistance() {
        assertEquals(222.39, calculate(Coordinates(0.0, 179.0), Coordinates(0.0, -179.0)), 0.1)
    }

    @Test fun antipodalPointsStayFinite() {
        assertEquals(20015.11, calculate(Coordinates(90.0, 0.0), Coordinates(-90.0, 0.0)), 0.1)
    }

    @Test fun invalidShelterCoordinatesAreNotTurnedIntoZero() {
        assertNull(Coordinates.fromOrNull(null, null))
        assertNull(Coordinates.fromOrNull(Double.NaN, 20.0))
        assertNull(Coordinates.fromOrNull(91.0, 20.0))
        assertNotNull(Coordinates.fromOrNull(0.0, 0.0))
    }
}
