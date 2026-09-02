package com.example.weanimals.locationsearch.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.weanimals.locationsearch.domain.LocationDetails
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class AndroidLocationRepository(context: Context) : LocationRepository {

    private val applicationContext = context.applicationContext
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

    override suspend fun getCurrentLocation(): Result<LocationDetails> = runCatching {
        ensureLocationPermission()
        val location = getBestAvailableLocation()
            ?: error("Current location is unavailable.")
        LocationDetails(
            latitude = location.latitude,
            longitude = location.longitude,
            address = reverseGeocode(location)
        )
    }

    override suspend fun searchLocations(query: String): Result<List<LocationDetails>> = runCatching {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty() || !Geocoder.isPresent()) return@runCatching emptyList()

        val geocoder = Geocoder(applicationContext, Locale.forLanguageTag("pt-BR"))
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    normalizedQuery,
                    MAX_SEARCH_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(normalizedQuery, MAX_SEARCH_RESULTS).orEmpty()
            }
        }

        addresses.mapNotNull(::toLocationDetails)
            .distinctBy { "${it.latitude}:${it.longitude}" }
    }

    private suspend fun getBestAvailableLocation(): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).await() ?: fusedLocationClient.lastLocation.await()
        } finally {
            cancellationTokenSource.cancel()
        }
    }

    private suspend fun reverseGeocode(location: Location): String {
        if (!Geocoder.isPresent()) return ""
        val geocoder = Geocoder(applicationContext, Locale.forLanguageTag("pt-BR"))
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
            }
        }
        return formatAddress(address)
    }

    private fun toLocationDetails(address: Address): LocationDetails? {
        val latitude = address.latitude
        val longitude = address.longitude
        val title = listOfNotNull(address.thoroughfare, address.subThoroughfare)
            .joinToString(", ")
            .ifBlank { address.getAddressLine(0).orEmpty() }
        if (title.isBlank()) return null

        val neighborhood = address.subLocality ?: address.subAdminArea
        val city = address.locality ?: address.adminArea
        val state = address.adminArea
        val secondary = listOfNotNull(
            neighborhood?.takeIf(String::isNotBlank),
            listOfNotNull(city?.takeIf(String::isNotBlank), state?.takeIf(String::isNotBlank))
                .distinct()
                .joinToString(", ")
                .takeIf(String::isNotBlank)
        ).joinToString(" — ")

        return LocationDetails(
            latitude = latitude,
            longitude = longitude,
            address = title,
            secondaryAddress = secondary
        )
    }

    private fun formatAddress(address: Address?): String {
        if (address == null) return ""
        val street = listOfNotNull(address.thoroughfare, address.subThoroughfare)
            .joinToString(", ")
        val neighborhood = address.subLocality ?: address.subAdminArea
        val city = address.locality ?: address.adminArea
        return when {
            street.isNotBlank() && !neighborhood.isNullOrBlank() -> "$street — $neighborhood"
            street.isNotBlank() && !city.isNullOrBlank() -> "$street — $city"
            street.isNotBlank() -> street
            !address.getAddressLine(0).isNullOrBlank() -> address.getAddressLine(0)
            else -> ""
        }
    }

    private fun ensureLocationPermission() {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        check(hasFinePermission || hasCoarsePermission) { "Location permission is required." }
    }

    private companion object {
        const val MAX_SEARCH_RESULTS = 5
    }
}
