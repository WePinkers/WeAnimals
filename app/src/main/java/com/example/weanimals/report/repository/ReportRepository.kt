package com.example.weanimals.report.repository

import com.example.weanimals.report.domain.NewReport
import com.example.weanimals.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun observeCurrentUserReports(): Flow<Result<List<Report>>>

    fun observeReport(reportId: String): Flow<Result<Report>>

    suspend fun markReportAsViewed(reportId: String): Result<Unit>

    suspend fun submitReport(report: NewReport): Result<Report>
}
