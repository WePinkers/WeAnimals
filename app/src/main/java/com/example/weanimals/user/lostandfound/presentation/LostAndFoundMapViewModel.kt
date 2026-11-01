package com.example.weanimals.user.lostandfound.presentation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository

enum class LostFoundMapFilter {
    ALL, LOST, FOUND
}

class LostAndFoundMapViewModel : ViewModel() {
    private val _filter = MutableLiveData(LostFoundMapFilter.ALL)
    val filter: LiveData<LostFoundMapFilter> = _filter

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _ads = MutableLiveData<List<FoundPetAd>>(emptyList())
    val ads: LiveData<List<FoundPetAd>> = _ads

    private val _selectedAd = MutableLiveData<FoundPetAd?>()
    val selectedAd: LiveData<FoundPetAd?> = _selectedAd

    fun setFilter(newFilter: LostFoundMapFilter) {
        _filter.value = newFilter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAd(ad: FoundPetAd?) {
        _selectedAd.value = ad
    }

    fun loadAds(context: Context) {
        val allAds = LostAndFoundRepository.getAllAds(context)
        val currentFilter = _filter.value ?: LostFoundMapFilter.ALL
        val query = _searchQuery.value.orEmpty().trim().lowercase()

        val filtered = allAds
            .filter { !it.isEnded && !it.isReunited && it.getRemainingDays() > 0 }
            .filter { ad -> ad.latitude != null && ad.longitude != null }
            .filter { ad ->
                when (currentFilter) {
                    LostFoundMapFilter.ALL -> true
                    LostFoundMapFilter.LOST -> ad.category.equals("PERDIDO", ignoreCase = true)
                    LostFoundMapFilter.FOUND -> ad.category.equals("ENCONTRADO", ignoreCase = true)
                }
            }
            .filter { ad ->
                if (query.isBlank()) true
                else ad.getDisplayTitle().lowercase().contains(query) ||
                        ad.getDisplayLocation().lowercase().contains(query) ||
                        ad.species.lowercase().contains(query) ||
                        (ad.neighborhood?.lowercase()?.contains(query) == true) ||
                        (ad.city?.lowercase()?.contains(query) == true)
            }

        _ads.value = filtered
    }
}
