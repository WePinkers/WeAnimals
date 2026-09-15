package com.example.weanimals.adoption.confirmation.presenter

import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate

interface AdoptionConfirmationContract {
    interface View {
        fun showLoading()
        fun showCandidate(candidate: AdoptionCandidate)
        fun showLoadError(error: Throwable)
        fun showSubmitting(submitting: Boolean)
        fun showSubmitted()
        fun showSubmitError(error: Throwable)
        fun closeScreen()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onRetryClicked()
        fun onSubmitClicked()
        fun onBackClicked()
    }
}
