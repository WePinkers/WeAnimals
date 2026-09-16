package com.example.weanimals.adoption.sent.presenter

import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionSentPresenterTest {
    private val summary = AdoptionSentSummary("animal-1", "Canela", "Abrigo", 74)
    private val view = RecordingView()
    private lateinit var presenter: AdoptionSentPresenter

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = AdoptionSentPresenter(summary)
        presenter.attachView(view)
    }

    @After fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test fun rendersSummaryAndOpensTracking() {
        presenter.start()
        assertEquals(summary, view.summary)
        presenter.onTrackClicked()
        assertEquals(summary, view.openedTracking)
    }

    @Test fun otherAnimalsReturnsToListing() {
        presenter.start()
        presenter.onOtherAnimalsClicked()
        assertTrue(view.openedListing)
    }

    private class RecordingView : AdoptionSentContract.View {
        var summary: AdoptionSentSummary? = null
        var openedTracking: AdoptionSentSummary? = null
        var openedListing = false
        override fun showSummary(summary: AdoptionSentSummary) { this.summary = summary }
        override fun openTracking(summary: AdoptionSentSummary) { openedTracking = summary }
        override fun openAdoptionListing() { openedListing = true }
    }
}
