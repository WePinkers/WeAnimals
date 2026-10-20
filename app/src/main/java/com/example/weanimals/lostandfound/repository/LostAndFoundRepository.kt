package com.example.weanimals.lostandfound.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.weanimals.lostandfound.model.FoundPetAd
import org.json.JSONArray
import org.json.JSONObject

data class IndicatorsSummary(
    val lostCount: Int,
    val foundCount: Int,
    val reunitedCount: Int
)

object LostAndFoundRepository {

    private const val PREFS_NAME = "lost_and_found_prefs"
    private const val KEY_ADS = "saved_ads"

    private val adsList = mutableListOf<FoundPetAd>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadFromPrefs(context)
    }

    fun saveAd(context: Context, ad: FoundPetAd) {
        init(context)
        adsList.add(0, ad)
        saveToPrefs(context)
    }

    fun getAllAds(context: Context): List<FoundPetAd> {
        init(context)
        return adsList.toList()
    }

    fun getAdById(context: Context, id: String): FoundPetAd? {
        init(context)
        return adsList.firstOrNull { it.id == id }
    }

    fun updateAdSituation(context: Context, id: String, newSituation: String) {
        init(context)
        val ad = adsList.firstOrNull { it.id == id } ?: return
        ad.currentSituation = newSituation

        if (newSituation.equals("Reencontrado", ignoreCase = true) ||
            newSituation.equals("Entregue ao tutor", ignoreCase = true)
        ) {
            ad.isReunited = true
            if (ad.reunitedTimestamp == null) {
                ad.reunitedTimestamp = System.currentTimeMillis()
            }
            ad.isEnded = true
        }

        saveToPrefs(context)
    }

    fun endAd(context: Context, id: String) {
        init(context)
        val ad = adsList.firstOrNull { it.id == id } ?: return
        ad.isEnded = true
        ad.currentSituation = "Encerrado"
        saveToPrefs(context)
    }

    fun getIndicatorsSummary(context: Context): IndicatorsSummary {
        init(context)
        val now = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        val windowStart = now - thirtyDaysMillis

        var lostCount = 0
        var foundCount = 0
        var reunitedCount = 0

        for (ad in adsList) {
            if (ad.isReunited) {
                val reunitedTime = ad.reunitedTimestamp ?: ad.publicationTimestamp
                if (reunitedTime >= windowStart) {
                    reunitedCount++
                }
            } else if (!ad.isEnded && ad.getRemainingDays() > 0 && ad.publicationTimestamp >= windowStart) {
                if (ad.category.equals("PERDIDO", ignoreCase = true)) {
                    lostCount++
                } else {
                    foundCount++
                }
            }
        }

        return IndicatorsSummary(lostCount, foundCount, reunitedCount)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveToPrefs(context: Context) {
        val array = JSONArray()
        for (ad in adsList) {
            val obj = JSONObject().apply {
                put("id", ad.id)
                put("ownerUserId", ad.ownerUserId)
                put("publisherName", ad.publisherName ?: "")
                put("category", ad.category)
                put("species", ad.species)
                put("otherType", ad.otherType ?: "")
                put("size", ad.size ?: "")
                put("ageApprox", ad.ageApprox ?: "")
                put("condition", ad.condition ?: "")
                put("photoUris", JSONArray(ad.photoUris))
                put("street", ad.street ?: "")
                put("number", ad.number ?: "")
                put("neighborhood", ad.neighborhood ?: "")
                put("city", ad.city ?: "")
                put("state", ad.state ?: "")
                put("complement", ad.complement ?: "")
                put("whenFound", ad.whenFound ?: "")
                put("whenDate", ad.whenDate ?: "")
                put("marks", ad.marks ?: "")
                put("calledNames", JSONArray(ad.calledNames))
                put("currentSituation", ad.currentSituation)
                put("publicationTimestamp", ad.publicationTimestamp)
                put("possibleTutorsCount", ad.possibleTutorsCount)
                put("isEnded", ad.isEnded)
                put("isReunited", ad.isReunited)
                put("reunitedTimestamp", ad.reunitedTimestamp ?: 0L)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_ADS, array.toString()).apply()
    }

    private fun loadFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_ADS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            adsList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val photoUrisArray = obj.optJSONArray("photoUris")
                val photoUrisList = mutableListOf<String>()
                if (photoUrisArray != null) {
                    for (j in 0 until photoUrisArray.length()) {
                        photoUrisList.add(photoUrisArray.getString(j))
                    }
                }

                val calledNamesArray = obj.optJSONArray("calledNames")
                val calledNamesList = mutableListOf<String>()
                if (calledNamesArray != null) {
                    for (j in 0 until calledNamesArray.length()) {
                        calledNamesList.add(calledNamesArray.getString(j))
                    }
                }

                val reunitedTs = obj.optLong("reunitedTimestamp", 0L)

                val ad = FoundPetAd(
                    id = obj.optString("id"),
                    ownerUserId = obj.optString("ownerUserId", "local_user"),
                    publisherName = obj.optString("publisherName").ifBlank { null },
                    category = obj.optString("category", "ENCONTRADO"),
                    species = obj.optString("species"),
                    otherType = obj.optString("otherType").ifBlank { null },
                    size = obj.optString("size").ifBlank { null },
                    ageApprox = obj.optString("ageApprox").ifBlank { null },
                    condition = obj.optString("condition").ifBlank { null },
                    photoUris = photoUrisList,
                    street = obj.optString("street").ifBlank { null },
                    number = obj.optString("number").ifBlank { null },
                    neighborhood = obj.optString("neighborhood").ifBlank { null },
                    city = obj.optString("city").ifBlank { null },
                    state = obj.optString("state").ifBlank { null },
                    complement = obj.optString("complement").ifBlank { null },
                    whenFound = obj.optString("whenFound").ifBlank { null },
                    whenDate = obj.optString("whenDate").ifBlank { null },
                    marks = obj.optString("marks").ifBlank { null },
                    calledNames = calledNamesList,
                    currentSituation = obj.optString("currentSituation", "Comigo"),
                    publicationTimestamp = obj.optLong("publicationTimestamp", System.currentTimeMillis()),
                    possibleTutorsCount = obj.optInt("possibleTutorsCount", 0),
                    isEnded = obj.optBoolean("isEnded", false),
                    isReunited = obj.optBoolean("isReunited", false),
                    reunitedTimestamp = if (reunitedTs > 0L) reunitedTs else null
                )
                adsList.add(ad)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
