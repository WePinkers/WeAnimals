package com.example.weanimals.reporting.tracking.repository

import com.example.weanimals.reporting.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    fun observeReport(reportId: String): Flow<Result<Report>>
}
