package com.example.weanimals.adoption.compatibility.presenter

import com.example.weanimals.adoption.compatibility.domain.AnimalRecommendation
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers

interface CompatibleProfileContract {
    interface View {
        fun showLoading()
        fun showProfile(profile: AdoptionProfile, recommendations: List<AnimalRecommendation>)
        fun showEmpty(profile: AdoptionProfile)
        fun showError(error: Throwable)
        fun openQuestionnaire(animalId: String)
        fun openQuestionnaireForEditing(animalId: String, answers: AdoptionQuestionnaireAnswers)
        fun openAnimalDetails(animalId: String)
        fun closeScreen()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onRetryClicked()
        fun onEditAnswersClicked()
        fun onAnswersUpdated()
        fun onAnimalClicked(animalId: String)
        fun onBackClicked()
    }
}
