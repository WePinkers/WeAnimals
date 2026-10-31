package com.example.weanimals.user.ombudsman.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityAccountIssuesBinding

class AccountIssuesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountIssuesBinding
    private var selectedOptionText = "Não consigo entrar na conta"
    private var isOtherProblem = false

    private val attachScreenshotLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "print.jpg"
            binding.txtAttachedScreenshot.text = "Anexado: $fileName"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityAccountIssuesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.screenHeader.headerTitle.text = "Problemas com a conta"
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.btnAttachScreenshot.setOnClickListener {
            attachScreenshotLauncher.launch("image/*")
        }

        binding.inputDetails.doAfterTextChanged { text ->
            val count = text?.length ?: 0
            binding.charCounterIssue.text = "$count/1000"
            validateForm()
        }

        fun selectOption(cardId: Int) {
            binding.radioOpt1.isChecked = (cardId == 1)
            binding.radioOpt2.isChecked = (cardId == 2)
            binding.radioOpt3.isChecked = (cardId == 3)
            binding.radioOpt4.isChecked = (cardId == 4)
            binding.radioOpt5.isChecked = (cardId == 5)
            binding.radioOpt6.isChecked = (cardId == 6)

            isOtherProblem = (cardId == 6)
            selectedOptionText = when (cardId) {
                1 -> "Não consigo entrar na conta"
                2 -> "Não recebo e-mail ou código"
                3 -> "Conta bloqueada ou suspensa"
                4 -> "Suspeita de acesso indevido"
                5 -> "Erro ao cadastrar ou editar dados"
                else -> "Outro problema"
            }

            if (isOtherProblem) {
                binding.labelDetails.text = "DESCREVA O PROBLEMA *"
                binding.labelDetails.setTextColor(ContextCompat.getColor(this, R.color.pine700))
                binding.helperTextOther.visibility = View.VISIBLE
                binding.btnAttachScreenshot.visibility = View.VISIBLE
                binding.inputDetails.hint = "Conte o que aconteceu, quando começou e o que você já tentou fazer..."
            } else {
                binding.labelDetails.text = "DETALHES (OPCIONAL)"
                binding.labelDetails.setTextColor(ContextCompat.getColor(this, R.color.muted))
                binding.helperTextOther.visibility = View.GONE
                binding.btnAttachScreenshot.visibility = View.GONE
                binding.inputDetails.hint = "Conte mais sobre o que aconteceu…"
            }
            validateForm()
        }

        binding.cardOpt1.setOnClickListener { selectOption(1) }
        binding.cardOpt2.setOnClickListener { selectOption(2) }
        binding.cardOpt3.setOnClickListener { selectOption(3) }
        binding.cardOpt4.setOnClickListener { selectOption(4) }
        binding.cardOpt5.setOnClickListener { selectOption(5) }
        binding.cardOpt6.setOnClickListener { selectOption(6) }

        binding.btnSubmitIssue.setOnClickListener {
            val details = binding.inputDetails.text.toString().trim()
            if (isOtherProblem && details.isBlank()) {
                binding.inputDetails.error = "Descrição obrigatória para outro problema"
                Toast.makeText(this, "Por favor, descreva o problema ocorrido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AccountIssueSentActivity::class.java).apply {
                putExtra("extra_issue_type", selectedOptionText)
                putExtra("extra_issue_details", details)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun validateForm() {
        val details = binding.inputDetails.text.toString().trim()
        val isValid = !isOtherProblem || details.isNotBlank()
        binding.btnSubmitIssue.isEnabled = isValid
        binding.btnSubmitIssue.alpha = if (isValid) 1.0f else 0.5f
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
