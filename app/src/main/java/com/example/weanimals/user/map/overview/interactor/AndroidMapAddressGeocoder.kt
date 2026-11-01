package com.example.weanimals.user.map.overview.interactor

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.weanimals.core.location.domain.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Uses the device's Android geocoding provider; no API key or extra paid service. */
class AndroidMapAddressGeocoder(context: Context) : MapAddressGeocoder {
    private val appContext = context.applicationContext

    override fun isAvailable() = Geocoder.isPresent()

    override fun isOnline(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override suspend fun find(query: String): Coordinates? {
        val geocoder = Geocoder(appContext, Locale.forLanguageTag("pt-BR"))
        return if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(coordinates(addresses.firstOrNull()))
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resumeWithException(IOException("Geocoding unavailable"))
                    }
                })
            }
        } else {
            // Legacy Android API is blocking, so it must never execute on the UI thread.
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                coordinates(geocoder.getFromLocationName(query, 1)?.firstOrNull())
            }
        }
    }

    private fun coordinates(address: Address?): Coordinates? =
        if (address != null && address.hasLatitude() && address.hasLongitude())
            Coordinates.fromOrNull(address.latitude, address.longitude) else null
}
