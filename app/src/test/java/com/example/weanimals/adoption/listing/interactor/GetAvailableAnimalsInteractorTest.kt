package com.example.weanimals.adoption.listing.interactor

import com.example.weanimals.adoption.listing.FakeAnimalRepository
import com.example.weanimals.adoption.listing.TestCursor
import com.example.weanimals.adoption.listing.animal
import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.domain.UserLocationUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetAvailableAnimalsInteractorTest {
    private val repository = FakeAnimalRepository()
    private val interactor = GetAvailableAnimalsInteractor(repository)

    @Test fun forwardsSpeciesCursorAndPageSizeWithoutReorderingRecentAnimals() = runTest {
        val cursor = TestCursor(1)
        val next = TestCursor(2)
        repository.answer = { Result.success(AnimalPage(listOf(animal("far", 1.0), animal("near")), next)) }
        val page = interactor(SpeciesFilter.CATS, cursor, Coordinates(0.0, 0.0), 2).getOrThrow()

        assertEquals(SpeciesFilter.CATS, repository.requests.single().filter)
        assertSame(cursor, repository.requests.single().cursor)
        assertEquals(2, repository.requests.single().limit)
        assertSame(next, page.nextPage)
        assertEquals(listOf("far", "near"), page.animals.map { it.id })
        assertEquals(0.0, page.animals.last().distanceKm!!, 0.0001)
    }

    @Test fun nearestIncludesCloserAnimalsFromLaterServerPages() = runTest {
        repository.answer = {
            Result.success(if (it.cursor == null) {
                AnimalPage(listOf(animal("far", 2.0), animal("unknown", null, null)), TestCursor(1))
            } else {
                AnimalPage(listOf(animal("closest", 0.0), animal("middle", 1.0)))
            })
        }
        val origin = Coordinates(0.0, 0.0)
        val first = interactor(SpeciesFilter.NEAREST, location = origin, limit = 2).getOrThrow()
        val second = interactor(SpeciesFilter.NEAREST, first.nextPage, origin, 2).getOrThrow()

        assertEquals(listOf("closest", "middle"), first.animals.map { it.id })
        assertEquals(listOf("far", "unknown"), second.animals.map { it.id })
        assertNull(second.nextPage)
        assertNull(second.animals.last().distanceKm)
        assertEquals(2, repository.requests.size) // no second Firestore scan on scrolling
        assertTrue(repository.requests.all { it.filter == null })
    }

    @Test fun failedLaterPageDoesNotPresentIncompleteNearestRanking() = runTest {
        repository.answer = {
            if (it.cursor == null) Result.success(AnimalPage(listOf(animal("far")), TestCursor(1)))
            else Result.failure(IllegalStateException("network"))
        }
        val result = interactor(SpeciesFilter.NEAREST, location = Coordinates(0.0, 0.0))
        assertTrue(result.isFailure)
    }

    @Test fun nearestRequiresLocationWithoutReadingFirestore() = runTest {
        val result = interactor(SpeciesFilter.NEAREST)
        assertTrue(result.exceptionOrNull() is UserLocationUnavailableException)
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun allAnimalsAreAvailableWithoutLocationAndDistanceIsHidden() = runTest {
        repository.answer = { Result.success(AnimalPage(listOf(animal("a")))) }
        assertNull(interactor(SpeciesFilter.ALL).getOrThrow().animals.single().distanceKm)
    }

    @Test fun cancellationIsNotConvertedToLoadError() = runTest {
        repository.answer = { throw CancellationException("filter changed") }
        try {
            interactor(SpeciesFilter.ALL)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, repository.requests.size)
        }
    }
}
