package com.example.weanimals.user.community.create.domain

import com.example.weanimals.user.reporting.locationsearch.domain.LocationDetails

/** Keep the searchable location precise in the form, but publish only a broad area. */
fun LocationDetails.publicLocationLabel(): String? {
    val area = secondaryAddress.substringBefore(" — ").trim()
        .ifBlank { address.substringAfterLast(" — ", "").trim() }
    return area.take(80).takeIf(String::isNotBlank)
}
