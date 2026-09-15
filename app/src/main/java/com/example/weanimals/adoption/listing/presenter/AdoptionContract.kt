package com.example.weanimals.adoption.listing.presenter

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.SpeciesFilter

interface AdoptionContract {
    interface View {
        fun showAnimals(animals: List<Animal>)
        fun appendAnimals(animals: List<Animal>)
        fun showFilterSelected(filter: SpeciesFilter)
        fun showEmptyState(filter: SpeciesFilter)
        fun showLoading(nextPage: Boolean)
        fun hideLoading()
        fun showError(error: Throwable, hasAnimals: Boolean)
        fun showLocationRequired()
        fun requestLocationPermission()
        fun openAnimalDetails(animalId: String)
        fun showRecommendationsAvailable(available: Boolean)
        fun openCompatibleProfile()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start(initialFilter: SpeciesFilter = SpeciesFilter.ALL)
        fun onFilterSelected(filter: SpeciesFilter)
        fun onScrolledToEnd()
        fun onRetryClicked()
        fun onLocationPermissionResult(granted: Boolean)
        fun onPetClicked(animalId: String)
        fun onRecommendationsClicked()
    }
}
