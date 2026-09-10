package com.example.weanimals.adoption.listing.presenter

import com.example.weanimals.adoption.listing.FakeAnimalRepository
import com.example.weanimals.adoption.listing.FakeUserLocationRepository
import com.example.weanimals.adoption.listing.TestCursor
import com.example.weanimals.adoption.listing.animal
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.example.weanimals.adoption.listing.interactor.GetAvailableAnimalsInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionPresenterTest {
    private val repository = FakeAnimalRepository()
    private val view = RecordingView()
    private lateinit var presenter: AdoptionPresenter

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = AdoptionPresenter(
            GetAvailableAnimalsInteractor(repository),
            GetUserLocationInteractor(FakeUserLocationRepository())
        )
        presenter.attachView(view)
    }

    @After fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test fun rapidScrollRequestsOnlyOneNextPageAndDeduplicatesAnimals() = runTest {
        val secondPage = CompletableDeferred<Result<AnimalPage>>()
        repository.answer = {
            if (it.cursor == null) Result.success(AnimalPage(listOf(animal("a")), TestCursor(1)))
            else secondPage.await()
        }
        presenter.start()
        runCurrent()
        repeat(5) { presenter.onScrolledToEnd() }
        runCurrent()
        assertEquals(2, repository.requests.size)
        secondPage.complete(Result.success(AnimalPage(listOf(animal("a"), animal("b")))))
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), view.animals.map { it.id })
        presenter.onScrolledToEnd()
        assertEquals(2, repository.requests.size)
    }

    @Test fun aCancelledFilterCannotOverwriteTheNewFilter() = runTest {
        val stale = CompletableDeferred<Result<AnimalPage>>()
        repository.answer = {
            if (it.filter == SpeciesFilter.ALL) withContext(NonCancellable) { stale.await() }
            else Result.success(AnimalPage(listOf(animal("cat"))))
        }
        presenter.start()
        runCurrent()
        presenter.onFilterSelected(SpeciesFilter.CATS)
        runCurrent()
        stale.complete(Result.success(AnimalPage(listOf(animal("old-dog")))))
        advanceUntilIdle()
        assertEquals(SpeciesFilter.CATS, view.filter)
        assertEquals(listOf("cat"), view.animals.map { it.id })
        assertNull(view.lastError)
    }

    @Test fun paginationFailurePreservesRowsAndRetryUsesSameCursor() = runTest {
        val next = TestCursor(1)
        var failNext = true
        repository.answer = {
            when {
                it.cursor == null -> Result.success(AnimalPage(listOf(animal("a")), next))
                failNext -> Result.failure(IllegalStateException("offline"))
                else -> Result.success(AnimalPage(listOf(animal("b"))))
            }
        }
        presenter.start()
        advanceUntilIdle()
        presenter.onScrolledToEnd()
        advanceUntilIdle()
        assertEquals(listOf("a"), view.animals.map { it.id })
        assertTrue(view.errorWithAnimals)
        presenter.onScrolledToEnd()
        assertEquals(2, repository.requests.size) // no automatic retry loop
        failNext = false
        presenter.onRetryClicked()
        advanceUntilIdle()
        assertSame(next, repository.requests.last().cursor)
        assertEquals(listOf("a", "b"), view.animals.map { it.id })
    }

    @Test fun emptyMessagesFollowFilterAndResetPagination() = runTest {
        presenter.start(SpeciesFilter.CATS)
        advanceUntilIdle()
        assertEquals(SpeciesFilter.CATS, view.emptyFilter)
        presenter.onFilterSelected(SpeciesFilter.DOGS)
        advanceUntilIdle()
        assertEquals(SpeciesFilter.DOGS, view.emptyFilter)
        assertTrue(repository.requests.all { it.cursor == null })
    }

    @Test fun locationUnavailableHasItsOwnStateAndPermissionAction() = runTest {
        presenter.start(SpeciesFilter.NEAREST)
        advanceUntilIdle()
        assertTrue(view.needsLocation)
        assertNull(view.lastError)
        assertTrue(repository.requests.isEmpty())
        presenter.onRetryClicked()
        assertTrue(view.permissionRequested)
    }

    @Test fun detachedViewIsNotCalledAndReattachingRestoresTheResult() = runTest {
        val response = CompletableDeferred<Result<AnimalPage>>()
        repository.answer = { response.await() }
        presenter.start()
        runCurrent()
        presenter.detachView()
        response.complete(Result.success(AnimalPage(listOf(animal("a")))))
        advanceUntilIdle()
        assertTrue(view.animals.isEmpty())
        presenter.attachView(view)
        presenter.start()
        assertEquals(listOf("a"), view.animals.map { it.id })
        assertEquals(1, repository.requests.size)
    }

    @Test fun followsAnEmptyServerPageThatStillHasACursor() = runTest {
        repository.answer = {
            if (it.cursor == null) Result.success(AnimalPage(emptyList(), TestCursor(1)))
            else Result.success(AnimalPage(listOf(animal("valid"))))
        }
        presenter.start()
        advanceUntilIdle()
        assertEquals(listOf("valid"), view.animals.map { it.id })
        assertNull(view.emptyFilter)
    }

    @Test fun onlyAnAnimalFromTheCurrentListCanRequestDetails() = runTest {
        repository.answer = { Result.success(AnimalPage(listOf(animal("a")))) }
        presenter.start()
        advanceUntilIdle()
        presenter.onPetClicked("unknown")
        assertNull(view.selectedAnimal)
        presenter.onPetClicked("a")
        assertEquals("a", view.selectedAnimal)
    }

    @Test fun anEmptyFirstPageAfterFilterChangeDoesNotAppendToTheOldFilter() = runTest {
        repository.answer = {
            Result.success(when {
                it.filter == SpeciesFilter.ALL -> AnimalPage(listOf(animal("old")))
                it.cursor == null -> AnimalPage(emptyList(), TestCursor(1))
                else -> AnimalPage(listOf(animal("cat")))
            })
        }
        presenter.start()
        advanceUntilIdle()
        presenter.onFilterSelected(SpeciesFilter.CATS)
        advanceUntilIdle()
        assertEquals(listOf("cat"), view.animals.map { it.id })
    }

    private class RecordingView : AdoptionContract.View {
        var animals = emptyList<Animal>()
        var filter: SpeciesFilter? = null
        var emptyFilter: SpeciesFilter? = null
        var lastError: Throwable? = null
        var errorWithAnimals = false
        var needsLocation = false
        var permissionRequested = false
        var selectedAnimal: String? = null
        override fun showAnimals(animals: List<Animal>) { this.animals = animals }
        override fun appendAnimals(animals: List<Animal>) { this.animals += animals }
        override fun showFilterSelected(filter: SpeciesFilter) { this.filter = filter }
        override fun showEmptyState(filter: SpeciesFilter) { emptyFilter = filter }
        override fun showLoading(nextPage: Boolean) = Unit
        override fun hideLoading() = Unit
        override fun showError(error: Throwable, hasAnimals: Boolean) {
            lastError = error
            errorWithAnimals = hasAnimals
        }
        override fun showLocationRequired() { needsLocation = true }
        override fun requestLocationPermission() { permissionRequested = true }
        override fun openAnimalDetails(animalId: String) {
            selectedAnimal = animalId
        }
    }
}
