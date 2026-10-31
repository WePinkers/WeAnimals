package com.example.weanimals.user.adoption.listing.domain

enum class SpeciesFilter(val species: Species? = null) {
    ALL,
    DOGS(Species.DOG),
    CATS(Species.CAT),
    NEAREST
}
