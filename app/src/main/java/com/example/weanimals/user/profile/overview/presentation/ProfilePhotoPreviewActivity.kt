package com.example.weanimals.user.profile.overview.presentation

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityProfilePhotoPreviewBinding
import java.io.File
import java.io.FileOutputStream

class ProfilePhotoPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePhotoPreviewBinding
    private var selectedUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            loadPhotoPreview(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePhotoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, "Foto não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        selectedUri = Uri.parse(uriString)

        binding.includeHeader.headerTitleTv.text = "Prévia da foto de perfil"
        binding.includeHeader.headerBackBtn.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        loadPhotoPreview(selectedUri!!)

        setupListeners()
    }

    private fun loadPhotoPreview(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                binding.ivPreviewAvatar.setImageBitmap(bitmap)
            } else {
                binding.ivPreviewAvatar.setImageURI(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.ivPreviewAvatar.setImageURI(uri)
        }
    }

    private fun setupListeners() {
        binding.btnChooseAnotherPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSavePhoto.setOnClickListener {
            val uri = selectedUri ?: return@setOnClickListener
            saveProfilePicture(uri)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun saveProfilePicture(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_picture.jpg")
            FileOutputStream(file).use { out ->
                inputStream?.copyTo(out)
            }
            val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
            prefs.edit().putString("profile_picture_path", file.absolutePath).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
