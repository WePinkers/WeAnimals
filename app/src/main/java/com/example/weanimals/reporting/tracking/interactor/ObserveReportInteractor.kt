package com.example.weanimals.reporting.tracking.interactor

import com.example.weanimals.reporting.tracking.repository.TrackingRepository

class ObserveReportInteractor(
    private val repository: TrackingRepository
) {
    operator fun invoke(reportId: String) = repository.observeReport(reportId)
}
