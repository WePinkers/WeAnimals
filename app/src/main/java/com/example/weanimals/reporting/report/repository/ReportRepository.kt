package com.example.weanimals.reporting.report.repository

import com.example.weanimals.reporting.report.domain.NewReport
import com.example.weanimals.reporting.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun observeCurrentUserReports(): Flow<Result<List<Report>>>

    fun observeReport(reportId: String, includePhotoData: Boolean = false): Flow<Result<Report>>

    suspend fun markReportAsViewed(reportId: String): Result<Unit>

    suspend fun submitReport(report: NewReport): Result<Report>
}
