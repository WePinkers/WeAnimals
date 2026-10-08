package com.example.weanimals.organization.dashboard.domain

enum class OrganizationReportFilter {
    ACTIVE,
    RESOLVED,
    ALL
}

enum class OrganizationReportStatus(
    val storageValue: String
) {
    TEAM_CALLED("team_called"),
    TEAM_ON_WAY("team_on_way"),
    RESCUE("rescue"),
    SHELTER("shelter"),
    RESOLVED("resolved")
}

data class OrganizationReport(
    val id: String,
    val protocolNumber: Int,
    val animalType: String,
    val urgency: String,
    val triageClassification: String? = null,
    val address: String,
    val addressDetail: String,
    val status: OrganizationReportStatus,
    val statusDescription: String,
    val elapsedLabel: String,
    val createdAtMillis: Long = 0L,
    val description: String,
    val assignedAgent: String? = null,
    val latitude: Double = -23.5505,
    val longitude: Double = -46.6333
)

fun OrganizationReportFilter.accepts(report: OrganizationReport): Boolean = when (this) {
    OrganizationReportFilter.ACTIVE -> report.status != OrganizationReportStatus.RESOLVED
    OrganizationReportFilter.RESOLVED -> report.status == OrganizationReportStatus.RESOLVED
    OrganizationReportFilter.ALL -> true
}
