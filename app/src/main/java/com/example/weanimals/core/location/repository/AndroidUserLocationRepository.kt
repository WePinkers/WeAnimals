package com.example.weanimals.core.location.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.weanimals.core.location.domain.Coordinates
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class AndroidUserLocationRepository(context: Context) : UserLocationRepository {
    private val applicationContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(applicationContext)

    override suspend fun getUserLocation(): Coordinates? {
        val permissionGranted = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).any {
            ContextCompat.checkSelfPermission(applicationContext, it) ==
                PackageManager.PERMISSION_GRANTED
        }
        if (!permissionGranted) return null

        val cancellation = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
                val location = client.lastLocation.await() ?: client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellation.token
                ).await()
                Coordinates.fromOrNull(location?.latitude, location?.longitude)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            cancellation.cancel()
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 8_000L
    }
}
