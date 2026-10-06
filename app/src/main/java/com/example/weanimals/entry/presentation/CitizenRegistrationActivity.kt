package com.example.weanimals.entry.presentation

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.databinding.DialogAuthMessageBinding
import com.example.weanimals.core.formatting.BrazilianDocumentMask
import com.example.weanimals.entry.auth.repository.FirebaseAuthRepository
import com.example.weanimals.databinding.ActivityCitizenRegistrationBinding
import com.example.weanimals.home.presentation.HomeActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.regex.Pattern
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
class CitizenRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenRegistrationBinding
    private var registrationDialog: Dialog? = null
    private val authRepository by lazy {
        FirebaseAuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCitizenRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.legalAcceptanceCheckbox.buttonTintList = null

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
        binding.legalAcceptanceCheckbox.setOnCheckedChangeListener { _, checked ->
            if (checked) clearLegalAcceptanceError()
        }
        configureLegalLinks()
        configureCheckboxTouchTarget(binding.legalAcceptanceCheckbox)
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
                withTimeout(REGISTRATION_TIMEOUT_MS) {
                    authRepository.register(fullName, cpf, email, password)
                }
            }.onSuccess {
                startActivity(Intent(this@CitizenRegistrationActivity, HomeActivity::class.java))
                finishAffinity()
            }.onFailure { error ->
                setLoading(false)
                showRegistrationError(error)
            }
        }
    }

    private fun showRegistrationError(error: Throwable) {
        val messageRes = registrationErrorMessage(error)
        registrationDialog?.dismiss()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogAuthMessageBinding.inflate(layoutInflater)
        dialogBinding.dialogAuthTitle.setText(R.string.register_error_title)
        dialogBinding.dialogAuthMessage.setText(messageRes)
        dialogBinding.dialogAuthButton.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            if (registrationDialog === dialog) registrationDialog = null
        }
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.86f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { dimAmount = 0.58f }
            }
        }
        registrationDialog = dialog
        dialog.show()
    }
    private fun registrationErrorMessage(error: Throwable): Int {
        Log.e(TAG, "Citizen registration failed", error)
        val authCode = (error as? FirebaseAuthException)?.errorCode
        val firestoreCode = (error as? FirebaseFirestoreException)?.code

        return when {
            error is FirebaseAuthUserCollisionException ||
                authCode == "ERROR_EMAIL_ALREADY_IN_USE" ->
                R.string.register_email_exists
            error is FirebaseAuthWeakPasswordException ||
                authCode == "ERROR_WEAK_PASSWORD" ->
                R.string.register_password_invalid
            error is TimeoutCancellationException ->
                R.string.register_timeout
            error is FirebaseNetworkException ||
                authCode == "ERROR_NETWORK_REQUEST_FAILED" ->
                R.string.login_network_error
            authCode == "ERROR_OPERATION_NOT_ALLOWED" ->
                R.string.register_auth_disabled
            firestoreCode == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                R.string.register_database_error
            else -> R.string.register_error_generic
        }
    }
    private fun showFieldError(field: android.widget.EditText, messageRes: Int) {
        field.error = getString(messageRes)
        field.requestFocus()
    }

    private fun showLegalAcceptanceError() {
        binding.legalAcceptanceCheckbox.background =
            getDrawable(R.drawable.bg_legal_acceptance_error)
        binding.legalAcceptanceCheckbox.buttonDrawable =
            getDrawable(R.drawable.checkbox_legal_error_selector)
        binding.legalAcceptanceError.visibility = View.VISIBLE
        Snackbar.make(binding.root, R.string.legal_acceptance_required, Snackbar.LENGTH_LONG).show()
    }

    private fun clearLegalAcceptanceError() {
        binding.legalAcceptanceCheckbox.background =
            getDrawable(R.drawable.bg_legal_acceptance)
        binding.legalAcceptanceCheckbox.buttonDrawable =
            getDrawable(R.drawable.checkbox_donation_selector)
        binding.legalAcceptanceError.visibility = View.GONE
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


    private fun configureCheckboxTouchTarget(checkBox: android.widget.CompoundButton) {
        checkBox.isClickable = false
        val density = resources.displayMetrics.density

        checkBox.setOnTouchListener { _, event ->
            val visibleRect = android.graphics.Rect()
            checkBox.getGlobalVisibleRect(visibleRect)
            val iconLeft = visibleRect.left + (12 * density).toInt()
            val iconRight = visibleRect.left + (64 * density).toInt()
            val insideIcon = event.rawX >= iconLeft && event.rawX <= iconRight

            if (insideIcon) {
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> true
                    android.view.MotionEvent.ACTION_UP -> {
                        checkBox.isChecked = !checkBox.isChecked
                        true
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> true
                    else -> true
                }
            } else {
                !isTouchOnLegalLink(checkBox, event)
            }
        }
    }

    private fun isTouchOnLegalLink(
        checkBox: android.widget.CompoundButton,
        event: android.view.MotionEvent
    ): Boolean {
        val text = checkBox.text
        val textLayout = checkBox.layout
        if (text !is Spanned || textLayout == null) return false

        val textX = event.x - checkBox.totalPaddingLeft + checkBox.scrollX
        val textY = event.y - checkBox.totalPaddingTop + checkBox.scrollY
        if (textX < 0 || textY < 0 || textY >= textLayout.height) return false

        val line = textLayout.getLineForVertical(textY.toInt())
        val offset = textLayout.getOffsetForHorizontal(line, textX)
        val spanEnd = minOf(offset + 1, text.length)
        return text.getSpans(offset, spanEnd, ClickableSpan::class.java).isNotEmpty()
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
        val fullText = getString(R.string.legal_acceptance_checkbox)
        val termsLabel = getString(R.string.legal_terms_label)
        val privacyLabel = getString(R.string.legal_privacy_label)
        val styledText = SpannableString(fullText)

        addLegalLink(styledText, termsLabel) {
            openLegalDocument(LegalDocumentActivity.DOCUMENT_TERMS)
        }
        addLegalLink(styledText, privacyLabel) {
            openLegalDocument(LegalDocumentActivity.DOCUMENT_PRIVACY)
        }

        binding.legalAcceptanceCheckbox.text = styledText
        binding.legalAcceptanceCheckbox.movementMethod = LinkMovementMethod.getInstance()
        binding.legalAcceptanceCheckbox.highlightColor = android.graphics.Color.TRANSPARENT
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
                    drawState.isFakeBoldText = true
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
        binding.registerHeader.backButton.isEnabled = !loading
        binding.registerNameInput.isEnabled = !loading
        binding.registerCpfInput.isEnabled = !loading
        binding.registerEmailInput.isEnabled = !loading
        binding.registerPasswordInput.isEnabled = !loading
        binding.legalAcceptanceCheckbox.isEnabled = !loading
        binding.registerButton.setText(
            if (loading) R.string.register_loading else R.string.register_button
        )
        binding.registerLoadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.registerLoadingSilhouette.startAnimation()
        } else {
            binding.registerLoadingSilhouette.stopAnimation()
        }
    }
    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private companion object {
        private const val TAG = "CitizenRegistration"
        private const val REGISTRATION_TIMEOUT_MS = 20_000L
        const val CPF_LENGTH = 11
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
