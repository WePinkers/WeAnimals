package com.example.weanimals.home.repository

import com.example.weanimals.report.repository.ReportRepository

/** Home's read adapter. The Firebase details remain hidden behind the report repository. */
class FirebaseHomeRepository(
    private val reportRepository: ReportRepository
) : HomeRepository {
    override fun observeUserReports() = reportRepository.observeCurrentUserReports()

    override suspend fun markReportAsViewed(reportId: String) =
        reportRepository.markReportAsViewed(reportId)
}
