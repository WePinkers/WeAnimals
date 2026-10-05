package com.example.weanimals.community.create.presenter

import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails

interface CreateCommunityPostContract {
    interface View {
        fun showCategorySelected(category: CommunityCategory)
        fun showLocationLoading()
        fun showLocation(location: LocationDetails)
        fun showLocationPermissionRequired()
        fun showLocationUnavailable()
        fun showDescriptionRequired()
        fun showLocationRequired()
        fun showSubmitting(submitting: Boolean)
        fun showPublishError(error: Throwable)
        fun showPublished()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun selectCategory(category: CommunityCategory)
        fun loadCurrentLocation()
        fun selectLocation(location: LocationDetails)
        fun onLocationPermissionDenied()
        fun onLocationUnavailable()
        fun publish(body: String, photoUri: String?)
    }
}
