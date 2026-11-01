package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.user.lostandfound.model.FoundPetAd
import org.junit.Assert.*
import org.junit.Test

class PublicAdMapLocationTest {
    private val now = 1_800_000_000_000L
    private fun ad() = FoundPetAd(id = "test", ownerUserId = "test-owner",
        species = "Gato", category = "PERDIDO", publicationTimestamp = now)

    @Test fun rejectsMissingInvalidAndLegacyPlaceholderCoordinates() {
        listOf(null to -46.0, -23.0 to null, Double.NaN to 1.0,
            91.0 to 1.0, 1.0 to 181.0, 0.0 to 0.0).forEach {
            assertNull(PublicAdMapLocation.publicCoordinates(it.first, it.second))
        }
    }

    @Test fun usesApproximateCoordinatesWithoutLosingEquatorOrPrimeMeridian() {
        assertEquals(-23.551 to -46.633,
            PublicAdMapLocation.publicCoordinates(-23.55081, -46.63329))
        assertEquals(0.0 to 10.0, PublicAdMapLocation.publicCoordinates(0.0, 10.0))
        assertEquals(10.0 to 0.0, PublicAdMapLocation.publicCoordinates(10.0, 0.0))
    }

    @Test fun excludesClosedExpiredReunitedAndUnrecognizedCategories() {
        val active = ad()
        assertTrue(PublicAdMapLocation.isActive(active, now))
        assertTrue(PublicAdMapLocation.isActive(active.copy(category = "ENCONTRADO"), now))
        listOf(active.copy(isEnded = true), active.copy(isReunited = true),
            active.copy(currentSituation = "Entregue ao tutor"),
            active.copy(currentSituation = "Reencontrado"),
            active.copy(currentSituation = "Encerrado"),
            active.copy(category = "DENUNCIA"),
            active.copy(publicationTimestamp = now - 7L * 24 * 60 * 60 * 1000)
        ).forEach { assertFalse(PublicAdMapLocation.isActive(it, now)) }
    }
}
