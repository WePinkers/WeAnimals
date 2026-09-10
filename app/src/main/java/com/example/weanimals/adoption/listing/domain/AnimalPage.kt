package com.example.weanimals.adoption.listing.domain

/** Opaque continuation: Firebase snapshots stay inside the repository implementation. */
interface AnimalPageCursor

data class AnimalPage(
    val animals: List<Animal>,
    val nextPage: AnimalPageCursor? = null
)
