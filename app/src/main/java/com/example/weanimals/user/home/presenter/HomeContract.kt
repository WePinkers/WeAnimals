package com.example.weanimals.user.home.presenter

import com.example.weanimals.user.reporting.report.domain.Report

interface HomeContract {
    interface View {
        fun showGreeting(name: String?)
        fun showLoading()
        fun showReports(reports: List<Report>)
        fun showReportsError(error: Throwable)
        fun openReport(report: Report)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun loadReports()
        fun openReport(report: Report)
    }
}
