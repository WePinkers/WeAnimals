package com.example.weanimals.reporting.report.interactor

import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.report.domain.ReportDraft

class PrepareReportDraftInteractor {
    operator fun invoke(
        animalType: String,
        urgency: String,
        description: String,
        location: LocationDetails?,
        photoUri: String?
    ): Result<ReportDraft> = runCatching {
        val normalizedDescription = description.trim()
        if (normalizedDescription.isEmpty()) throw DescriptionRequiredException()

        val selectedLocation = location ?: throw LocationRequiredException()
        val normalizedAddress = listOf(
            selectedLocation.address,
            selectedLocation.secondaryAddress
        )
            .filter(String::isNotBlank)
            .joinToString(" — ")
        if (normalizedAddress.isEmpty()) throw LocationRequiredException()

        ReportDraft(
            animalType = animalType,
            urgency = urgency,
            description = normalizedDescription,
            address = normalizedAddress,
            latitude = selectedLocation.latitude,
            longitude = selectedLocation.longitude,
            photoUri = photoUri
        )
    }
}

class DescriptionRequiredException : IllegalArgumentException()

class LocationRequiredException : IllegalArgumentException()
