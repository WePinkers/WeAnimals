package com.example.weanimals.reporting.triage.domain

enum class TriageQuestion(val storageKey: String) {
    IMMEDIATE_DANGER("immediateDanger"),
    ACTIVE_ABUSE("activeAbuse"),
    INJURED_OR_IMMOBILIZED("injuredOrImmobilized"),
    STREET_SITUATION("streetSituation")
}

enum class TriageClassification(val storageValue: String) {
    VERY_URGENT("very_urgent"),
    URGENT("urgent"),
    PRIORITY("priority"),
    LOW("low")
}
