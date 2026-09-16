package com.example.weanimals.profile.reports.interactor

import com.example.weanimals.reporting.report.repository.ReportRepository

class ObserveMyReportsInteractor(private val repository: ReportRepository) {
    operator fun invoke() = repository.observeCurrentUserReports()
}
