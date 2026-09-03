package com.example.weanimals.reporting.details.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.reporting.details.domain.ReportDetails
import com.example.weanimals.reporting.details.interactor.ObserveReportDetailsInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DetailsPresenter(
    private val reportId: String,
    private val observeReportDetailsInteractor: ObserveReportDetailsInteractor
) : BasePresenter<DetailsContract.View>(), DetailsContract.Presenter {

    private var detailsJob: Job? = null
    private var lastDetails: ReportDetails? = null
    private var lastError: Throwable? = null

    override fun attachView(view: DetailsContract.View) {
        super.attachView(view)
        lastDetails?.let { view.showDetails(it) }
        lastError?.let { view.showDetailsError(it) }
    }

    override fun loadDetails() {
        if (detailsJob?.isActive == true) return
        withView { it.showLoading() }
        detailsJob = presenterScope.launch {
            observeReportDetailsInteractor(reportId).collect { result ->
                result.fold(
                    onSuccess = { details ->
                        lastDetails = details
                        lastError = null
                        withView { it.showDetails(details) }
                    },
                    onFailure = { error ->
                        lastDetails = null
                        lastError = error
                        withView { it.showDetailsError(error) }
                    }
                )
            }
        }
    }
}
