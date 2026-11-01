package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

interface MapAddressGeocoder {
    fun isAvailable(): Boolean
    fun isOnline(): Boolean
    suspend fun find(query: String): Coordinates?
}

sealed interface MapAddressSearchResult {
    data class Found(val coordinates: Coordinates) : MapAddressSearchResult
    data object EmptyQuery : MapAddressSearchResult
    data object NotFound : MapAddressSearchResult
    data object Offline : MapAddressSearchResult
    data object Unavailable : MapAddressSearchResult
    data object Failed : MapAddressSearchResult
}

class SearchMapAddress(
    private val geocoder: MapAddressGeocoder,
    private val timeoutMillis: Long = 12_000
) {
    suspend operator fun invoke(query: String): MapAddressSearchResult {
        val text = query.trim()
        if (text.isEmpty()) return MapAddressSearchResult.EmptyQuery
        return try {
            when {
                !geocoder.isAvailable() -> MapAddressSearchResult.Unavailable
                !geocoder.isOnline() -> MapAddressSearchResult.Offline
                else -> withTimeout(timeoutMillis) { geocoder.find(text) }
                    ?.let { MapAddressSearchResult.Found(it) } ?: MapAddressSearchResult.NotFound
            }
        } catch (_: TimeoutCancellationException) {
            MapAddressSearchResult.Failed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            if (geocoder.isOnline()) MapAddressSearchResult.Failed else MapAddressSearchResult.Offline
        } catch (_: Exception) {
            MapAddressSearchResult.Failed
        }
    }
}
