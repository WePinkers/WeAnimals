package com.example.weanimals.user.reporting.locationsearch.presenter

import com.example.weanimals.user.reporting.locationsearch.domain.LocationDetails

interface LocationSearchContract {
    interface View {
        fun showSearchLoading()
        fun showSearchResults(results: List<LocationDetails>)
        fun showSearchEmpty()
        fun showSearchError(error: Throwable)
        fun showLocationSelected(location: LocationDetails)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun search(query: String)
        fun selectLocation(location: LocationDetails)
    }
}
