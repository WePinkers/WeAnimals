package com.example.weanimals.community.create.domain

import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails

data class CommunityPostDraft(
    val category: CommunityCategory,
    val body: String,
    val location: LocationDetails,
    val photoUri: String?
)
