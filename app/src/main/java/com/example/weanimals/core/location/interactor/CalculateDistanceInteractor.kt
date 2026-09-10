package com.example.weanimals.core.location.interactor

import com.example.weanimals.core.location.domain.Coordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CalculateDistanceInteractor {
    operator fun invoke(from: Coordinates, to: Coordinates): Double {
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val haversine = (
            sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
                cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
                sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
            ).coerceIn(0.0, 1.0)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0088
    }
}
