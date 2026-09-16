package com.example.weanimals.map.overview.repository

import com.example.weanimals.map.overview.domain.PublicOccurrence

interface PublicOccurrenceRepository {
    suspend fun getOccurrences(): Result<List<PublicOccurrence>>
}
