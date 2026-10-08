package com.example.weanimals.organization.dashboard.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.organization.dashboard.domain.OrganizationReportStatus
import com.example.weanimals.organization.dashboard.repository.OrganizationReportsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class OrganizationReportDetailPresenter(
    private val reportId: String,
    private val repository: OrganizationReportsRepository
) : BasePresenter<OrganizationReportDetailContract.View>(),
    OrganizationReportDetailContract.Presenter {

    private var reportJob: Job? = null
    private var updateJob: Job? = null

    override fun attachView(view: OrganizationReportDetailContract.View) {
        super.attachView(view)
    }

    override fun load() {
        if (reportJob?.isActive == true) return
        withView { it.showLoading() }
        reportJob = presenterScope.launch {
            var hasRenderedReport = false
            repository.observeReport(reportId)
                .catch { error ->
                    if (!hasRenderedReport) withView { it.showError(error) }
                }
                .collect { report ->
                    if (report == null) {
                        if (!hasRenderedReport) {
                            withView { it.showError(NoSuchElementException("Denúncia não encontrada.")) }
                        }
                    } else {
                        hasRenderedReport = true
                        withView { it.showReport(report) }
                    }
                }
        }
    }

    override fun updateStatus(status: OrganizationReportStatus) {
        if (updateJob?.isActive == true) return
        updateJob = presenterScope.launch {
            repository.updateStatus(reportId, status)
                .onFailure { error -> withView { it.showStatusError(error) } }
        }
    }
}
