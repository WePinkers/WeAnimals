package com.example.weanimals.organization.dashboard.repository

import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.math.max

/**
 * Fonte institucional das denúncias criadas por todos os cidadãos.
 *
 * O listener permanece ativo enquanto a tela estiver aberta. Assim, uma nova
 * denúncia ou uma mudança de status chega à tela da ONG sem precisar atualizar.
 */
class FirebaseOrganizationReportsRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : OrganizationReportsRepository {

    private val cachedReports = MutableStateFlow<List<OrganizationReport>>(emptyList())

    override fun observeReports(): Flow<List<OrganizationReport>> = callbackFlow {
        if (auth.currentUser == null) {
            close(IllegalStateException("A conta institucional não está autenticada."))
            return@callbackFlow
        }

        val registration = firestore.collection(REPORTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> {
                        val reports = ArrayList<OrganizationReport>()
                        snapshot.documents.forEach { document ->
                            toOrganizationReport(document)?.let(reports::add)
                        }
                        reports.sortWith(
                            Comparator { first: OrganizationReport, second: OrganizationReport ->
                                second.createdAtMillis.compareTo(first.createdAtMillis)
                            }
                        )
                        val sortedReports = reports.toList()
                        cachedReports.value = sortedReports
                        trySend(sortedReports)
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    override fun observeReport(reportId: String): Flow<OrganizationReport?> = callbackFlow {
        if (auth.currentUser == null) {
            close(IllegalStateException("A conta institucional não está autenticada."))
            return@callbackFlow
        }

        cachedReports.value
            .firstOrNull { report -> report.id == reportId }
            ?.let(::trySend)

        val registration = firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot == null || !snapshot.exists() -> trySend(null)
                    else -> trySend(toOrganizationReport(snapshot))
                }
            }

        awaitClose { registration.remove() }
    }

    override suspend fun updateStatus(
        reportId: String,
        status: OrganizationReportStatus
    ): Result<Unit> = runCatching {
        check(auth.currentUser != null) { "A conta institucional não está autenticada." }
        firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .update(
                mapOf(
                    FIELD_STATUS to status.toCitizenStatus(),
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    private fun toOrganizationReport(document: DocumentSnapshot): OrganizationReport? {
        val protocolNumber = document.getLong(FIELD_PROTOCOL)?.toInt() ?: return null
        val createdAtMillis = document.timestampMillis(FIELD_CREATED_AT)
        val statusValue = document.getString(FIELD_STATUS).orEmpty()
        val status = statusValue.toOrganizationStatus()

        return OrganizationReport(
            id = document.id,
            protocolNumber = protocolNumber,
            animalType = document.getString(FIELD_ANIMAL_TYPE).orEmpty().ifBlank { ANIMAL_OTHER },
            urgency = document.getString(FIELD_URGENCY).orEmpty().ifBlank { URGENCY_MEDIUM },
            triageClassification = document.getString(FIELD_TRIAGE_CLASSIFICATION),
            address = document.getString(FIELD_ADDRESS).orEmpty().ifBlank { ADDRESS_NOT_INFORMED },
            addressDetail = document.getString(FIELD_ADDRESS_DETAIL).orEmpty(),
            status = status,
            statusDescription = statusDescription(statusValue, status),
            elapsedLabel = elapsedLabel(createdAtMillis),
            createdAtMillis = createdAtMillis,
            description = document.getString(FIELD_DESCRIPTION).orEmpty(),
            assignedAgent = document.getString(FIELD_ASSIGNED_AGENT)
                ?: document.getString(FIELD_ASSIGNED_TEAM_ID),
            latitude = document.getDouble(FIELD_LATITUDE) ?: DEFAULT_LATITUDE,
            longitude = document.getDouble(FIELD_LONGITUDE) ?: DEFAULT_LONGITUDE
        )
    }

    private fun DocumentSnapshot.timestampMillis(field: String): Long {
        return getTimestamp(field)?.toDate()?.time
            ?: (get(field) as? Date)?.time
            ?: getLong(field)
            ?: 0L
    }

    private fun String.toOrganizationStatus(): OrganizationReportStatus = when (this) {
        OrganizationReportStatus.TEAM_CALLED.storageValue,
        "new",
        "under_review" -> OrganizationReportStatus.TEAM_CALLED

        OrganizationReportStatus.TEAM_ON_WAY.storageValue,
        "in_progress" -> OrganizationReportStatus.TEAM_ON_WAY

        OrganizationReportStatus.RESCUE.storageValue,
        "rescued" -> OrganizationReportStatus.RESCUE

        OrganizationReportStatus.SHELTER.storageValue -> OrganizationReportStatus.SHELTER
        OrganizationReportStatus.RESOLVED.storageValue,
        "closed" -> OrganizationReportStatus.RESOLVED
        else -> OrganizationReportStatus.TEAM_CALLED
    }

    private fun OrganizationReportStatus.toCitizenStatus(): String = when (this) {
        OrganizationReportStatus.TEAM_CALLED -> "under_review"
        OrganizationReportStatus.TEAM_ON_WAY -> "in_progress"
        OrganizationReportStatus.RESCUE,
        OrganizationReportStatus.SHELTER -> "rescued"
        OrganizationReportStatus.RESOLVED -> "closed"
    }

    private fun statusDescription(
        rawStatus: String,
        status: OrganizationReportStatus
    ): String = when (rawStatus) {
        "new", "under_review", OrganizationReportStatus.TEAM_CALLED.storageValue ->
            "Equipe acionada — aguardando chegada"

        "in_progress", OrganizationReportStatus.TEAM_ON_WAY.storageValue ->
            "Equipe a caminho"

        "rescued", OrganizationReportStatus.RESCUE.storageValue ->
            "Resgate em andamento"

        OrganizationReportStatus.SHELTER.storageValue -> "Encaminhado a abrigo"
        "closed", OrganizationReportStatus.RESOLVED.storageValue -> "Denúncia resolvida"
        else -> statusDescription(status)
    }

    private fun statusDescription(status: OrganizationReportStatus): String = when (status) {
        OrganizationReportStatus.TEAM_CALLED -> "Equipe acionada — aguardando chegada"
        OrganizationReportStatus.TEAM_ON_WAY -> "Equipe a caminho"
        OrganizationReportStatus.RESCUE -> "Resgate em andamento"
        OrganizationReportStatus.SHELTER -> "Encaminhado a abrigo"
        OrganizationReportStatus.RESOLVED -> "Denúncia resolvida"
    }

    private fun elapsedLabel(createdAtMillis: Long): String {
        if (createdAtMillis <= 0L) return "agora"
        val elapsedMillis = max(0L, System.currentTimeMillis() - createdAtMillis)
        val minutes = elapsedMillis / MILLIS_PER_MINUTE
        return when {
            minutes < 1L -> "agora"
            minutes < 60L -> "há ${minutes} min"
            minutes < 24L * 60L -> "há ${minutes / 60L} h"
            else -> "há ${minutes / (24L * 60L)} d"
        }
    }

    private companion object {
        const val REPORTS_COLLECTION = "reports"
        const val FIELD_PROTOCOL = "protocolNumber"
        const val FIELD_ANIMAL_TYPE = "animalType"
        const val FIELD_URGENCY = "urgency"
        const val FIELD_TRIAGE_CLASSIFICATION = "triageClassification"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_ADDRESS = "address"
        const val FIELD_ADDRESS_DETAIL = "addressDetail"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_ASSIGNED_AGENT = "assignedAgent"
        const val FIELD_ASSIGNED_TEAM_ID = "assignedTeamId"
        const val ANIMAL_OTHER = "other"
        const val URGENCY_MEDIUM = "medium"
        const val ADDRESS_NOT_INFORMED = "Localização não informada"
        const val DEFAULT_LATITUDE = -23.5505
        const val DEFAULT_LONGITUDE = -46.6333
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
