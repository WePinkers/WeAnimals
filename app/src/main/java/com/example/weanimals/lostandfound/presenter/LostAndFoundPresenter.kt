package com.example.weanimals.lostandfound.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.lostandfound.domain.AnimalSize
import com.example.weanimals.lostandfound.domain.AnimalSpecies
import com.example.weanimals.lostandfound.domain.PetMatch

class LostAndFoundPresenter :
    BasePresenter<LostAndFoundContract.View>(),
    LostAndFoundContract.Presenter {

    private var state = LostAndFoundState()

    override fun start() = render()

    override fun submitForm(species: AnimalSpecies, size: AnimalSize) {
        state = state.copy(
            step = LostAndFoundState.Step.MATCHES,
            species = species,
            size = size,
            selectedMatch = null
        )
        render()
    }

    override fun openMatch(match: PetMatch) {
        state = state.copy(
            step = LostAndFoundState.Step.DETAILS,
            selectedMatch = match
        )
        render()
    }

    override fun openVerification() {
        state = state.copy(step = LostAndFoundState.Step.VERIFICATION)
        render()
    }

    override fun submitVerification(answers: List<String>) {
        state = state.copy(
            step = LostAndFoundState.Step.WAITING,
            answers = answers
        )
        render()
    }

    override fun submitReport() {
        state = state.copy(step = LostAndFoundState.Step.REPORT_SENT)
        render()
    }

    override fun goBack() {
        state = state.copy(
            step = when (state.step) {
                LostAndFoundState.Step.FORM -> null
                LostAndFoundState.Step.MATCHES -> LostAndFoundState.Step.FORM
                LostAndFoundState.Step.DETAILS -> LostAndFoundState.Step.MATCHES
                LostAndFoundState.Step.VERIFICATION -> LostAndFoundState.Step.DETAILS
                LostAndFoundState.Step.WAITING,
                LostAndFoundState.Step.REPORT_SENT -> LostAndFoundState.Step.MATCHES
            } ?: return
        )
        render()
    }

    private fun render() {
        withView { it.render(state) }
    }
}
