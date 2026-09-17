package com.example.weanimals.community.create.presenter

import com.example.weanimals.community.create.domain.CommunityPostDraft
import com.example.weanimals.community.create.interactor.PublishCommunityPostInteractor
import com.example.weanimals.community.create.repository.CommunityPostRepository
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.interactor.GetCurrentLocationInteractor
import com.example.weanimals.reporting.locationsearch.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CreateCommunityPostPresenterTest {
    private val dispatcher = StandardTestDispatcher()
    private val location = LocationDetails(-23.55, -46.63, "Rua Exemplo — Centro")
    private val repository = RecordingRepository()
    private val view = RecordingView()
    private lateinit var presenter: CreateCommunityPostPresenter

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        presenter = CreateCommunityPostPresenter(
            GetCurrentLocationInteractor(object : LocationRepository {
                override suspend fun getCurrentLocation() = Result.success(location)
                override suspend fun searchLocations(query: String) =
                    Result.success(emptyList<LocationDetails>())
            }),
            PublishCommunityPostInteractor(repository)
        )
        presenter.attachView(view)
    }

    @After fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test fun requiresTextAndLocationBeforePublishing() {
        presenter.publish("  ", null)
        assertTrue(view.descriptionRequired)
        presenter.publish("Vi um animal perdido", null)
        assertTrue(view.locationRequired)
        assertEquals(null, repository.draft)
    }

    @Test fun publishesSelectedCategoryAndLocation() = runTest(dispatcher) {
        presenter.selectCategory(CommunityCategory.QUESTION)
        presenter.selectLocation(location)
        presenter.publish("Alguém pode ajudar?", "content://photo")
        assertTrue(view.submitting)
        advanceUntilIdle()
        assertEquals(CommunityCategory.QUESTION, repository.draft?.category)
        assertEquals(location, repository.draft?.location)
        assertEquals("content://photo", repository.draft?.photoUri)
        assertTrue(view.published)
    }

    @Test fun loadsCurrentLocationAndRejectsCampaignCategory() = runTest(dispatcher) {
        presenter.selectCategory(CommunityCategory.VACCINATION)
        assertEquals(CommunityCategory.FOUND, view.category)
        presenter.loadCurrentLocation()
        advanceUntilIdle()
        assertEquals(location, view.location)
        assertFalse(view.locationRequired)
    }

    private class RecordingRepository : CommunityPostRepository {
        var draft: CommunityPostDraft? = null
        override suspend fun publish(draft: CommunityPostDraft): Result<String> {
            this.draft = draft
            return Result.success("post-1")
        }
    }

    private class RecordingView : CreateCommunityPostContract.View {
        var category = CommunityCategory.FOUND
        var location: LocationDetails? = null
        var descriptionRequired = false
        var locationRequired = false
        var submitting = false
        var published = false
        override fun showCategorySelected(category: CommunityCategory) { this.category = category }
        override fun showLocationLoading() = Unit
        override fun showLocation(location: LocationDetails) { this.location = location }
        override fun showLocationPermissionRequired() = Unit
        override fun showLocationUnavailable() = Unit
        override fun showDescriptionRequired() { descriptionRequired = true }
        override fun showLocationRequired() { locationRequired = true }
        override fun showSubmitting(submitting: Boolean) { this.submitting = submitting }
        override fun showPublishError(error: Throwable) = Unit
        override fun showPublished() { published = true }
    }
}
