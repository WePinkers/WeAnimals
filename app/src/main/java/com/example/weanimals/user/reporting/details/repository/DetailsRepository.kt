package com.example.weanimals.user.reporting.details.repository

import com.example.weanimals.user.reporting.details.domain.ReportDetails
import kotlinx.coroutines.flow.Flow

interface DetailsRepository {
    fun observeReport(reportId: String): Flow<Result<ReportDetails>>
}
