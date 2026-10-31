package com.example.weanimals.user.reporting.triage.repository

import com.example.weanimals.user.reporting.report.domain.Report
import com.example.weanimals.user.reporting.report.domain.ReportDraft
import com.example.weanimals.user.reporting.triage.domain.TriageClassification
import com.example.weanimals.user.reporting.triage.domain.TriageQuestion

interface TriageRepository {
    suspend fun submit(
        draft: ReportDraft,
        answers: Map<TriageQuestion, Boolean>,
        classification: TriageClassification
    ): Result<Report>
}
