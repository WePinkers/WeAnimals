package com.example.weanimals.adoption.confirmation.presenter

import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.confirmation.domain.AlreadyAppliedException
import com.example.weanimals.adoption.confirmation.interactor.GetAdoptionCandidateInteractor
import com.example.weanimals.adoption.confirmation.interactor.SubmitAdoptionApplicationInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AdoptionConfirmationPresenter(
    private val animalId: String,
    private val getCandidate: GetAdoptionCandidateInteractor,
    private val submitApplication: SubmitAdoptionApplicationInteractor
) : BasePresenter<AdoptionConfirmationContract.View>(), AdoptionConfirmationContract.Presenter {
    private var candidate: AdoptionCandidate? = null
    private var loadError: Throwable? = null
    private var loading = false
    private var submitting = false
    private var submitted = false

    override fun start() {
        if (candidate == null && loadError == null && !loading) load()
        else render()
    }

    override fun onRetryClicked() {
        if (!loading) load()
    }

    override fun onSubmitClicked() {
        val selected = candidate ?: return
        if (submitting || submitted) return
        submitting = true
        withView { it.showSubmitting(true) }
        presenterScope.launch {
            try {
                submitApplication(selected).fold(
                    onSuccess = {
                        submitting = false
                        submitted = true
                        withView { it.showSubmitted() }
                    },
                    onFailure = { error ->
                        submitting = false
                        if (error is AlreadyAppliedException) submitted = true
                        withView {
                            if (error is AlreadyAppliedException) it.showSubmitted()
                            else {
                                it.showSubmitting(false)
                                it.showSubmitError(error)
                            }
                        }
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    override fun onBackClicked() = withView(AdoptionConfirmationContract.View::closeScreen)

    private fun load() {
        loading = true
        loadError = null
        withView { it.showLoading() }
        presenterScope.launch {
            try {
                getCandidate(animalId).fold(
                    onSuccess = {
                        candidate = it
                        loading = false
                        render()
                    },
                    onFailure = {
                        loadError = it
                        loading = false
                        render()
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun render() = withView { view ->
        when {
            loading -> view.showLoading()
            candidate != null -> {
                view.showCandidate(requireNotNull(candidate))
                if (submitted) view.showSubmitted() else view.showSubmitting(submitting)
            }
            loadError != null -> view.showLoadError(requireNotNull(loadError))
        }
    }
}
