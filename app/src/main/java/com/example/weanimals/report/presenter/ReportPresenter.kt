package com.example.weanimals.report.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.locationsearch.domain.LocationDetails
import com.example.weanimals.locationsearch.interactor.GetCurrentLocationInteractor
import com.example.weanimals.report.domain.Report
import com.example.weanimals.report.interactor.DescriptionRequiredException
import com.example.weanimals.report.interactor.LocationRequiredException
import com.example.weanimals.report.interactor.PrepareReportDraftInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReportPresenter(
    private val getCurrentLocationInteractor: GetCurrentLocationInteractor,
    private val prepareReportDraftInteractor: PrepareReportDraftInteractor
) : BasePresenter<ReportContract.View>(), ReportContract.Presenter {

    private var selectedAnimalType = Report.ANIMAL_DOG
    private var selectedUrgency = Report.URGENCY_HIGH
    private var selectedLocation: LocationDetails? = null
    private var locationJob: Job? = null

    override fun attachView(view: ReportContract.View) {
        super.attachView(view)
        withView {
            it.showAnimalTypeSelected(selectedAnimalType)
            it.showUrgencySelected(selectedUrgency)
            it.setSubmitEnabled(selectedLocation != null)
            selectedLocation?.let(it::showLocation)
        }
    }

    override fun selectAnimalType(animalType: String) {
        if (animalType !in setOf(Report.ANIMAL_DOG, Report.ANIMAL_CAT, Report.ANIMAL_OTHER)) {
            return
        }
        selectedAnimalType = animalType
        withView { it.showAnimalTypeSelected(animalType) }
    }

    override fun selectUrgency(urgency: String) {
        if (urgency !in setOf(
                Report.URGENCY_LOW,
                Report.URGENCY_MEDIUM,
                Report.URGENCY_HIGH
            )
        ) {
            return
        }
        selectedUrgency = urgency
        withView { it.showUrgencySelected(urgency) }
    }

    override fun loadCurrentLocation() {
        if (locationJob?.isActive == true) return

        selectedLocation = null
        withView {
            it.showLocationLoading()
            it.setSubmitEnabled(false)
        }
        locationJob = presenterScope.launch {
            getCurrentLocationInteractor().fold(
                onSuccess = { location ->
                    selectedLocation = location
                    withView {
                        it.showLocation(location)
                        it.setSubmitEnabled(true)
                    }
                },
                onFailure = {
                    selectedLocation = null
                    withView {
                        it.showLocationUnavailable()
                        it.setSubmitEnabled(false)
                    }
                }
            )
        }
    }

    override fun onLocationPermissionDenied() {
        selectedLocation = null
        withView {
            it.showLocationPermissionRequired()
            it.setSubmitEnabled(false)
        }
    }

    override fun onLocationUnavailable() {
        selectedLocation = null
        withView {
            it.showLocationUnavailable()
            it.setSubmitEnabled(false)
        }
    }

    override fun selectLocation(location: LocationDetails) {
        selectedLocation = location
        withView {
            it.showLocation(location)
            it.setSubmitEnabled(true)
        }
    }

    override fun continueToTriage(description: String, photoUri: String?) {
        prepareReportDraftInteractor(
            animalType = selectedAnimalType,
            urgency = selectedUrgency,
            description = description,
            location = selectedLocation,
            photoUri = photoUri
        ).fold(
            onSuccess = { draft -> withView { it.showTriage(draft) } },
            onFailure = { error ->
                when (error) {
                    is DescriptionRequiredException -> withView { it.showDescriptionRequired() }
                    is LocationRequiredException -> withView { it.showLocationRequired() }
                    else -> withView { it.showLocationRequired() }
                }
            }
        )
    }
}
