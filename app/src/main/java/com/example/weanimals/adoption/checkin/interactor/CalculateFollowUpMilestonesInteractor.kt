package com.example.weanimals.adoption.checkin.interactor

import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.FollowUpMilestone
import com.example.weanimals.adoption.checkin.domain.MilestoneState
import java.util.Calendar

class CalculateFollowUpMilestonesInteractor {
    operator fun invoke(
        followUp: AdoptionFollowUp,
        nowMillis: Long = System.currentTimeMillis()
    ): List<FollowUpMilestone> {
        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            clearTime()
        }.timeInMillis
        val dueDates = MILESTONES.associateWith { days ->
            Calendar.getInstance().apply {
                timeInMillis = followUp.adoptedAtMillis
                clearTime()
                add(Calendar.DAY_OF_YEAR, days)
            }.timeInMillis
        }
        val availableDays = MILESTONES.firstOrNull { days ->
            days !in followUp.checkIns && Calendar.getInstance().apply {
                timeInMillis = requireNotNull(dueDates[days])
                add(Calendar.DAY_OF_YEAR, -AVAILABILITY_DAYS_BEFORE_DUE)
            }.timeInMillis <= today
        }
        return MILESTONES.map { days ->
            val checkIn = followUp.checkIns[days]
            FollowUpMilestone(
                days = days,
                dueAtMillis = requireNotNull(dueDates[days]),
                state = when {
                    checkIn != null -> MilestoneState.COMPLETED
                    days == availableDays -> MilestoneState.AVAILABLE
                    else -> MilestoneState.SCHEDULED
                },
                checkIn = checkIn
            )
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    companion object {
        val MILESTONES = listOf(30, 90, 180)
        const val AVAILABILITY_DAYS_BEFORE_DUE = 14
    }
}
