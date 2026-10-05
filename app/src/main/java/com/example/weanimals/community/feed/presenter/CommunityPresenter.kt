package com.example.weanimals.community.feed.presenter

import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.domain.CommunityFilter
import com.example.weanimals.community.feed.interactor.GetCommunityFeedInteractor
import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.interactor.GetCurrentLocationInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class CommunityPresenter(
    private val getFeed: GetCommunityFeedInteractor,
    private val getCurrentLocation: GetCurrentLocationInteractor
) :
    BasePresenter<CommunityContract.View>(), CommunityContract.Presenter {

    private var job: Job? = null
    private var allItems: List<CommunityFeedItem> = emptyList()
    private var filter = CommunityFilter.ALL
    private var query = ""
    private var currentNeighborhood: String? = null
    private var neighborhoodJob: Job? = null

    override fun load() {
        job?.cancel()
        withView(CommunityContract.View::showLoading)
        job = presenterScope.launch {
            getFeed().onSuccess { items ->
                allItems = items
                render()
            }.onFailure {
                withView(CommunityContract.View::showError)
            }
        }
    }

    override fun onFilterSelected(filter: CommunityFilter) {
        this.filter = filter
        if (!filter.requiresNeighborhood()) {
            neighborhoodJob?.cancel()
            neighborhoodJob = null
            render()
            return
        }
        if (currentNeighborhood == null) {
            neighborhoodJob?.cancel()
            withView(CommunityContract.View::showNeighborhoodLoading)
            neighborhoodJob = presenterScope.launch {
                getCurrentLocation().fold(
                    onSuccess = { location ->
                        currentNeighborhood = extractNeighborhood(location)
                        neighborhoodJob = null
                        render()
                    },
                    onFailure = {
                        neighborhoodJob = null
                        withView(CommunityContract.View::showNeighborhoodUnavailable)
                    }
                )
            }
            return
        }
        render()
    }

    override fun onSearchChanged(query: String) {
        this.query = query.trim()
        render()
    }

    override fun detachView() {
        job?.cancel()
        neighborhoodJob?.cancel()
        super.detachView()
    }

    private fun render() {
        val items = allItems.filter { item ->
            val categoryMatches = when (filter) {
                CommunityFilter.ALL -> true
                CommunityFilter.CAMPAIGNS -> item is CommunityFeedItem.Campaign
                CommunityFilter.POSTS -> item is CommunityFeedItem.Post
                CommunityFilter.FOUND -> item.category == CommunityCategory.FOUND
                CommunityFilter.QUESTION -> item.category == CommunityCategory.QUESTION
                CommunityFilter.NOTICE -> item.category == CommunityCategory.NOTICE
                CommunityFilter.NEUTERING -> item.category == CommunityCategory.NEUTERING
                CommunityFilter.VACCINATION -> item.category == CommunityCategory.VACCINATION
                CommunityFilter.ADOPTION -> item.category == CommunityCategory.ADOPTION
                CommunityFilter.DONATION -> item.category == CommunityCategory.DONATION
                CommunityFilter.CAMPAIGN_NEIGHBORHOOD ->
                    item is CommunityFeedItem.Campaign && sameNeighborhood(item)
                CommunityFilter.POST_NEIGHBORHOOD ->
                    item is CommunityFeedItem.Post && sameNeighborhood(item)
                CommunityFilter.MY_NEIGHBORHOOD -> sameNeighborhood(item)
            }
            categoryMatches && (query.isEmpty() || when (item) {
                is CommunityFeedItem.Campaign -> listOf(
                    item.title, item.organization, item.locationText
                ).any { it.contains(query, ignoreCase = true) }
                is CommunityFeedItem.Post -> listOf(
                    item.author, item.body, item.neighborhood.orEmpty()
                ).any { it.contains(query, ignoreCase = true) }
            })
        }
        withView { view ->
            if (items.isEmpty()) view.showEmpty(filter, query) else view.showItems(items)
        }
    }

    private fun sameNeighborhood(item: CommunityFeedItem): Boolean {
        val userNeighborhood = currentNeighborhood ?: return false
        val itemNeighborhood = item.neighborhood ?: return false
        return normalizeNeighborhood(itemNeighborhood) == normalizeNeighborhood(userNeighborhood)
    }

    private fun extractNeighborhood(location: LocationDetails): String? {
        val neighborhood = location.secondaryAddress.substringBefore(" — ").trim()
            .ifBlank { location.address.substringAfterLast(" — ", "").trim() }
        return neighborhood.takeIf(String::isNotBlank)
    }

    private fun normalizeNeighborhood(value: String): String {
        return value.substringBefore(" · ")
            .substringBefore(" — ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    private fun CommunityFilter.requiresNeighborhood(): Boolean = this ==
        CommunityFilter.MY_NEIGHBORHOOD || this == CommunityFilter.CAMPAIGN_NEIGHBORHOOD ||
        this == CommunityFilter.POST_NEIGHBORHOOD
}
