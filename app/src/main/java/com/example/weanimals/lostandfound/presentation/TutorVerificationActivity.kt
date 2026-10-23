package com.example.weanimals.lostandfound.presentation

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityTutorVerificationBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository

class TutorVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorVerificationBinding
    private var currentAd: FoundPetAd? = null
    private var selectedPhotoUri: Uri? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                binding.ivVerificationPreview.setImageURI(uri)
                binding.ivVerificationPreview.visibility = View.VISIBLE
                binding.btnRemoveVerificationPhoto.visibility = View.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adId = intent.getStringExtra(EXTRA_AD_ID)
        if (adId.isNullOrBlank()) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentAd = LostAndFoundRepository.getAdById(this, adId)
        if (currentAd == null) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBackVerification.setOnClickListener { finish() }

        binding.etPetName.setOnClickListener {
            binding.etPetName.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(binding.etPetName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        binding.etSpecialMark.setOnClickListener {
            binding.etSpecialMark.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(binding.etSpecialMark, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        binding.layoutVerificationPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.btnRemoveVerificationPhoto.setOnClickListener {
            selectedPhotoUri = null
            binding.ivVerificationPreview.visibility = View.GONE
            binding.btnRemoveVerificationPhoto.visibility = View.GONE
        }

        binding.btnSubmitVerification.setOnClickListener {
            submitVerification()
        }
    }

    private fun submitVerification() {
        val ad = currentAd ?: return
        val petName = binding.etPetName.text.toString().trim()
        val specialMark = binding.etSpecialMark.text.toString().trim()

        var hasError = false
        if (petName.isBlank()) {
            binding.etPetName.error = "Informe o nome do animal."
            hasError = true
        }

        if (specialMark.isBlank()) {
            binding.etSpecialMark.error = "Descreva uma característica particular do animal."
            hasError = true
        }

        if (hasError) return

        binding.btnSubmitVerification.isEnabled = false

        val request = TutorVerificationRequest(
            adId = ad.id,
            ownerUserId = ad.ownerUserId,
            petName = petName,
            specialMark = specialMark,
            photoUri = selectedPhotoUri?.toString()
        )

        val saved = LostAndFoundRepository.saveVerificationRequest(this, request)
        if (!saved) {
            Toast.makeText(this, "Você já enviou uma solicitação para este anúncio.", Toast.LENGTH_SHORT).show()
            binding.btnSubmitVerification.isEnabled = true
            return
        }

        showSuccessDialog()
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Respostas enviadas!")
            .setMessage("Suas respostas foram enviadas para análise. A pessoa que encontrou o animal poderá verificar as informações antes de entrar em contato.")
            .setPositiveButton("Voltar ao anúncio") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
