package com.example.weanimals.report.presenter

import com.example.weanimals.locationsearch.domain.LocationDetails
import com.example.weanimals.report.domain.ReportDraft

interface ReportContract {
    interface View {
        fun showAnimalTypeSelected(animalType: String)
        fun showUrgencySelected(urgency: String)
        fun showLocationLoading()
        fun showLocation(location: LocationDetails)
        fun showLocationPermissionRequired()
        fun showLocationUnavailable()
        fun setSubmitEnabled(enabled: Boolean)
        fun showDescriptionRequired()
        fun showLocationRequired()
        fun showTriage(draft: ReportDraft)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun selectAnimalType(animalType: String)
        fun selectUrgency(urgency: String)
        fun loadCurrentLocation()
        fun onLocationPermissionDenied()
        fun onLocationUnavailable()
        fun selectLocation(location: LocationDetails)
        fun continueToTriage(description: String, photoUri: String?)
    }
}
