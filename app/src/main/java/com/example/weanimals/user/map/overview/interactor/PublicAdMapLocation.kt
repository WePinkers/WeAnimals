package com.example.weanimals.user.map.overview.interactor

import android.content.Context
import android.location.Geocoder
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.round

/** Public map projection only; never changes the original address or coordinates. */
class PublicAdMapLocation(context: Context) {
    private val appContext = context.applicationContext
    private val cache = mutableMapOf<String, Pair<Double, Double>>()

    suspend fun resolve(ad: FoundPetAd): Pair<Double, Double>? {
        publicCoordinates(ad.latitude, ad.longitude)?.let { return it }
        // Resolve only a named area, never a residential number or complement.
        val neighborhood = ad.neighborhood?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val city = ad.city?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val query = listOfNotNull(neighborhood, city, ad.state?.takeIf { it.isNotBlank() }).joinToString(", ")
        cache[query]?.let { return it }
        val result = withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            try {
                @Suppress("DEPRECATION")
                val address = Geocoder(appContext, Locale.forLanguageTag("pt-BR"))
                    .getFromLocationName(query, 1)?.firstOrNull()
                if (address != null && address.hasLatitude() && address.hasLongitude()) {
                    publicCoordinates(address.latitude, address.longitude)
                } else null
            } catch (_: java.io.IOException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        if (result != null) cache[query] = result
        return result
    }

    companion object {
        /** Approximate ~100 m grid, so a public marker does not identify a house. */
        fun publicCoordinates(latitude: Double?, longitude: Double?): Pair<Double, Double>? {
            if (latitude == null || longitude == null ||
                !latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0 ||
                (latitude == 0.0 && longitude == 0.0)) return null
            return round(latitude * 1000) / 1000 to round(longitude * 1000) / 1000
        }

        fun isActive(ad: FoundPetAd, now: Long = System.currentTimeMillis()): Boolean =
            !ad.isEnded && !ad.isReunited &&
                now - ad.publicationTimestamp < 7L * 24 * 60 * 60 * 1000 &&
                (ad.category.equals("PERDIDO", true) || ad.category.equals("ENCONTRADO", true)) &&
                !ad.currentSituation.equals("Encerrado", true) &&
                !ad.currentSituation.equals("Reencontrado", true) &&
                !ad.currentSituation.equals("Entregue ao tutor", true)
    }
}
