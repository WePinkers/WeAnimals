package com.example.weanimals.adoption.sent.presenter

import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.core.base.BasePresenter

class AdoptionSentPresenter(
    private val summary: AdoptionSentSummary
) : BasePresenter<AdoptionSentContract.View>(), AdoptionSentContract.Presenter {

    override fun start() = withView { it.showSummary(summary) }

    override fun onTrackClicked() = withView { it.openTracking(summary) }

    override fun onOtherAnimalsClicked() = withView { it.openAdoptionListing() }
}
