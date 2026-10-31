package com.example.weanimals.user.reporting.tracking.interactor

import com.example.weanimals.user.reporting.tracking.repository.TrackingRepository

class ObserveReportInteractor(
    private val repository: TrackingRepository
) {
    operator fun invoke(reportId: String) = repository.observeReport(reportId)
}
