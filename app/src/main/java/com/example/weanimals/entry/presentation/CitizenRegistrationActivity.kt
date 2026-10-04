package com.example.weanimals.entry.presentation

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.TextPaint
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.formatting.BrazilianDocumentMask
import com.example.weanimals.entry.auth.repository.FirebaseAuthRepository
import com.example.weanimals.databinding.ActivityCitizenRegistrationBinding
import com.example.weanimals.home.presentation.HomeActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern
import kotlinx.coroutines.launch

class CitizenRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenRegistrationBinding
    private val authRepository by lazy {
        FirebaseAuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCitizenRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerHeader.backButton.setOnClickListener { finish() }
        binding.registerHeader.headerTitle.setText(R.string.register_title)
        setRequiredLabel(binding.registerNameLabel, R.string.register_name_label)
        setRequiredLabel(binding.registerCpfLabel, R.string.register_cpf_label)
        setRequiredLabel(binding.registerEmailLabel, R.string.register_email_label)
        setRequiredLabel(binding.registerPasswordLabel, R.string.register_password_label)
        BrazilianDocumentMask.attachCpf(binding.registerCpfInput)
        binding.registerButton.setOnClickListener {
            register()
        }
        configureLegalLinks()
    }

    private fun register() {
        val fullName = binding.registerNameInput.text?.toString().orEmpty().trim()
        val cpf = binding.registerCpfInput.text?.toString().orEmpty().trim()
        val email = binding.registerEmailInput.text?.toString().orEmpty().trim().lowercase()
        val password = binding.registerPasswordInput.text?.toString().orEmpty()

        binding.registerNameInput.error = null
        binding.registerCpfInput.error = null
        binding.registerEmailInput.error = null
        binding.registerPasswordInput.error = null

        when {
            fullName.isBlank() -> showFieldError(binding.registerNameInput, R.string.register_name_required)
            !isValidCpf(cpf) ->
                showFieldError(binding.registerCpfInput, R.string.register_cpf_invalid)
            !EMAIL_PATTERN.matcher(email).matches() ->
                showFieldError(binding.registerEmailInput, R.string.register_email_invalid)
            password.length < MIN_PASSWORD_LENGTH ->
                showFieldError(binding.registerPasswordInput, R.string.register_password_invalid)
            !binding.legalAcceptanceCheckbox.isChecked ->
                showLegalAcceptanceError()
            else -> submitRegistration(fullName, cpf, email, password)
        }
    }

    private fun submitRegistration(
        fullName: String,
        cpf: String,
        email: String,
        password: String
    ) {
        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                authRepository.register(fullName, cpf, email, password)
            }.onSuccess {
                startActivity(Intent(this@CitizenRegistrationActivity, HomeActivity::class.java))
                finishAffinity()
            }.onFailure { error ->
                setLoading(false)
                val message = when (error) {
                    is FirebaseAuthUserCollisionException -> R.string.register_email_exists
                    is FirebaseAuthWeakPasswordException -> R.string.register_password_invalid
                    is FirebaseNetworkException -> R.string.login_network_error
                    else -> R.string.register_error_generic
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showFieldError(field: android.widget.EditText, messageRes: Int) {
        field.error = getString(messageRes)
        field.requestFocus()
    }

    private fun showLegalAcceptanceError() {
        binding.legalAcceptanceCheckbox.error = getString(R.string.legal_acceptance_required)
        binding.legalAcceptanceCheckbox.requestFocus()
        Snackbar.make(binding.root, R.string.legal_acceptance_required, Snackbar.LENGTH_LONG).show()
    }

    private fun isValidCpf(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        if (digits.length != CPF_LENGTH || digits.all { it == digits.first() }) return false

        val firstDigit = calculateCpfDigit(digits.take(9), 10)
        val secondDigit = calculateCpfDigit(digits.take(10), 11)
        return digits[9].digitToInt() == firstDigit && digits[10].digitToInt() == secondDigit
    }

    private fun calculateCpfDigit(value: String, initialWeight: Int): Int {
        val sum = value.mapIndexed { index, digit ->
            digit.digitToInt() * (initialWeight - index)
        }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    private fun setRequiredLabel(label: TextView, textRes: Int) {
        val text = getString(textRes)
        val styledText = SpannableString(text)
        val starIndex = text.lastIndexOf('*')
        if (starIndex >= 0) {
            styledText.setSpan(
                ForegroundColorSpan(getColor(R.color.alert600)),
                starIndex,
                starIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        label.text = styledText
    }

    private fun configureLegalLinks() {
        val fullText = getString(R.string.register_terms)
        val termsLabel = getString(R.string.legal_terms_label)
        val privacyLabel = getString(R.string.legal_privacy_label)
        val styledText = SpannableString(fullText)

        addLegalLink(styledText, termsLabel) {
            openLegalDocument(LegalDocumentActivity.DOCUMENT_TERMS)
        }
        addLegalLink(styledText, privacyLabel) {
            openLegalDocument(LegalDocumentActivity.DOCUMENT_PRIVACY)
        }

        binding.termsLink.text = styledText
        binding.termsLink.movementMethod = LinkMovementMethod.getInstance()
        binding.termsLink.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun addLegalLink(text: SpannableString, label: String, action: () -> Unit) {
        val start = text.toString().indexOf(label)
        if (start < 0) return
        text.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) = action()

                override fun updateDrawState(drawState: TextPaint) {
                    drawState.color = getColor(R.color.pine700)
                    drawState.isUnderlineText = true
                }
            },
            start,
            start + label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun openLegalDocument(documentType: String) {
        startActivity(
            Intent(this, LegalDocumentActivity::class.java)
                .putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, documentType)
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.registerButton.isEnabled = !loading
        binding.registerButton.setText(
            if (loading) R.string.register_loading else R.string.register_button
        )
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private companion object {
        const val CPF_LENGTH = 11
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
