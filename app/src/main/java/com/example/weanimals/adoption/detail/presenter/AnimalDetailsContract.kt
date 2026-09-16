package com.example.weanimals.adoption.detail.presenter

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.domain.AnimalShareDocument

interface AnimalDetailsContract {
    interface View {
        fun showLoading()
        fun showAnimal(details: AnimalDetails)
        fun showDistance(distanceKm: Double?, shelterAddress: String)
        fun showError(error: Throwable)
        fun showFavorite(favorite: Boolean)
        fun showShareLoading(loading: Boolean)
        fun shareAnimalDocument(document: AnimalShareDocument)
        fun showShareError(error: Throwable)
        fun requestUserLocation()
        fun showAdoptionProfileLoading(loading: Boolean)
        fun showAdoptionProfileError(error: Throwable)
        fun openAdoptionQuestionnaire(animalId: String)
        fun openAdoptionConfirmation(animalId: String)
        fun openShelterChat(details: AnimalDetails)
        fun closeScreen()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onRetryClicked()
        fun onBackClicked()
        fun onShareClicked()
        fun onFavoriteClicked()
        fun onConversationClicked()
        fun onAdoptClicked()
        fun onLocationAccessResult(available: Boolean)
    }
}
