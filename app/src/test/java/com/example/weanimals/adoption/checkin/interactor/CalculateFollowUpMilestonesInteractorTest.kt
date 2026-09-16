package com.example.weanimals.adoption.checkin.interactor

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.AdoptionCheckIn
import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.MilestoneState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CalculateFollowUpMilestonesInteractorTest {
    private val calculate = CalculateFollowUpMilestonesInteractor()

    @Test fun datesAndStatesComeFromAdoptionAndSavedResponses() {
        val followUp = followUp(date(2026, Calendar.JUNE, 28), mapOf(
            30 to AdoptionCheckIn(30, AdaptationRating.EXCELLENT, "Tudo ótimo", 0L)
        ))

        val milestones = calculate(followUp, date(2026, Calendar.SEPTEMBER, 15))

        assertEquals(listOf(
            MilestoneState.COMPLETED, MilestoneState.AVAILABLE, MilestoneState.SCHEDULED
        ), milestones.map { it.state })
        assertEquals(date(2026, Calendar.SEPTEMBER, 26), milestones[1].dueAtMillis)
        assertEquals(date(2026, Calendar.DECEMBER, 25), milestones[2].dueAtMillis)
    }

    @Test fun nextMilestoneOpensTwoWeeksBeforeDueDate() {
        val followUp = followUp(date(2026, Calendar.JUNE, 28), mapOf(
            30 to AdoptionCheckIn(30, AdaptationRating.GOOD, "", 0L)
        ))
        assertEquals(MilestoneState.SCHEDULED,
            calculate(followUp, date(2026, Calendar.SEPTEMBER, 11))[1].state)
        assertEquals(MilestoneState.AVAILABLE,
            calculate(followUp, date(2026, Calendar.SEPTEMBER, 12))[1].state)
    }

    @Test fun onlyTheEarliestUnansweredMilestoneIsAvailable() {
        val followUp = followUp(date(2025, Calendar.JANUARY, 1))
        val milestones = calculate(followUp, date(2026, Calendar.SEPTEMBER, 15))
        assertEquals(listOf(
            MilestoneState.AVAILABLE, MilestoneState.SCHEDULED, MilestoneState.SCHEDULED
        ), milestones.map { it.state })
    }

    private fun followUp(
        adoptedAt: Long,
        checkIns: Map<Int, AdoptionCheckIn> = emptyMap()
    ) = AdoptionFollowUp("animal-1", "Canela", adoptedAt, null, null, checkIns)

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day)
    }.timeInMillis
}
