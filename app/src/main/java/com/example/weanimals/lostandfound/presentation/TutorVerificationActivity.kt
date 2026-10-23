package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivityTutorVerificationBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import kotlinx.coroutines.launch

class TutorVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorVerificationBinding
    private var currentAd: FoundPetAd? = null
    private var selectedPhotoUri: Uri? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                loadPhotoPreview(uri)
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

        if (savedInstanceState != null) {
            val savedUriStr = savedInstanceState.getString("KEY_SAVED_PHOTO_URI")
            if (!savedUriStr.isNullOrBlank()) {
                selectedPhotoUri = Uri.parse(savedUriStr)
                loadPhotoPreview(selectedPhotoUri!!)
            }
        }

        setupListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedPhotoUri?.let { outState.putString("KEY_SAVED_PHOTO_URI", it.toString()) }
    }

    private fun loadPhotoPreview(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = ImageThumbnailLoader.loadThumbnail(
                this@TutorVerificationActivity,
                uri.toString(),
                600,
                400
            )
            if (bitmap != null) {
                binding.ivVerificationPreview.setImageBitmap(bitmap)
                binding.ivVerificationPreview.visibility = View.VISIBLE
                binding.btnRemoveVerificationPhoto.visibility = View.VISIBLE
            } else {
                Toast.makeText(
                    this@TutorVerificationActivity,
                    "Não foi possível carregar a prévia da foto.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBackVerification.setOnClickListener { finish() }

        binding.etPetName.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.etSpecialMark.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.layoutVerificationPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.btnRemoveVerificationPhoto.setOnClickListener {
            selectedPhotoUri = null
            binding.ivVerificationPreview.setImageDrawable(null)
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

        val currentProfileName = getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("user_name", null)
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
            ?: "Usuário"

        val request = TutorVerificationRequest(
            adId = ad.id,
            requesterUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "local_user",
            requesterName = currentProfileName,
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

        val intent = Intent(this, SubmittedAnswersActivity::class.java).apply {
            putExtra(SubmittedAnswersActivity.EXTRA_REQUEST_ID, request.id)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
