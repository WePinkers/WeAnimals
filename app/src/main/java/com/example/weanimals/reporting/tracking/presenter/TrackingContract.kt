package com.example.weanimals.reporting.tracking.presenter

import com.example.weanimals.reporting.report.domain.Report

interface TrackingContract {
    interface View {
        fun showLoading()
        fun showReport(report: Report)
        fun showReportError(error: Throwable)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun loadReport()
    }
}
