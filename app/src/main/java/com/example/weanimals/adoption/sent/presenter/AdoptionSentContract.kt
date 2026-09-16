package com.example.weanimals.adoption.sent.presenter

import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary

interface AdoptionSentContract {
    interface View {
        fun showSummary(summary: AdoptionSentSummary)
        fun openTracking(summary: AdoptionSentSummary)
        fun openAdoptionListing()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onTrackClicked()
        fun onOtherAnimalsClicked()
    }
}
