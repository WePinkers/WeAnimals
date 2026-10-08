package com.example.weanimals.organization.dashboard.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportFilter
import com.example.weanimals.organization.dashboard.domain.accepts
import com.example.weanimals.organization.dashboard.repository.OrganizationReportsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class OrganizationReportsPresenter(
    private val repository: OrganizationReportsRepository
) : BasePresenter<OrganizationReportsContract.View>(), OrganizationReportsContract.Presenter {

    private var reportsJob: Job? = null
    private var currentReports: List<OrganizationReport> = emptyList()
    private var currentFilter = OrganizationReportFilter.ACTIVE
    private var lastError: Throwable? = null

    override fun attachView(view: OrganizationReportsContract.View) {
        super.attachView(view)
        if (currentReports.isNotEmpty()) {
            render()
        } else {
            lastError?.let { view.showError(it) }
        }
    }

    override fun load() {
        reportsJob?.cancel()
        withView { it.showLoading() }
        reportsJob = presenterScope.launch {
            repository.observeReports()
                .catch { error ->
                    if (currentReports.isEmpty()) {
                        lastError = error
                        withView { it.showError(error) }
                    } else {
                        // A falha momentânea do listener não deve apagar os
                        // dados que já foram carregados na tela.
                        lastError = null
                        render()
                    }
                }
                .collect { reports ->
                    currentReports = reports
                    lastError = null
                    render()
                }
        }
    }

    override fun selectFilter(filter: OrganizationReportFilter) {
        currentFilter = filter
        render()
    }

    override fun openReport(report: OrganizationReport) {
        withView { it.openReport(report) }
    }

    private fun render() {
        withView { view ->
            view.showReports(
                reports = currentReports.filter(currentFilter::accepts),
                filter = currentFilter
            )
        }
    }
}
