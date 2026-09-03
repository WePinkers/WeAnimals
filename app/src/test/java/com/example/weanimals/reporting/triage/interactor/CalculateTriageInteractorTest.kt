package com.example.weanimals.reporting.triage.interactor

import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.triage.domain.TriageClassification
import com.example.weanimals.reporting.triage.domain.TriageQuestion
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateTriageInteractorTest {

    private val calculate = CalculateTriageInteractor()

    @Test
    fun `immediate danger plus high urgency is very urgent`() {
        val result = calculate(
            initialUrgency = Report.URGENCY_HIGH,
            answers = answers(immediateDanger = true)
        )

        assertEquals(TriageClassification.VERY_URGENT, result)
    }

    @Test
    fun `active abuse without immediate danger is urgent`() {
        val result = calculate(
            initialUrgency = Report.URGENCY_MEDIUM,
            answers = answers(activeAbuse = true)
        )

        assertEquals(TriageClassification.URGENT, result)
    }

    @Test
    fun `high urgency alone is priority`() {
        val result = calculate(
            initialUrgency = Report.URGENCY_HIGH,
            answers = answers()
        )

        assertEquals(TriageClassification.PRIORITY, result)
    }

    @Test
    fun `low urgency in a street situation is low urgency`() {
        val result = calculate(
            initialUrgency = Report.URGENCY_LOW,
            answers = answers(streetSituation = true)
        )

        assertEquals(TriageClassification.LOW, result)
    }

    private fun answers(
        immediateDanger: Boolean = false,
        activeAbuse: Boolean = false,
        injuredOrImmobilized: Boolean = false,
        streetSituation: Boolean = false
    ) = mapOf(
        TriageQuestion.IMMEDIATE_DANGER to immediateDanger,
        TriageQuestion.ACTIVE_ABUSE to activeAbuse,
        TriageQuestion.INJURED_OR_IMMOBILIZED to injuredOrImmobilized,
        TriageQuestion.STREET_SITUATION to streetSituation
    )
}
