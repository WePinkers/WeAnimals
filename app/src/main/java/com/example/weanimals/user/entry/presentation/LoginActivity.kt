package com.example.weanimals.user.entry.presentation

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.session.SessionAccountType
import com.example.weanimals.databinding.DialogAuthMessageBinding
import com.example.weanimals.user.entry.auth.repository.FirebaseAuthRepository
import com.example.weanimals.user.entry.auth.security.LocalLoginAttemptLock
import com.example.weanimals.databinding.ActivityLoginBinding
import com.example.weanimals.user.home.presentation.HomeActivity
import com.example.weanimals.organization.accreditation.OrganizationAccreditationDraft
import com.example.weanimals.organization.accreditation.presentation.OrganizationAccreditationActivity
import com.example.weanimals.organization.accreditation.presentation.OrganizationLoginBlockedActivity
import com.example.weanimals.organization.dashboard.presentation.OrganizationReportsActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var authDialog: Dialog? = null
    private var isOrganization = false
    private val authRepository by lazy {
        FirebaseAuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }
    private val localLoginAttemptLock by lazy { LocalLoginAttemptLock(this) }
    private var isPasswordVisible = false
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginHeader.backButton.setOnClickListener { finish() }
        binding.loginOrganizationTab.setOnClickListener {
            selectAccountType(isOrganization = true)
        }
        binding.loginCitizenTab.setOnClickListener {
            selectAccountType(isOrganization = false)
        }
        binding.loginButton.setOnClickListener {
            signIn()
        }
        binding.googleLoginButton.setOnClickListener {
            startGoogleSignIn()
        }
        binding.passwordToggleButton.setOnClickListener { togglePasswordVisibility() }
        binding.forgotPasswordLink.paintFlags =
            binding.forgotPasswordLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.forgotPasswordLink.setOnClickListener {
            startActivity(PasswordRecoveryActivity.newIntent(this, isOrganization))
        }
        binding.loginCreateAccountLink.paintFlags =
            binding.loginCreateAccountLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.loginCreateAccountLink.setOnClickListener {
            startActivity(Intent(this, CitizenRegistrationActivity::class.java))
        }
        binding.loginOrganizationRequestLink.paintFlags =
            binding.loginOrganizationRequestLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.loginOrganizationRequestLink.setOnClickListener {
            startActivity(Intent(this, OrganizationAccreditationActivity::class.java))
        }
        binding.loginHeader.headerTitle.setText(R.string.login_title)

        selectAccountType(
            isOrganization = intent.getBooleanExtra(EXTRA_ORGANIZATION, false)
        )

        if (intent.getBooleanExtra(EXTRA_PASSWORD_RESET_SUCCESS, false)) {
            intent.removeExtra(EXTRA_PASSWORD_RESET_SUCCESS)
            showMessage(R.string.login_password_reset_success)
        }
    }

    private fun selectAccountType(isOrganization: Boolean) {
        this.isOrganization = isOrganization
        binding.loginCitizenTab.setBackgroundResource(
            if (isOrganization) R.drawable.bg_entry_segment_unselected
            else R.drawable.bg_entry_segment_selected
        )
        binding.loginOrganizationTab.setBackgroundResource(
            if (isOrganization) R.drawable.bg_entry_segment_selected
            else R.drawable.bg_entry_segment_unselected
        )
        binding.loginHeader.headerTitle.setText(
            if (isOrganization) R.string.login_organization_title else R.string.login_title
        )
        setRequiredLabel(
            binding.loginIdentifierLabel,
            if (isOrganization) {
                R.string.login_organization_identifier_label
            } else {
                R.string.login_email_label
            }
        )
        binding.loginEmailInput.setHint(
            if (isOrganization) {
                R.string.login_organization_identifier_hint
            } else {
                R.string.login_email_hint
            }
        )
        setRequiredLabel(binding.loginPasswordLabel, R.string.login_password_label)
        binding.loginPasswordInput.setHint(
            if (isOrganization) {
                R.string.login_organization_password_hint
            } else {
                R.string.login_password_hint
            }
        )
        binding.loginButton.setText(
            if (isOrganization) R.string.login_organization_button else R.string.login_button
        )
        binding.loginCitizenOptions.visibility = if (isOrganization) View.GONE else View.VISIBLE
        binding.loginOrganizationOptions.visibility = if (isOrganization) View.VISIBLE else View.GONE
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

    private fun signIn() {
        val identifier = binding.loginEmailInput.text?.toString().orEmpty().trim()
        val password = binding.loginPasswordInput.text?.toString().orEmpty()

        binding.loginEmailInput.error = null
        binding.loginPasswordInput.error = null

        when {
            identifier.isBlank() -> {
                binding.loginEmailInput.error = getString(R.string.login_identifier_required)
                binding.loginEmailInput.requestFocus()
                return
            }
            password.isBlank() -> {
                binding.loginPasswordInput.error = getString(R.string.login_password_required)
                binding.loginPasswordInput.requestFocus()
                return
            }
        }

        val lockIdentifier = loginAttemptIdentifier(identifier)
        val existingLock = localLoginAttemptLock.check(lockIdentifier)
        if (existingLock.isLocked) {
            showLocalLockMessage(existingLock)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                withTimeout(AUTH_TIMEOUT_MS) {
                    if (isOrganization) {
                        authRepository.signInOrganization(
                            identifier = identifier,
                            password = password,
                            pendingEmail = pendingOrganizationEmail(identifier)
                        )
                    } else {
                        authRepository.signIn(identifier, password)
                    }
                }
            }.onSuccess {
                localLoginAttemptLock.clear(lockIdentifier)
                if (isOrganization) {
                    SessionAccountType.rememberOrganization(this@LoginActivity)
                    if (hasLocalPendingAccreditation(identifier)
                        && !FirebaseAuthRepository.isTemporaryOrganizationTestAccess(identifier, this@LoginActivity)
                    ) {
                        openPendingAccreditation()
                    } else {
                        openOrganizationPanel()
                    }
                } else {
                    SessionAccountType.rememberCitizen(this@LoginActivity)
                    openApp()
                }
            }.onFailure { error ->
                setLoading(false)
                if (isOrganization && error is FirebaseAuthRepository.OrganizationAccreditationPendingException) {
                    openOrganizationPending()
                    return@onFailure
                }
                if (isCredentialFailure(error)) {
                    val lockState = localLoginAttemptLock.registerFailure(lockIdentifier)
                    if (lockState.isLocked) {
                        showLocalLockMessage(lockState)
                        return@onFailure
                    }
                }
                showAuthError(error)
            }
        }
    }

    private fun loginAttemptIdentifier(identifier: String): String {
        val normalized = if (identifier.contains('@')) {
            identifier.lowercase()
        } else {
            identifier.filter(Char::isDigit)
        }
        val accountType = if (isOrganization) "organization" else "citizen"
        return "$accountType:$normalized"
    }

    private fun isCredentialFailure(error: Throwable): Boolean {
        val firebaseCode = (error as? FirebaseAuthException)?.errorCode
        return error is FirebaseAuthInvalidCredentialsException ||
            firebaseCode == "ERROR_WRONG_PASSWORD" ||
            firebaseCode == "ERROR_INVALID_CREDENTIAL" ||
            firebaseCode == "ERROR_INVALID_LOGIN_CREDENTIALS"
    }

    private fun showLocalLockMessage(lockState: LocalLoginAttemptLock.LockState) {
        showMessage(
            R.string.login_local_lock_title,
            getString(R.string.login_local_lock_message, lockState.remainingMinutes)
        )
    }
    private fun pendingOrganizationEmail(identifier: String): String? {
        val normalizedIdentifier = identifier.filter(Char::isDigit)
        if (normalizedIdentifier.length != ORGANIZATION_CNPJ_LENGTH) return null

        return OrganizationAccreditationDraft.load(this)
            ?.takeIf { it.cnpj.filter(Char::isDigit) == normalizedIdentifier }
            ?.email
    }

    private fun hasLocalPendingAccreditation(identifier: String): Boolean {
        val draft = OrganizationAccreditationDraft.load(this) ?: return false
        return if (identifier.contains('@')) {
            draft.email.equals(identifier.trim(), ignoreCase = true)
        } else {
            draft.cnpj.filter(Char::isDigit) == identifier.filter(Char::isDigit)
        }
    }

    private fun openPendingAccreditation() {
        startActivity(
            Intent(this, OrganizationAccreditationActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }

    private fun startGoogleSignIn() {
        val webClientIdRes = resources.getIdentifier(
            "default_web_client_id",
            "string",
            packageName
        )
        if (webClientIdRes == 0) {
            showMessage(R.string.login_google_not_configured)
            return
        }

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(webClientIdRes))
            .requestEmail()
            .build()
        setLoading(true)
        googleSignInLauncher.launch(GoogleSignIn.getClient(this, options).signInIntent)
    }

    private fun handleGoogleResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            setLoading(false)
            return
        }

        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
        } catch (error: ApiException) {
            setLoading(false)
            val message = when (error.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR -> R.string.login_google_config_error
                CommonStatusCodes.NETWORK_ERROR -> R.string.login_network_error
                else -> R.string.login_google_error
            }
            Log.e(TAG, "Google sign-in failed with status ${error.statusCode}", error)
            showMessage(message)
            return
        } catch (error: Exception) {
            setLoading(false)
            Log.e(TAG, "Google account result could not be read", error)
            showMessage(R.string.login_google_error)
            return
        }
        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            setLoading(false)
            showMessage(R.string.login_google_error)
            return
        }

        lifecycleScope.launch {
            runCatching {
                withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signInWithGoogle(idToken)
                }
            }.onSuccess {
                SessionAccountType.rememberCitizen(this@LoginActivity)
                openApp()
            }.onFailure { error ->
                setLoading(false)
                showAuthError(error, googleFlow = true)
            }
        }
    }

    private fun togglePasswordVisibility() {
        val selection = binding.loginPasswordInput.selectionStart
        isPasswordVisible = !isPasswordVisible
        binding.loginPasswordInput.transformationMethod = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        binding.passwordToggleButton.setImageResource(
            if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )
        binding.passwordToggleButton.contentDescription = getString(
            if (isPasswordVisible) R.string.login_hide_password else R.string.login_show_password
        )
        binding.loginPasswordInput.setSelection(selection.coerceAtLeast(0))
    }

    private fun setLoading(loading: Boolean) {
        binding.loginButton.isEnabled = !loading
        binding.googleLoginButton.isEnabled = !loading
        binding.loginEmailInput.isEnabled = !loading
        binding.loginPasswordInput.isEnabled = !loading
        binding.loginButton.setText(
            when {
                loading -> R.string.login_loading
                isOrganization -> R.string.login_organization_button
                else -> R.string.login_button
            }
        )
        binding.loginLoadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.loginLoadingSilhouette.startAnimation()
        } else {
            binding.loginLoadingSilhouette.stopAnimation()
        }
    }

    private fun showAuthError(error: Throwable, googleFlow: Boolean = false) {
        Log.e(TAG, "Authentication failed", error)
        val firebaseCode = (error as? FirebaseAuthException)?.errorCode
        val firestoreCode = (error as? FirebaseFirestoreException)?.code
        val message = when {
            error is FirebaseAuthRepository.AccountNotFoundException -> R.string.login_cpf_not_found
            error is FirebaseAuthRepository.InvalidCpfException -> R.string.login_invalid_identifier
            error is FirebaseAuthRepository.OrganizationAccountNotFoundException ->
                R.string.login_organization_not_found
            error is FirebaseAuthRepository.OrganizationEmailVerificationRequiredException ->
                R.string.login_organization_email_not_verified
            error is FirebaseAuthRepository.InvalidOrganizationIdentifierException ->
                R.string.login_organization_invalid_identifier
            error is kotlinx.coroutines.TimeoutCancellationException -> R.string.login_timeout
            error is FirebaseNetworkException || firebaseCode == "ERROR_NETWORK_REQUEST_FAILED" ->
                R.string.login_network_error
            firebaseCode == "ERROR_OPERATION_NOT_ALLOWED" -> R.string.login_provider_disabled
            firestoreCode == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                R.string.login_rules_not_published
            firebaseCode == "ERROR_USER_NOT_FOUND" ||
                firebaseCode == "ERROR_WRONG_PASSWORD" ||
                firebaseCode == "ERROR_INVALID_CREDENTIAL" ||
                firebaseCode == "ERROR_INVALID_LOGIN_CREDENTIALS" ||
                error is FirebaseAuthInvalidCredentialsException ||
                error is FirebaseAuthInvalidUserException ->
                if (googleFlow) R.string.login_google_error else R.string.login_invalid_credentials
            googleFlow -> R.string.login_google_error
            else -> R.string.login_error_generic
        }
        showMessage(message)
    }

    private fun showMessage(messageRes: Int) {
        showMessage(
            getString(R.string.login_error_title),
            getString(messageRes)
        )
    }

    private fun showMessage(titleRes: Int, message: CharSequence) {
        showMessage(getString(titleRes), message)
    }

    private fun showMessage(title: CharSequence, message: CharSequence) {
        authDialog?.dismiss()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogAuthMessageBinding.inflate(layoutInflater)
        dialogBinding.dialogAuthTitle.text = title
        dialogBinding.dialogAuthMessage.text = message
        dialogBinding.dialogAuthButton.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            if (authDialog === dialog) authDialog = null
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
        authDialog = dialog
        dialog.show()
    }
    private fun openApp() {
        startActivity(Intent(this, HomeActivity::class.java))
        finishAffinity()
    }

    private fun openOrganizationPanel() {
        startActivity(
            Intent(this, OrganizationReportsActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }

    private fun openOrganizationPending() {
        startActivity(
            Intent(this, OrganizationLoginBlockedActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val EXTRA_ORGANIZATION = "extra_organization"
        private const val EXTRA_PASSWORD_RESET_SUCCESS = "extra_password_reset_success"
        private const val AUTH_TIMEOUT_MS = 10_000L
        private const val ORGANIZATION_CNPJ_LENGTH = 14

        fun newIntent(context: Context, isOrganization: Boolean): Intent =
            Intent(context, LoginActivity::class.java).putExtra(EXTRA_ORGANIZATION, isOrganization)

        fun newOrganizationIntent(context: Context): Intent =
            newIntent(context, isOrganization = true)

        fun newPasswordResetSuccessIntent(context: Context, isOrganization: Boolean): Intent =
            newIntent(context, isOrganization).putExtra(EXTRA_PASSWORD_RESET_SUCCESS, true)
    }
}
