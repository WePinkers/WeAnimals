package com.example.weanimals.user.lostandfound.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class LostAnimalFormData(
    var photoUri: String? = null,
    var name: String = "",
    var species: String = "dog",
    var size: String = "medium",
    var age: String = "adult",
    var breed: String = "SRD (sem raça definida)",
    var distinctiveMarks: String = "",
    var identification: MutableSet<String> = mutableSetOf(),
    var behavior: String = "Dócil com estranhos",
    var secretDetail: String = "",
    var lastLocationTitle: String = "Vila Marlene",
    var lastLocationAddress: String = "Perto da Rua Aimberê",
    var date: String = "",
    var time: String = "",
    var usualWalkArea: String = ""
)

class LostAnimalViewModel : ViewModel() {

    val formData = LostAnimalFormData()

    private val _currentStep = MutableLiveData(0)
    val currentStep: LiveData<Int> = _currentStep

    fun nextStep() {
        val current = _currentStep.value ?: 0
        if (current < 2) {
            _currentStep.value = current + 1
        }
    }

    fun previousStep() {
        val current = _currentStep.value ?: 0
        if (current > 0) {
            _currentStep.value = current - 1
        }
    }

    fun setStep(step: Int) {
        if (step in 0..2) {
            _currentStep.value = step
        }
    }
}
