package com.example.weanimals.profile.reports.presenter

import com.example.weanimals.reporting.report.domain.Report

interface MyReportsContract {
    interface View {
        fun showReports(reports: List<Report>)
        fun showError()
    }
    interface Presenter { fun start() }
}
