package com.example.weanimals.reporting.details.interactor

import com.example.weanimals.reporting.details.repository.DetailsRepository

class ObserveReportDetailsInteractor(
    private val repository: DetailsRepository
) {
    operator fun invoke(reportId: String) = repository.observeReport(reportId)
}
