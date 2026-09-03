package com.example.weanimals.reporting.locationsearch.domain

data class LocationDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val secondaryAddress: String = ""
)
