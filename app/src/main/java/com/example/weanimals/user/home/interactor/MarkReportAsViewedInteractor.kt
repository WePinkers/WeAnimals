package com.example.weanimals.user.home.interactor

import com.example.weanimals.user.home.repository.HomeRepository

class MarkReportAsViewedInteractor(
    private val repository: HomeRepository
) {
    suspend operator fun invoke(reportId: String): Result<Unit> =
        repository.markReportAsViewed(reportId)
}
