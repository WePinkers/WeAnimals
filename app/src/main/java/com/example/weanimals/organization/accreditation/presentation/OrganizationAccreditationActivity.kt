package com.example.weanimals.organization.accreditation.presentation

import android.content.Intent
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.formatting.BrazilianDocumentMask
import com.example.weanimals.databinding.DialogAuthMessageBinding
import com.example.weanimals.databinding.ActivityOrganizationAccreditationBinding
import com.example.weanimals.organization.accreditation.OrganizationAccreditationDraft
import com.example.weanimals.organization.accreditation.repository.FirebaseOrganizationAccreditationRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.regex.Pattern
import android.view.Window
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import com.example.weanimals.entry.presentation.LegalDocumentActivity
import com.example.weanimals.entry.presentation.LoginActivity

class OrganizationAccreditationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizationAccreditationBinding
    private var errorDialog: Dialog? = null
    private val repository by lazy {
        FirebaseOrganizationAccreditationRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }
    private var selectedDocumentUri: Uri? = null
    private var selectedDocumentName: String = ""
    private var selectedDocumentSizeBytes: Long = 0L
    private var selectedDocumentContentType: String = ""
    private var isPasswordVisible = false

    private val documentPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val contentType = contentResolver.getType(uri).orEmpty()
        val size = documentSize(uri)
        if (!isAllowedDocument(contentType) || size > MAX_DOCUMENT_BYTES) {
            showErrorMessage(R.string.accreditation_file_error)
            return@registerForActivityResult
        }

        selectedDocumentUri = uri
        selectedDocumentName = documentName(uri)
        selectedDocumentSizeBytes = size
        selectedDocumentContentType = contentType
        binding.accreditationDocumentAction.text = selectedDocumentName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOrganizationAccreditationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.accreditationLegalAcceptanceCheckbox.buttonTintList = null

        binding.accreditationHeader.headerTitle.setText(R.string.accreditation_title)
        binding.accreditationHeader.backButton.setOnClickListener { leaveAccreditation() }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leaveAccreditation()
                }
            }
        )
        setRequiredLabel(binding.accreditationCnpjLabel, R.string.accreditation_cnpj_label)
        setRequiredLabel(binding.accreditationLegalNameLabel, R.string.accreditation_legal_name_label)
        setRequiredLabel(binding.accreditationEmailLabel, R.string.accreditation_email_label)
        setRequiredLabel(binding.accreditationPasswordLabel, R.string.accreditation_password_label)
        setRequiredLabel(binding.accreditationDocumentLabel, R.string.accreditation_document_label)
        setRequiredLabel(binding.accreditationPixLabel, R.string.accreditation_pix_label)
        BrazilianDocumentMask.attachCnpj(binding.accreditationCnpjInput)
        binding.accreditationDocumentDropzone.setOnClickListener {
            documentPicker.launch(arrayOf("application/pdf", "image/*"))
        }
        binding.accreditationSubmitButton.setOnClickListener { submit() }
        binding.accreditationPasswordToggleButton.setOnClickListener {
            togglePasswordVisibility()
        }
        binding.accreditationLegalAcceptanceCheckbox.setOnCheckedChangeListener { _, checked ->
            if (checked) clearLegalAcceptanceError()
        }
        configureLegalLinks()
        configureCheckboxTouchTarget(binding.accreditationLegalAcceptanceCheckbox)
        restorePendingDraft()
    }

    private fun leaveAccreditation() {
        if (binding.accreditationLoadingOverlay.visibility == View.VISIBLE) return
        if (!isTaskRoot) {
            finish()
            return
        }

        startActivity(LoginActivity.newOrganizationIntent(this))
        finish()
    }

    private fun togglePasswordVisibility() {
        val selection = binding.accreditationPasswordInput.selectionStart
        isPasswordVisible = !isPasswordVisible
        binding.accreditationPasswordInput.transformationMethod = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        binding.accreditationPasswordToggleButton.setImageResource(
            if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )
        binding.accreditationPasswordToggleButton.contentDescription = getString(
            if (isPasswordVisible) R.string.login_hide_password else R.string.login_show_password
        )
        binding.accreditationPasswordInput.setSelection(selection.coerceAtLeast(0))
    }

    private fun restorePendingDraft() {
        val draft = OrganizationAccreditationDraft.load(this) ?: return
        binding.accreditationCnpjInput.setText(draft.cnpj)
        binding.accreditationLegalNameInput.setText(draft.legalName)
        binding.accreditationEmailInput.setText(draft.email)
        binding.accreditationPixInput.setText(draft.pixKey)
        selectedDocumentUri = Uri.EMPTY
        selectedDocumentName = draft.documentName
        selectedDocumentSizeBytes = draft.documentSizeBytes
        selectedDocumentContentType = draft.documentContentType
        if (draft.documentName.isNotBlank()) {
            binding.accreditationDocumentAction.text = draft.documentName
        }
    }

    private fun submit() {
        val cnpj = binding.accreditationCnpjInput.text?.toString().orEmpty().trim()
        val legalName = binding.accreditationLegalNameInput.text?.toString().orEmpty().trim()
        val institutionalEmail = binding.accreditationEmailInput.text?.toString().orEmpty().trim().lowercase()
        val password = binding.accreditationPasswordInput.text?.toString().orEmpty()
        val pixKey = binding.accreditationPixInput.text?.toString().orEmpty().trim()

        binding.accreditationCnpjInput.error = null
        binding.accreditationLegalNameInput.error = null
        binding.accreditationEmailInput.error = null
        binding.accreditationPasswordInput.error = null
        binding.accreditationPixInput.error = null

        when {
            !isValidCnpj(cnpj) -> {
                showFieldError(binding.accreditationCnpjInput, R.string.accreditation_cnpj_required)
            }
            legalName.isBlank() -> {
                showFieldError(binding.accreditationLegalNameInput, R.string.accreditation_name_required)
            }
            !EMAIL_PATTERN.matcher(institutionalEmail).matches() -> {
                showFieldError(binding.accreditationEmailInput, R.string.accreditation_email_required)
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                showFieldError(binding.accreditationPasswordInput, R.string.accreditation_password_required)
            }
            selectedDocumentUri == null -> {
                binding.accreditationDocumentDropzone.requestFocus()
                showErrorMessage(R.string.accreditation_document_required)
            }
            pixKey.isBlank() -> {
                showFieldError(binding.accreditationPixInput, R.string.accreditation_pix_required)
            }
            !isValidPixKey(pixKey) -> {
                showFieldError(binding.accreditationPixInput, R.string.accreditation_pix_invalid)
            }
            !binding.accreditationLegalAcceptanceCheckbox.isChecked -> {
                showLegalAcceptanceError()
            }
            else -> sendRequest(cnpj, legalName, institutionalEmail, password, pixKey)
        }
    }

    private fun sendRequest(
        cnpj: String,
        legalName: String,
        institutionalEmail: String,
        password: String,
        pixKey: String
    ) {
        if (selectedDocumentUri == null) return
        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                withTimeout(SUBMIT_TIMEOUT_MS) {
                    repository.submit(
                        cnpj = cnpj,
                        legalName = legalName,
                        institutionalEmail = institutionalEmail,
                        password = password,
                        documentName = selectedDocumentName,
                        documentSizeBytes = selectedDocumentSizeBytes,
                        documentContentType = selectedDocumentContentType,
                        pixKey = pixKey
                    )
                }
            }.onSuccess {
                OrganizationAccreditationDraft.clear(this@OrganizationAccreditationActivity)
                startActivity(
                    Intent(
                        this@OrganizationAccreditationActivity,
                        OrganizationVerificationPendingActivity::class.java
                    )
                )
                finish()
            }.onFailure { error ->
                setLoading(false)
                showSubmitError(error)
            }
        }
    }

    private fun showFieldError(field: EditText, messageRes: Int) {
        field.error = getString(messageRes)
        field.requestFocus()
        showErrorMessage(messageRes)
    }

    private fun setLoading(loading: Boolean) {
        binding.accreditationSubmitButton.isEnabled = !loading
        binding.accreditationHeader.backButton.isEnabled = !loading
        binding.accreditationCnpjInput.isEnabled = !loading
        binding.accreditationLegalNameInput.isEnabled = !loading
        binding.accreditationDocumentDropzone.isEnabled = !loading
        binding.accreditationEmailInput.isEnabled = !loading
        binding.accreditationPasswordInput.isEnabled = !loading
        binding.accreditationPasswordToggleButton.isEnabled = !loading
        binding.accreditationPixInput.isEnabled = !loading
        binding.accreditationLegalAcceptanceCheckbox.isEnabled = !loading
        binding.accreditationSubmitButton.setText(
            if (loading) R.string.accreditation_submitting else R.string.accreditation_submit
        )
        binding.accreditationLoadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.accreditationLoadingSilhouette.startAnimation()
        } else {
            binding.accreditationLoadingSilhouette.stopAnimation()
        }
    }

    private fun showSubmitError(error: Throwable) {
        if (error is FirebaseOrganizationAccreditationRepository.EmailVerificationRequiredException) {
            savePendingDraft()
            showErrorMessage(
                R.string.accreditation_email_verification_required,
                R.string.accreditation_email_verification_title
            )
            return
        }
        val authCode = (error as? FirebaseAuthException)?.errorCode
        val firestoreCode = (error as? FirebaseFirestoreException)?.code
        val message = when {
            error is FirebaseAuthUserCollisionException || authCode == "ERROR_EMAIL_ALREADY_IN_USE" ->
                R.string.accreditation_email_exists
            error is FirebaseAuthInvalidCredentialsException ||
                authCode == "ERROR_INVALID_EMAIL" ||
                authCode == "ERROR_INVALID_CREDENTIAL" ->
                R.string.accreditation_email_invalid
            error is FirebaseAuthWeakPasswordException || authCode == "ERROR_WEAK_PASSWORD" ->
                R.string.accreditation_weak_password
            error is TimeoutCancellationException -> R.string.accreditation_timeout
            error is FirebaseNetworkException || authCode == "ERROR_NETWORK_REQUEST_FAILED" ->
                R.string.accreditation_network_error
            firestoreCode == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                R.string.accreditation_rules_error
            else -> R.string.accreditation_submit_error
        }
        showErrorMessage(message)
    }

    private fun savePendingDraft() {
        OrganizationAccreditationDraft.save(
            this,
            OrganizationAccreditationDraft.Values(
                cnpj = binding.accreditationCnpjInput.text?.toString().orEmpty().trim(),
                legalName = binding.accreditationLegalNameInput.text?.toString().orEmpty().trim(),
                email = binding.accreditationEmailInput.text?.toString().orEmpty().trim().lowercase(),
                documentName = selectedDocumentName,
                documentSizeBytes = selectedDocumentSizeBytes,
                documentContentType = selectedDocumentContentType,
                pixKey = binding.accreditationPixInput.text?.toString().orEmpty().trim()
            )
        )
    }

    private fun showErrorMessage(
        messageRes: Int,
        titleRes: Int = R.string.accreditation_error_title
    ) {
        errorDialog?.dismiss()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogAuthMessageBinding.inflate(layoutInflater)
        dialogBinding.dialogAuthTitle.setText(titleRes)
        dialogBinding.dialogAuthMessage.setText(messageRes)
        dialogBinding.dialogAuthButton.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            if (errorDialog === dialog) errorDialog = null
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
        errorDialog = dialog
        dialog.show()
    }

    private fun showLegalAcceptanceError() {
        binding.accreditationLegalAcceptanceCheckbox.background =
            getDrawable(R.drawable.bg_legal_acceptance_error)
        binding.accreditationLegalAcceptanceCheckbox.buttonDrawable =
            getDrawable(R.drawable.checkbox_legal_error_selector)
        binding.accreditationLegalAcceptanceError.visibility = View.VISIBLE
        showErrorMessage(R.string.legal_acceptance_required)
    }

    private fun clearLegalAcceptanceError() {
        binding.accreditationLegalAcceptanceCheckbox.background =
            getDrawable(R.drawable.bg_legal_acceptance)
        binding.accreditationLegalAcceptanceCheckbox.buttonDrawable =
            getDrawable(R.drawable.checkbox_donation_selector)
        binding.accreditationLegalAcceptanceError.visibility = View.GONE
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

        binding.accreditationLegalAcceptanceCheckbox.text = styledText
        binding.accreditationLegalAcceptanceCheckbox.movementMethod = LinkMovementMethod.getInstance()
        binding.accreditationLegalAcceptanceCheckbox.highlightColor = Color.TRANSPARENT
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

    private fun documentName(uri: Uri): String {
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        return name?.takeIf(String::isNotBlank) ?: getString(R.string.accreditation_document_selected)
    }

    private fun documentSize(uri: Uri): Long {
        return contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else 0L
            } ?: 0L
    }

    private fun isValidPixKey(value: String): Boolean {
        val key = value.trim()
        if (key.isEmpty() || key.length > 180) return false

        val isEmail = EMAIL_PATTERN.matcher(key).matches()
        val isPhone = key.matches(Regex("^\\+55\\d{10,11}$"))
        val normalizedDocument = key.filter(Char::isDigit)
        val isCpfOrCnpj = normalizedDocument.length in setOf(11, 14)
            && key.all { it.isDigit() || it in ".-/ " }
        val isRandomKey = key.matches(
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        )

        return isEmail || isPhone || isCpfOrCnpj || isRandomKey
    }

    private fun isAllowedDocument(contentType: String): Boolean =
        contentType == "application/pdf" || contentType in setOf(
            "image/jpeg", "image/png", "image/webp"
        )

    private fun isValidCnpj(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        if (digits.length != CNPJ_LENGTH || digits.all { it == digits.first() }) return false

        val firstDigit = calculateCnpjDigit(
            digits.take(12),
            intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        )
        val secondDigit = calculateCnpjDigit(
            digits.take(13),
            intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        )
        return digits[12].digitToInt() == firstDigit && digits[13].digitToInt() == secondDigit
    }

    private fun calculateCnpjDigit(value: String, weights: IntArray): Int {
        val sum = value.mapIndexed { index, digit ->
            digit.digitToInt() * weights[index]
        }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private companion object {
        const val CNPJ_LENGTH = 14
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_DOCUMENT_BYTES = 10L * 1024L * 1024L
        const val SUBMIT_TIMEOUT_MS = 30_000L
        val EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
