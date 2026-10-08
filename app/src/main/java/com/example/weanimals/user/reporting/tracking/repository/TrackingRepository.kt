package com.example.weanimals.user.reporting.tracking.repository

import com.example.weanimals.user.reporting.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    fun observeReport(reportId: String): Flow<Result<Report>>
}
