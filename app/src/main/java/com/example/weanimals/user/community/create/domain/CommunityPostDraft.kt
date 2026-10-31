package com.example.weanimals.user.community.create.domain

import com.example.weanimals.user.community.feed.domain.CommunityCategory
import com.example.weanimals.user.reporting.locationsearch.domain.LocationDetails

data class CommunityPostDraft(
    val category: CommunityCategory,
    val body: String,
    val location: LocationDetails,
    val photoUri: String?
)
