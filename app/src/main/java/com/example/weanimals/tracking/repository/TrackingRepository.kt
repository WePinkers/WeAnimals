package com.example.weanimals.tracking.repository

import com.example.weanimals.report.domain.Report
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    fun observeReport(reportId: String): Flow<Result<Report>>
}
