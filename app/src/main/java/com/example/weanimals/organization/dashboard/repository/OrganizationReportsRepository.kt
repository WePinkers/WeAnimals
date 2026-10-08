package com.example.weanimals.organization.dashboard.repository

import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportStatus
import kotlinx.coroutines.flow.Flow

interface OrganizationReportsRepository {
    fun observeReports(): Flow<List<OrganizationReport>>

    fun observeReport(reportId: String): Flow<OrganizationReport?>

    suspend fun updateStatus(
        reportId: String,
        status: OrganizationReportStatus
    ): Result<Unit>
}
