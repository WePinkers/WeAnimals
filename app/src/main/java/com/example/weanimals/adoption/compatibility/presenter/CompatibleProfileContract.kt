package com.example.weanimals.adoption.compatibility.presenter

import com.example.weanimals.adoption.compatibility.domain.AnimalRecommendation
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile

interface CompatibleProfileContract {
    interface View {
        fun showLoading()
        fun showProfile(profile: AdoptionProfile, recommendations: List<AnimalRecommendation>)
        fun showEmpty(profile: AdoptionProfile)
        fun showError(error: Throwable)
        fun openQuestionnaire(animalId: String)
        fun openAnimalDetails(animalId: String)
        fun closeScreen()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onRetryClicked()
        fun onAnimalClicked(animalId: String)
        fun onBackClicked()
    }
}
