package com.example.weanimals.community.create.domain

import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicLocationLabelTest {
    @Test fun currentLocationKeepsNeighborhoodNotStreet() {
        val location = LocationDetails(-23.55, -46.63, "Rua Exemplo, 19 — Jardim Matarazzo")
        assertEquals("Jardim Matarazzo", location.publicLocationLabel())
    }

    @Test fun searchedLocationUsesSecondaryArea() {
        val location = LocationDetails(
            -23.55, -46.63, "Rua Exemplo, 19", "Vila Marlene — São Paulo, SP"
        )
        assertEquals("Vila Marlene", location.publicLocationLabel())
    }

    @Test fun neverPublishesBareStreetAddress() {
        val location = LocationDetails(-23.55, -46.63, "Rua Exemplo, 19")
        assertNull(location.publicLocationLabel())
    }
}
