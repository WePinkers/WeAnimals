package com.example.weanimals.lostandfound.presenter

import com.example.weanimals.lostandfound.domain.AnimalSize
import com.example.weanimals.lostandfound.domain.AnimalSpecies
import com.example.weanimals.lostandfound.domain.PetMatch

data class LostAndFoundState(
    val step: Step = Step.FORM,
    val species: AnimalSpecies = AnimalSpecies.DOG,
    val size: AnimalSize = AnimalSize.MEDIUM,
    val selectedMatch: PetMatch? = null,
    val answers: List<String> = emptyList()
) {
    enum class Step {
        FORM,
        MATCHES,
        DETAILS,
        VERIFICATION,
        WAITING,
        REPORT_SENT
    }
}

interface LostAndFoundContract {
    interface View {
        fun render(state: LostAndFoundState)
    }

    interface Presenter {
        fun start()
        fun submitForm(species: AnimalSpecies, size: AnimalSize)
        fun openMatch(match: PetMatch)
        fun openVerification()
        fun submitVerification(answers: List<String>)
        fun submitReport()
        fun goBack()
    }
}
