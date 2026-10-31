package com.example.weanimals.user.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivitySendQuestionBinding

class SendQuestionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendQuestionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivitySendQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupInteractions()
    }

    private fun setupSpinner() {
        val categories = listOf("Denúncias", "Adoção", "Conta", "Golpes", "Achados e Perdidos")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupInteractions() {
        binding.screenHeader.headerTitle.text = "Enviar pergunta"
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.inputQuestion.doAfterTextChanged { text ->
            val count = text?.length ?: 0
            binding.charCounter.text = "$count/500"
        }

        binding.btnSubmitQuestion.setOnClickListener {
            val question = binding.inputQuestion.text.toString().trim()
            if (question.isBlank()) {
                binding.inputQuestion.error = "Escreva sua dúvida"
                Toast.makeText(this, "Por favor, digite sua pergunta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(Intent(this, QuestionSentActivity::class.java))
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
