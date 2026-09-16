package com.example.weanimals.adoption.tracking.presenter

import com.example.weanimals.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.adoption.tracking.domain.AdoptionApplication
import com.example.weanimals.adoption.tracking.interactor.ObserveAdoptionApplicationInteractor
import com.example.weanimals.adoption.tracking.interactor.WithdrawAdoptionApplicationInteractor
import com.example.weanimals.adoption.tracking.repository.AdoptionTrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionTrackingPresenterTest {
    private val summary = AdoptionSentSummary("animal-1", "Canela", "Abrigo Esperança", 74)
    private val repository = FakeRepository(summary)
    private val view = RecordingView()
    private lateinit var presenter: AdoptionTrackingPresenter

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = AdoptionTrackingPresenter(
            summary,
            ObserveAdoptionApplicationInteractor(repository),
            WithdrawAdoptionApplicationInteractor(repository)
        )
        presenter.attachView(view)
    }

    @After fun tearDown() {
        presenter.stop()
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test fun statusUpdatesRenderWithoutReopeningTheScreen() = runTest {
        presenter.start()
        advanceUntilIdle()
        assertEquals(AdoptionApplicationStatus.PENDING, view.application?.status)
        repository.events.value = Result.success(
            AdoptionApplication(summary, AdoptionApplicationStatus.REJECTED, 100L, 200L)
        )
        advanceUntilIdle()
        assertEquals(AdoptionApplicationStatus.REJECTED, view.application?.status)
        presenter.onWithdrawClicked()
        assertFalse(view.withdrawalConfirmed)
    }

    @Test fun pendingApplicationCanBeWithdrawnAfterConfirmation() = runTest {
        presenter.start()
        advanceUntilIdle()
        presenter.onWithdrawClicked()
        assertTrue(view.withdrawalConfirmed)
        presenter.onWithdrawConfirmed()
        advanceUntilIdle()
        assertEquals(1, repository.withdrawals)
        assertEquals(AdoptionApplicationStatus.CANCELLED, view.application?.status)
        assertFalse(view.withdrawalLoading)
    }

    @Test fun chatUsesTheAnimalSnapshotFromTheCurrentApplication() = runTest {
        presenter.start()
        advanceUntilIdle()
        val updated = summary.copy(animalName = "Pipoca", shelterName = "ONG Patas")
        repository.events.value = Result.success(
            AdoptionApplication(updated, AdoptionApplicationStatus.REVIEWING)
        )
        advanceUntilIdle()
        presenter.onChatClicked()
        assertEquals(updated, view.openedChat)
    }

    @Test fun loadErrorDoesNotPretendThereIsANewStatus() = runTest {
        repository.events.value = Result.failure(IllegalStateException("offline"))
        presenter.start()
        advanceUntilIdle()
        assertTrue(view.loadError is IllegalStateException)
        assertEquals(null, view.application)
    }

    private class FakeRepository(summary: AdoptionSentSummary) : AdoptionTrackingRepository {
        val events = MutableStateFlow<Result<AdoptionApplication>>(
            Result.success(AdoptionApplication(summary, AdoptionApplicationStatus.PENDING))
        )
        var observations = 0
        var withdrawals = 0
        override fun observe(summary: AdoptionSentSummary): Flow<Result<AdoptionApplication>> {
            observations++
            return events
        }
        override suspend fun withdraw(animalId: String): Result<Unit> {
            withdrawals++
            events.value = Result.success(events.value.getOrThrow().copy(
                status = AdoptionApplicationStatus.CANCELLED
            ))
            return Result.success(Unit)
        }
    }

    private class RecordingView : AdoptionTrackingContract.View {
        var application: AdoptionApplication? = null
        var loadError: Throwable? = null
        var withdrawalConfirmed = false
        var withdrawalLoading = false
        var openedChat: AdoptionSentSummary? = null
        override fun showLoading() = Unit
        override fun showApplication(application: AdoptionApplication) { this.application = application }
        override fun showLoadError(error: Throwable) { loadError = error }
        override fun confirmWithdrawal() { withdrawalConfirmed = true }
        override fun showWithdrawalLoading(loading: Boolean) { withdrawalLoading = loading }
        override fun showWithdrawalError(error: Throwable) = Unit
        override fun openAdoptionChat(summary: AdoptionSentSummary) { openedChat = summary }
        override fun openAdoptionListing() = Unit
    }
}
