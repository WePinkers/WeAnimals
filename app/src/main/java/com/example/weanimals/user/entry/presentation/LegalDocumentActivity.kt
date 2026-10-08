package com.example.weanimals.user.entry.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityLegalDocumentBinding

class LegalDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLegalDocumentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityLegalDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val documentType = intent.getStringExtra(EXTRA_DOCUMENT_TYPE) ?: DOCUMENT_TERMS
        val isPrivacyPolicy = documentType == DOCUMENT_PRIVACY
        binding.legalHeader.headerTitle.setText(
            if (isPrivacyPolicy) R.string.legal_privacy_title else R.string.legal_terms_title
        )
        binding.legalDocumentTitle.setText(
            if (isPrivacyPolicy) R.string.legal_privacy_title else R.string.legal_terms_title
        )
        val legalBody = if (isPrivacyPolicy) {
            getString(R.string.legal_privacy_body) +
                getString(R.string.legal_accreditation_privacy_section)
        } else {
            getString(R.string.legal_terms_body) +
                getString(R.string.legal_accreditation_terms_section)
        }
        binding.legalDocumentBody.text = legalBody
        binding.legalHeader.backButton.setOnClickListener { finish() }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        const val EXTRA_DOCUMENT_TYPE = "extra_document_type"
        const val DOCUMENT_TERMS = "terms"
        const val DOCUMENT_PRIVACY = "privacy"
    }
}
