package com.example.weanimals.community.feed.presenter

import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.domain.CommunityFilter

interface CommunityContract {
    interface View {
        fun showLoading()
        fun showNeighborhoodLoading()
        fun showItems(items: List<CommunityFeedItem>)
        fun showEmpty(filter: CommunityFilter, query: String)
        fun showNeighborhoodUnavailable()
        fun showError()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun load()
        fun onFilterSelected(filter: CommunityFilter)
        fun onSearchChanged(query: String)
    }
}
