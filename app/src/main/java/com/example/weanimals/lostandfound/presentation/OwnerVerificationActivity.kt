package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityOwnerVerificationBinding

class OwnerVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerVerificationBinding
    private var proofPhotoUri: String? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            proofPhotoUri = uri.toString()
            binding.proofPhotoPreview.setImageURI(uri)
            binding.proofPhotoPreview.visibility = View.VISIBLE
            binding.iconCamera.visibility = View.GONE
            binding.uploadLabel.text = "Foto adicionada"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOwnerVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.containerUploadProof.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSubmitVerification.setOnClickListener {
            val name = binding.inputPetName.text.toString().trim()
            val mark = binding.inputSecretMark.text.toString().trim()

            if (name.isBlank()) {
                binding.inputPetName.error = "Digite o nome do animal"
                Toast.makeText(this, "Por favor, informe o nome do animal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mark.isBlank()) {
                binding.inputSecretMark.error = "Informe a marca ou detalhe"
                Toast.makeText(this, "Por favor, informe uma marca ou detalhe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, VerificationSentActivity::class.java).apply {
                putExtra("extra_owner_name", name)
                putExtra("extra_secret_mark", mark)
                putExtra("extra_proof_uri", proofPhotoUri)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
