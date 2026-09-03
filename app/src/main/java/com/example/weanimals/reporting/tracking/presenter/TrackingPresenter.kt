package com.example.weanimals.reporting.tracking.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.tracking.interactor.ObserveReportInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TrackingPresenter(
    private val reportId: String,
    private val observeReportInteractor: ObserveReportInteractor
) : BasePresenter<TrackingContract.View>(), TrackingContract.Presenter {

    private var reportJob: Job? = null
    private var lastReport: Report? = null
    private var lastError: Throwable? = null

    override fun attachView(view: TrackingContract.View) {
        super.attachView(view)
        lastReport?.let { view.showReport(it) }
        lastError?.let { view.showReportError(it) }
    }

    override fun loadReport() {
        if (reportJob?.isActive == true) return
        withView { it.showLoading() }
        reportJob = presenterScope.launch {
            observeReportInteractor(reportId).collect { result ->
                result.fold(
                    onSuccess = { report ->
                        lastReport = report
                        lastError = null
                        withView { it.showReport(report) }
                    },
                    onFailure = { error ->
                        lastReport = null
                        lastError = error
                        withView { it.showReportError(error) }
                    }
                )
            }
        }
    }
}
