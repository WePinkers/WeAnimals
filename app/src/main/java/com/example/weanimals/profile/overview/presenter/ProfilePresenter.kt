package com.example.weanimals.profile.overview.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.profile.overview.interactor.GetProfileIdentityInteractor
import com.example.weanimals.reporting.report.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfilePresenter(
    private val getIdentity: GetProfileIdentityInteractor,
    private val reports: ReportRepository
) : BasePresenter<ProfileContract.View>(), ProfileContract.Presenter {
    private var reportsJob: Job? = null

    override fun start() {
        presenterScope.launch {
            getIdentity().onSuccess { identity -> withView { it.showIdentity(identity.displayName) } }
                .onFailure { withView { it.showIdentity(null) } }
        }
        reportsJob?.cancel()
        reportsJob = presenterScope.launch {
            reports.observeCurrentUserReports().collect { result ->
                result.onSuccess { list -> withView { it.showReportCount(list.size) } }
                    .onFailure { withView(ProfileContract.View::showReportsError) }
            }
        }
    }

    override fun detachView() {
        reportsJob?.cancel()
        super.detachView()
    }
}
