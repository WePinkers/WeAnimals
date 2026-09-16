package com.example.weanimals.map.overview.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.map.overview.interactor.GetNearbyOccurrencesInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapPresenter(private val getNearby: GetNearbyOccurrencesInteractor) :
    BasePresenter<MapContract.View>(), MapContract.Presenter {
    private var job: Job? = null

    override fun load() {
        job?.cancel()
        withView(MapContract.View::showLoading)
        job = presenterScope.launch {
            getNearby().onSuccess { result -> withView { it.showOccurrences(result) } }
                .onFailure { withView(MapContract.View::showError) }
        }
    }

    override fun detachView() {
        job?.cancel()
        super.detachView()
    }
}
