package com.example.weanimals.user.map.overview.domain

import com.example.weanimals.core.location.domain.Coordinates
import kotlin.math.floor

/**
 * A report location shown on the map.
 *
 * The grid fields remain for backwards compatibility with records created by
 * older app versions. New records also contain the exact coordinates selected
 * for the report, which keeps the marker anchored to the real location.
 */
data class PublicOccurrence(
    val id: String,
    val animalType: String,
    val urgency: String,
    val latitudeCell: Int,
    val longitudeCell: Int,
    val createdAtMillis: Long,
    val distanceKm: Double? = null,
    val exactLatitude: Double? = null,
    val exactLongitude: Double? = null
) {
    val coordinates: Coordinates
        get() = Coordinates.fromOrNull(exactLatitude, exactLongitude)
            ?: Coordinates((latitudeCell + 0.5) / 100.0, (longitudeCell + 0.5) / 100.0)

    companion object {
        fun cell(value: Double): Int = floor(value * 100.0).toInt()
    }
}
