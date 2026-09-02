package com.example.weanimals.home.interactor

import com.example.weanimals.home.repository.HomeRepository

class MarkReportAsViewedInteractor(
    private val repository: HomeRepository
) {
    suspend operator fun invoke(reportId: String): Result<Unit> =
        repository.markReportAsViewed(reportId)
}
