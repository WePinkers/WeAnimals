package com.example.weanimals.user.adoption.tracking.presenter

import com.example.weanimals.user.adoption.tracking.domain.AdoptionApplication
import com.example.weanimals.user.adoption.sent.domain.AdoptionSentSummary

interface AdoptionTrackingContract {
    interface View {
        fun showLoading()
        fun showApplication(application: AdoptionApplication)
        fun showLoadError(error: Throwable)
        fun confirmWithdrawal()
        fun showWithdrawalLoading(loading: Boolean)
        fun showWithdrawalError(error: Throwable)
        fun openAdoptionChat(summary: AdoptionSentSummary)
        fun openAdoptionListing()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun stop()
        fun onWithdrawClicked()
        fun onWithdrawConfirmed()
        fun onChatClicked()
        fun onOtherAnimalsClicked()
    }
}
