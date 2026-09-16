package com.example.weanimals.map.overview.domain

import com.example.weanimals.core.location.domain.Coordinates
import kotlin.math.floor

/** Public map locations are coarse grid cells, never the report's exact coordinates. */
data class PublicOccurrence(
    val id: String,
    val animalType: String,
    val urgency: String,
    val latitudeCell: Int,
    val longitudeCell: Int,
    val createdAtMillis: Long,
    val distanceKm: Double? = null
) {
    val coordinates: Coordinates
        get() = Coordinates((latitudeCell + 0.5) / 100.0, (longitudeCell + 0.5) / 100.0)

    companion object {
        fun cell(value: Double): Int = floor(value * 100.0).toInt()
    }
}
