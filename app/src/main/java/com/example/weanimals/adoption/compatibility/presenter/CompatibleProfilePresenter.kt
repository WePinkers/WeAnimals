package com.example.weanimals.adoption.compatibility.presenter

import com.example.weanimals.adoption.compatibility.domain.AnimalRecommendation
import com.example.weanimals.adoption.compatibility.interactor.GetAnimalRecommendationsInteractor
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.interactor.GetAdoptionProfileInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CompatibleProfilePresenter(
    private val originAnimalId: String,
    private val getProfile: GetAdoptionProfileInteractor,
    private val getRecommendations: GetAnimalRecommendationsInteractor
) : BasePresenter<CompatibleProfileContract.View>(), CompatibleProfileContract.Presenter {

    private var started = false
    private var loading = false
    private var profile: AdoptionProfile? = null
    private var recommendations = emptyList<AnimalRecommendation>()
    private var error: Throwable? = null

    override fun start() {
        if (!started) {
            started = true
            load()
        } else {
            render()
        }
    }

    override fun onRetryClicked() {
        if (!loading) load()
    }

    override fun onAnimalClicked(animalId: String) {
        if (recommendations.any { it.animal.id == animalId }) {
            withView { it.openAnimalDetails(animalId) }
        }
    }

    override fun onBackClicked() = withView(CompatibleProfileContract.View::closeScreen)

    private fun load() {
        loading = true
        error = null
        withView { it.showLoading() }
        presenterScope.launch {
            try {
                val savedProfile = getProfile().getOrElse {
                    fail(it)
                    return@launch
                }
                if (savedProfile == null) {
                    loading = false
                    withView { it.openQuestionnaire(originAnimalId) }
                    return@launch
                }
                profile = savedProfile
                recommendations = getRecommendations(savedProfile).getOrElse {
                    fail(it)
                    return@launch
                }
                loading = false
                render()
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun fail(cause: Throwable) {
        loading = false
        error = cause
        render()
    }

    private fun render() = withView { view ->
        when {
            loading -> view.showLoading()
            error != null -> view.showError(requireNotNull(error))
            profile != null && recommendations.isEmpty() -> view.showEmpty(requireNotNull(profile))
            profile != null -> view.showProfile(requireNotNull(profile), recommendations)
        }
    }
}
