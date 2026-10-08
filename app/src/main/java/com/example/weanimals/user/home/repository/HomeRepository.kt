package com.example.weanimals.user.home.repository

import com.example.weanimals.user.reporting.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeUserReports(): Flow<Result<List<Report>>>

    suspend fun markReportAsViewed(reportId: String): Result<Unit>
}
