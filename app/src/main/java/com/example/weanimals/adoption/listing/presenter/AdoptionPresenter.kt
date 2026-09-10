package com.example.weanimals.adoption.listing.presenter

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.example.weanimals.adoption.listing.interactor.GetAvailableAnimalsInteractor
import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.domain.UserLocationUnavailableException
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class AdoptionPresenter(
    private val getAvailableAnimals: GetAvailableAnimalsInteractor,
    private val getUserLocation: GetUserLocationInteractor
) : BasePresenter<AdoptionContract.View>(), AdoptionContract.Presenter {
    private var started = false
    private var filter = SpeciesFilter.ALL
    private var animals = emptyList<Animal>()
    private var nextPage: AnimalPageCursor? = null
    private var location: Coordinates? = null
    private var locationAttempted = false
    private var loading = false
    private var error: Throwable? = null
    private var loadJob: Job? = null

    override fun start(initialFilter: SpeciesFilter) {
        if (!started) {
            started = true
            reset(initialFilter)
        } else {
            render()
        }
    }

    override fun onFilterSelected(filter: SpeciesFilter) {
        if (this.filter != filter) reset(filter)
    }

    override fun onScrolledToEnd() {
        if (!loading && error == null && nextPage != null) load()
    }

    override fun onRetryClicked() {
        if (loading) return
        if (error is UserLocationUnavailableException) {
            withView { it.requestLocationPermission() }
        } else {
            load()
        }
    }

    override fun onLocationPermissionResult(granted: Boolean) {
        if (filter != SpeciesFilter.NEAREST) return
        if (granted) {
            locationAttempted = false
            reset(filter)
        } else {
            error = UserLocationUnavailableException()
            render()
        }
    }

    override fun onPetClicked(animalId: String) {
        animals.firstOrNull { it.id == animalId }?.let { animal ->
            withView { it.openAnimalDetails(animal.id) }
        }
    }

    private fun reset(filter: SpeciesFilter) {
        loadJob?.cancel()
        this.filter = filter
        animals = emptyList()
        nextPage = null
        error = null
        loading = false
        if (filter == SpeciesFilter.NEAREST) locationAttempted = false
        load()
    }

    private fun load() {
        if (loading) return
        loading = true
        error = null
        val requestFilter = filter
        withView {
            it.showFilterSelected(filter)
            it.showLoading(nextPage = animals.isNotEmpty())
        }
        loadJob = presenterScope.launch {
            if (!locationAttempted) {
                val userLocation = getUserLocation()
                currentCoroutineContext().ensureActive()
                location = userLocation
                locationAttempted = true
            }
            do {
                val result = getAvailableAnimals(requestFilter, nextPage, location)
                currentCoroutineContext().ensureActive()
                val page = result.getOrElse {
                    loading = false
                    error = it
                    render()
                    return@launch
                }
                val hadAnimals = animals.isNotEmpty()
                val knownIds = animals.mapTo(mutableSetOf()) { it.id }
                val additions = page.animals.filter { knownIds.add(it.id) }
                animals = animals + additions
                nextPage = page.nextPage
                // Invalid or duplicate-only server pages still advance the cursor.
                // Keep one cancellable job rather than starting nested loads.
                if (additions.isEmpty() && nextPage != null) continue
                loading = false
                withView {
                    it.hideLoading()
                    when {
                        animals.isEmpty() -> it.showEmptyState(filter)
                        !hadAnimals -> it.showAnimals(animals)
                        else -> it.appendAnimals(additions)
                    }
                }
                return@launch
            } while (nextPage != null)
        }
    }

    private fun render() = withView { view ->
        view.showFilterSelected(filter)
        view.hideLoading()
        if (animals.isNotEmpty()) view.showAnimals(animals)
        when {
            loading -> view.showLoading(animals.isNotEmpty())
            error is UserLocationUnavailableException -> view.showLocationRequired()
            error != null -> view.showError(requireNotNull(error), animals.isNotEmpty())
            animals.isEmpty() -> view.showEmptyState(filter)
        }
    }
}
