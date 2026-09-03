package com.example.weanimals.reporting.locationsearch.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.interactor.SearchLocationsInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationSearchPresenter(
    private val searchLocationsInteractor: SearchLocationsInteractor
) : BasePresenter<LocationSearchContract.View>(), LocationSearchContract.Presenter {

    private var searchJob: Job? = null

    override fun search(query: String) {
        searchJob?.cancel()
        if (query.trim().isEmpty()) {
            withView { it.showSearchEmpty() }
            return
        }

        withView { it.showSearchLoading() }
        searchJob = presenterScope.launch {
            delay(300)
            searchLocationsInteractor(query).fold(
                onSuccess = { results ->
                    if (results.isEmpty()) withView { it.showSearchEmpty() }
                    else withView { it.showSearchResults(results) }
                },
                onFailure = { error -> withView { it.showSearchError(error) } }
            )
        }
    }

    override fun selectLocation(location: LocationDetails) {
        withView { it.showLocationSelected(location) }
    }
}
