package com.example.weanimals.user.reporting.details.interactor

import com.example.weanimals.user.reporting.details.repository.DetailsRepository

class ObserveReportDetailsInteractor(
    private val repository: DetailsRepository
) {
    operator fun invoke(reportId: String) = repository.observeReport(reportId)
}
