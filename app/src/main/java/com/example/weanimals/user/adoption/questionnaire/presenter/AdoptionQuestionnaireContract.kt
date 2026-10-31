package com.example.weanimals.user.adoption.questionnaire.presenter

import com.example.weanimals.user.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.user.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.user.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.user.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.user.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.user.adoption.questionnaire.domain.RoutineOption

interface AdoptionQuestionnaireContract {
    interface View {
        fun showAnswers(answers: AdoptionQuestionnaireAnswers)
        fun showIncompleteQuestionnaire()
        fun showSaving()
        fun hideSaving()
        fun showSaveError(error: Throwable)
        fun openCompatibleProfile(animalId: String)
        fun closeScreen()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onRoutineSelected(option: RoutineOption)
        fun onAvailableSpaceSelected(option: AvailableSpaceOption)
        fun onPetExperienceSelected(option: PetExperienceOption)
        fun onDailyTimeSelected(option: DailyTimeOption)
        fun onHouseholdSelected(option: HouseholdOption)
        fun onSubmitClicked()
        fun onBackClicked()
    }
}
