package com.example.weanimals.community.create.presenter

import com.example.weanimals.community.create.domain.CommunityPostDraft
import com.example.weanimals.community.create.interactor.PublishCommunityPostInteractor
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.interactor.GetCurrentLocationInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CreateCommunityPostPresenter(
    private val getCurrentLocation: GetCurrentLocationInteractor,
    private val publishPost: PublishCommunityPostInteractor
) : BasePresenter<CreateCommunityPostContract.View>(), CreateCommunityPostContract.Presenter {
    private var category = CommunityCategory.FOUND
    private var location: LocationDetails? = null
    private var locationJob: Job? = null
    private var publishJob: Job? = null

    override fun attachView(view: CreateCommunityPostContract.View) {
        super.attachView(view)
        view.showCategorySelected(category)
        location?.let(view::showLocation)
    }

    override fun selectCategory(category: CommunityCategory) {
        if (category !in POST_CATEGORIES) return
        this.category = category
        withView { it.showCategorySelected(category) }
    }

    override fun loadCurrentLocation() {
        if (locationJob?.isActive == true) return
        withView { it.showLocationLoading() }
        locationJob = presenterScope.launch {
            getCurrentLocation().fold(
                onSuccess = { selectLocation(it) },
                onFailure = { onLocationUnavailable() }
            )
        }
    }

    override fun selectLocation(location: LocationDetails) {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()
            || location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) return
        locationJob?.cancel()
        this.location = location
        withView { it.showLocation(location) }
    }

    override fun onLocationPermissionDenied() {
        withView { it.showLocationPermissionRequired() }
    }

    override fun onLocationUnavailable() {
        withView { it.showLocationUnavailable() }
    }

    override fun publish(body: String, photoUri: String?) {
        if (publishJob?.isActive == true) return
        if (body.isBlank()) {
            withView { it.showDescriptionRequired() }
            return
        }
        val selectedLocation = location ?: run {
            withView { it.showLocationRequired() }
            return
        }
        withView { it.showSubmitting(true) }
        publishJob = presenterScope.launch {
            publishPost(CommunityPostDraft(category, body, selectedLocation, photoUri)).fold(
                onSuccess = { withView { it.showPublished() } },
                onFailure = { error ->
                    withView {
                        it.showSubmitting(false)
                        it.showPublishError(error)
                    }
                }
            )
        }
    }

    private companion object {
        val POST_CATEGORIES = setOf(
            CommunityCategory.FOUND,
            CommunityCategory.QUESTION,
            CommunityCategory.NOTICE,
            CommunityCategory.OTHER
        )
    }
}
