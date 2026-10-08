package com.example.weanimals.user.profile.reports.presenter

import com.example.weanimals.user.reporting.report.domain.Report

interface MyReportsContract {
    interface View {
        fun showReports(reports: List<Report>)
        fun showError()
    }
    interface Presenter { fun start() }
}
