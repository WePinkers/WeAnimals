package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates
import org.junit.Assert.*
import org.junit.Test

class InitialMapPositionTest {
    @Test fun prefersValidUserLocationOverAnnouncements() {
        assertEquals(InitialMapPosition(-23.55, -46.63, 15.0),
            InitialMapPositionPolicy.choose(Coordinates(-23.55, -46.63), listOf(Coordinates(10.0, 10.0))))
    }

    @Test fun fallsBackToARealRecordWhenGpsIsMissingOrInvalid() {
        val records = listOf(Coordinates(0.0, 0.0), Coordinates(-3.1, -60.0))
        assertEquals(InitialMapPosition(-3.1, -60.0, 13.0), InitialMapPositionPolicy.choose(null, records))
        assertEquals(InitialMapPosition(-3.1, -60.0, 13.0),
            InitialMapPositionPolicy.choose(Coordinates(0.0, 0.0), records))
    }

    @Test fun emptyMapStartsWithBrazilOverviewWithoutCreatingAPoint() {
        val fallback = InitialMapPositionPolicy.choose(null, emptyList())
        assertEquals(InitialMapPosition(-14.235, -51.9253, 3.5), fallback)
        assertFalse(InitialMapPositionPolicy.valid(Coordinates(0.0, 0.0)))
        assertTrue(InitialMapPositionPolicy.valid(Coordinates(0.0, -46.0)))
    }
}
