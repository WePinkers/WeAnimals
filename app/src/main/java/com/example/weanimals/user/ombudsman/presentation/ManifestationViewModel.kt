package com.example.weanimals.user.ombudsman.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weanimals.user.ombudsman.domain.ManifestationType

data class ManifestationFormData(
    var type: ManifestationType = ManifestationType.RECLAMACAO,
    var subject: String = "",
    var description: String = "",
    var isAnonymous: Boolean = false,
    var attachedFileName: String? = null
)

class ManifestationViewModel : ViewModel() {

    val formData = ManifestationFormData()

    private val _currentStep = MutableLiveData(0)
    val currentStep: LiveData<Int> = _currentStep

    fun nextStep() {
        val current = _currentStep.value ?: 0
        if (current < 3) {
            _currentStep.value = current + 1
        }
    }

    fun previousStep() {
        val current = _currentStep.value ?: 0
        if (current > 0) {
            _currentStep.value = current - 1
        }
    }
}
