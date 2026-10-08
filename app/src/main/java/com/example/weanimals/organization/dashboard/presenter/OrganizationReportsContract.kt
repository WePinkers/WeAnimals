package com.example.weanimals.organization.dashboard.presenter

import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportFilter

interface OrganizationReportsContract {
    interface View {
        fun showLoading()
        fun showReports(
            reports: List<OrganizationReport>,
            filter: OrganizationReportFilter
        )
        fun showError(error: Throwable)
        fun openReport(report: OrganizationReport)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun load()
        fun selectFilter(filter: OrganizationReportFilter)
        fun openReport(report: OrganizationReport)
    }
}
