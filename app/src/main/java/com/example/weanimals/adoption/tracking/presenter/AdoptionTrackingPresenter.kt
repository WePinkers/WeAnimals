package com.example.weanimals.adoption.tracking.presenter

import com.example.weanimals.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.adoption.tracking.domain.AdoptionApplication
import com.example.weanimals.adoption.tracking.interactor.ObserveAdoptionApplicationInteractor
import com.example.weanimals.adoption.tracking.interactor.WithdrawAdoptionApplicationInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdoptionTrackingPresenter(
    private val summary: AdoptionSentSummary,
    private val observeApplication: ObserveAdoptionApplicationInteractor,
    private val withdrawApplication: WithdrawAdoptionApplicationInteractor
) : BasePresenter<AdoptionTrackingContract.View>(), AdoptionTrackingContract.Presenter {
    private var observation: Job? = null
    private var withdrawing = false
    private var currentStatus: AdoptionApplicationStatus? = null
    private var currentApplication: AdoptionApplication? = null

    override fun start() {
        withView { it.showLoading() }
        observation?.cancel()
        observation = presenterScope.launch {
            observeApplication(summary).collect { result ->
                result.fold(onSuccess = ::show, onFailure = { error ->
                    withView { it.showLoadError(error) }
                })
            }
        }
    }

    override fun stop() {
        observation?.cancel()
        observation = null
    }

    override fun onWithdrawClicked() {
        if (currentStatus in WITHDRAWABLE_STATUSES && !withdrawing) {
            withView { it.confirmWithdrawal() }
        }
    }

    override fun onWithdrawConfirmed() {
        if (withdrawing || currentStatus !in WITHDRAWABLE_STATUSES) return
        withdrawing = true
        withView { it.showWithdrawalLoading(true) }
        presenterScope.launch {
            try {
                withdrawApplication(summary.animalId).fold(
                    onSuccess = { withdrawing = false; withView { it.showWithdrawalLoading(false) } },
                    onFailure = { error ->
                        withdrawing = false
                        withView {
                            it.showWithdrawalLoading(false)
                            it.showWithdrawalError(error)
                        }
                    }
                )
            } catch (cancelled: CancellationException) {
                withdrawing = false
                throw cancelled
            }
        }
    }

    override fun onChatClicked() {
        currentApplication?.summary?.let { animal ->
            withView { it.openAdoptionChat(animal) }
        }
    }
    override fun onOtherAnimalsClicked() = withView { it.openAdoptionListing() }

    private fun show(application: AdoptionApplication) {
        currentStatus = application.status
        currentApplication = application
        withView { it.showApplication(application) }
    }

    private companion object {
        val WITHDRAWABLE_STATUSES = setOf(
            AdoptionApplicationStatus.PENDING,
            AdoptionApplicationStatus.REVIEWING
        )
    }
}
