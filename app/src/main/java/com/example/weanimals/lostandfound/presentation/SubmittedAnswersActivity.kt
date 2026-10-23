package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivitySubmittedAnswersBinding
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository

class SubmittedAnswersActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubmittedAnswersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmittedAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val request = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (request == null) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvAnswerName.text = "Nome: ${request.petName}"
        binding.tvAnswerMark.text = "Marca: ${request.specialMark}"

        if (!request.photoUri.isNullOrBlank()) {
            binding.layoutPhotoAttached.visibility = View.VISIBLE
        } else {
            binding.layoutPhotoAttached.visibility = View.GONE
        }

        binding.btnBackToMyAds.setOnClickListener {
            val intent = Intent(this, MyAdsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
