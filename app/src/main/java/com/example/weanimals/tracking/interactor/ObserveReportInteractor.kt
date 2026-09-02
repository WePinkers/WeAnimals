package com.example.weanimals.tracking.interactor

import com.example.weanimals.tracking.repository.TrackingRepository

class ObserveReportInteractor(
    private val repository: TrackingRepository
) {
    operator fun invoke(reportId: String) = repository.observeReport(reportId)
}
