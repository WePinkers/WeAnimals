package com.example.weanimals.user.reporting.details.repository

import com.example.weanimals.user.reporting.details.domain.ReportDetails
import com.example.weanimals.user.reporting.report.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseDetailsRepository(
    private val reportRepository: ReportRepository
) : DetailsRepository {
    override fun observeReport(reportId: String): Flow<Result<ReportDetails>> =
        reportRepository.observeReport(reportId, includePhotoData = true).map { result ->
            result.map(::ReportDetails)
        }
}
