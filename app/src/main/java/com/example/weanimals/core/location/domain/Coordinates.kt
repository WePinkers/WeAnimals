package com.example.weanimals.core.location.domain

data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    companion object {
        fun fromOrNull(latitude: Double?, longitude: Double?): Coordinates? =
            if (
                latitude != null && longitude != null &&
                latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
            ) {
                Coordinates(latitude, longitude)
            } else {
                null
            }
    }
}

class UserLocationUnavailableException : IllegalStateException("User location is unavailable.")
