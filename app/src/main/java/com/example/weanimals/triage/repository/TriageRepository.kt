package com.example.weanimals.triage.repository

import com.example.weanimals.report.domain.Report
import com.example.weanimals.report.domain.ReportDraft
import com.example.weanimals.triage.domain.TriageClassification
import com.example.weanimals.triage.domain.TriageQuestion

interface TriageRepository {
    suspend fun submit(
        draft: ReportDraft,
        answers: Map<TriageQuestion, Boolean>,
        classification: TriageClassification
    ): Result<Report>
}
