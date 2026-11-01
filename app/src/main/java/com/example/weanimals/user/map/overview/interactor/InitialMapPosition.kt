package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates

data class InitialMapPosition(val latitude: Double, val longitude: Double, val zoom: Double)

object InitialMapPositionPolicy {
    fun valid(location: Coordinates?): Boolean = location != null &&
        location.latitude.isFinite() && location.longitude.isFinite() &&
        location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
        !(location.latitude == 0.0 && location.longitude == 0.0)

    fun choose(user: Coordinates?, records: List<Coordinates>): InitialMapPosition {
        if (valid(user)) return InitialMapPosition(user!!.latitude, user.longitude, 15.0)
        val record = records.firstOrNull { valid(it) }
        if (record != null) return InitialMapPosition(record.latitude, record.longitude, 13.0)
        // Country overview, not an invented animal/user location.
        return InitialMapPosition(-14.235, -51.9253, 3.5)
    }
}
