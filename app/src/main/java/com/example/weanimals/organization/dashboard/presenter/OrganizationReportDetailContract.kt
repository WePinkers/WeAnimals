package com.example.weanimals.organization.dashboard.presenter

import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportStatus

interface OrganizationReportDetailContract {
    interface View {
        fun showLoading()
        fun showReport(report: OrganizationReport)
        fun showError(error: Throwable)
        fun showStatusError(error: Throwable)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun load()
        fun updateStatus(status: OrganizationReportStatus)
    }
}
