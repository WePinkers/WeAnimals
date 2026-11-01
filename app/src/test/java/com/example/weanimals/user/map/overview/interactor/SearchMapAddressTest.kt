package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SearchMapAddressTest {
    private class FakeGeocoder : MapAddressGeocoder {
        var available = true
        var online = true
        var result: Coordinates? = Coordinates(-23.55, -46.63)
        var failure: Exception? = null
        var latency = 0L
        val queries = mutableListOf<String>()
        override fun isAvailable() = available
        override fun isOnline() = online
        override suspend fun find(query: String): Coordinates? {
            queries.add(query)
            delay(latency)
            failure?.let { throw it }
            return result
        }
    }

    @Test fun emptyQueryNeverCallsProvider() = runTest {
        val provider = FakeGeocoder()
        assertEquals(MapAddressSearchResult.EmptyQuery, SearchMapAddress(provider)("  "))
        assertTrue(provider.queries.isEmpty())
    }

    @Test fun unavailableServiceDoesNotInventAResult() = runTest {
        val provider = FakeGeocoder().apply { available = false }
        assertEquals(MapAddressSearchResult.Unavailable, SearchMapAddress(provider)("Mooca, São Paulo"))
        assertTrue(provider.queries.isEmpty())
    }

    @Test fun offlineDoesNotCallProvider() = runTest {
        val provider = FakeGeocoder().apply { online = false }
        assertEquals(MapAddressSearchResult.Offline, SearchMapAddress(provider)("Mooca, São Paulo"))
        assertTrue(provider.queries.isEmpty())
    }

    @Test fun manualSearchTrimsQueryAndUsesProviderCoordinatesUnchanged() = runTest {
        val provider = FakeGeocoder()
        assertEquals(MapAddressSearchResult.Found(Coordinates(-23.55, -46.63)),
            SearchMapAddress(provider)("  Avenida Paulista, São Paulo  "))
        assertEquals(listOf("Avenida Paulista, São Paulo"), provider.queries)
    }

    @Test fun noResultsAreReportedWithoutAFallbackLocation() = runTest {
        val provider = FakeGeocoder().apply { result = null }
        assertEquals(MapAddressSearchResult.NotFound, SearchMapAddress(provider)("endereço inexistente"))
    }

    @Test fun providerExceptionIsReportedAsFailure() = runTest {
        val provider = FakeGeocoder().apply { failure = IOException("provider failed") }
        assertEquals(MapAddressSearchResult.Failed, SearchMapAddress(provider)("Vila Maria, São Paulo"))
    }

    @Test fun timeoutDoesNotReturnAFictitiousLocation() = runTest {
        val provider = FakeGeocoder().apply { latency = 2000 }
        assertEquals(MapAddressSearchResult.Failed, SearchMapAddress(provider, 100)("Vila Maria, São Paulo"))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun cancellingAQueryCannotDeliverALateResult() = runTest {
        val provider = FakeGeocoder().apply { latency = 2000 }
        val request = async { SearchMapAddress(provider)("Vila Maria, São Paulo") }
        runCurrent()
        request.cancel()
        assertTrue(request.isCancelled)
    }
}
