package com.example.weanimals.home.presenter

import android.util.Log
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
        lastReports?.takeIf { it.isNotEmpty() }?.let {
            Log.d("DEBUG_DENUNCIAS", "HomePresenter attachView re-exibindo cache com ${it.size} itens")
            view.showReports(it)
        }
        lastError?.let { view.showReportsError(it) }
    }

    override fun loadReports() {
        loadProfileIdentity()
        lastReports?.takeIf { it.isNotEmpty() }?.let { reports ->
            Log.d("DEBUG_DENUNCIAS", "HomePresenter loadReports exibindo cache inicial com ${reports.size} itens")
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
                        Log.d("DEBUG_DENUNCIAS", "HomePresenter recebeu lista completa do repositório com ${reports.size} itens. Atualizando View...")
                        withView { it.showReports(reports) }
                    },
                    onFailure = { error ->
                        lastError = error
                        if (!lastReports.isNullOrEmpty()) {
                            Log.w("DEBUG_DENUNCIAS", "Erro no fluxo, mantendo ${lastReports?.size} itens exibidos anteriormente", error)
                            withView { it.showReports(lastReports!!) }
                        } else {
                            lastReports = null
                            Log.e("DEBUG_DENUNCIAS", "HomePresenter recebeu erro do repositório: ${error.message}", error)
                            withView { it.showReportsError(error) }
                        }
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
