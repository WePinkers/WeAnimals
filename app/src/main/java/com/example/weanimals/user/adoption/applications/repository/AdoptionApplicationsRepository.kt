package com.example.weanimals.user.adoption.applications.repository

import com.example.weanimals.user.adoption.applications.domain.MyAdoptionApplication
import kotlinx.coroutines.flow.Flow

interface AdoptionApplicationsRepository {
    fun observe(): Flow<Result<List<MyAdoptionApplication>>>
}
