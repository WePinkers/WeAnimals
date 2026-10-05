package com.example.weanimals.home.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.home.interactor.MarkReportAsViewedInteractor
import com.example.weanimals.home.interactor.ObserveUserReportsInteractor
import com.example.weanimals.profile.overview.interactor.GetProfileIdentityInteractor
import com.example.weanimals.reporting.report.domain.Report
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomePresenter(
    private val observeUserReportsInteractor: ObserveUserReportsInteractor,
    private val markReportAsViewedInteractor: MarkReportAsViewedInteractor,
    private val getProfileIdentityInteractor: GetProfileIdentityInteractor
) : BasePresenter<HomeContract.View>(), HomeContract.Presenter {

    private var reportsJob: Job? = null
    private var lastReports: List<Report>? = null
    private var lastError: Throwable? = null
    private var profileName: String? = null

    override fun attachView(view: HomeContract.View) {
        super.attachView(view)
        view.showGreeting(profileName)
        lastReports?.let { view.showReports(it) }
        lastError?.let { view.showReportsError(it) }
    }

    override fun loadReports() {
        loadProfileIdentity()
        lastReports?.let { reports ->
            withView { it.showReports(reports) }
        }
        if (reportsJob?.isActive == true) return
        if (lastReports == null) {
            withView { it.showLoading() }
        }
        reportsJob = presenterScope.launch {
            observeUserReportsInteractor().collect { result ->
                result.fold(
                    onSuccess = { reports ->
                        lastReports = reports
                        lastError = null
                        withView { it.showReports(reports) }
                    },
                    onFailure = { error ->
                        lastReports = null
                        lastError = error
                        withView { it.showReportsError(error) }
                    }
                )
            }
        }
    }

    private fun loadProfileIdentity() {
        presenterScope.launch {
            getProfileIdentityInteractor().onSuccess { identity ->
                profileName = identity.displayName
                withView { it.showGreeting(profileName) }
            }
        }
    }

    override fun openReport(report: Report) {
        lastReports = lastReports?.map { currentReport ->
            if (currentReport.id == report.id) currentReport.copy(isViewed = true) else currentReport
        }
        lastReports?.let { reports -> withView { it.showReports(reports) } }
        withView { it.openReport(report) }

        if (report.status == com.example.weanimals.reporting.report.domain.ReportStatus.NEW
            && !report.isViewed
        ) {
            presenterScope.launch {
                markReportAsViewedInteractor(report.id)
            }
        }
    }
}
