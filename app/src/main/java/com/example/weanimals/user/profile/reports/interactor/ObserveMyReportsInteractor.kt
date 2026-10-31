package com.example.weanimals.user.profile.reports.interactor

import com.example.weanimals.user.reporting.report.repository.ReportRepository

class ObserveMyReportsInteractor(private val repository: ReportRepository) {
    operator fun invoke() = repository.observeCurrentUserReports()
}
