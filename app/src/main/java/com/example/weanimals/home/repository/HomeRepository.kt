package com.example.weanimals.home.repository

import com.example.weanimals.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeUserReports(): Flow<Result<List<Report>>>

    suspend fun markReportAsViewed(reportId: String): Result<Unit>
}
