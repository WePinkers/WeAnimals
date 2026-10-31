package com.example.weanimals.user.map.overview.repository

import com.example.weanimals.user.map.overview.domain.PublicOccurrence

interface PublicOccurrenceRepository {
    suspend fun getOccurrences(): Result<List<PublicOccurrence>>
}
