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
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.PatternsCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.user.entry.auth.PasswordResetActionSettings
import com.example.weanimals.databinding.ActivityPasswordRecoveryBinding
import com.example.weanimals.databinding.DialogAuthMessageBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class PasswordRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordRecoveryBinding
    private var messageDialog: Dialog? = null
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val isOrganization: Boolean
        get() = intent.getBooleanExtra(EXTRA_ORGANIZATION, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityPasswordRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recoveryHeader.headerTitle.setText(R.string.password_recovery_title)
        binding.recoveryHeader.backButton.setOnClickListener { openLogin() }
        binding.recoveryBackToLogin.paintFlags =
            binding.recoveryBackToLogin.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.recoveryBackToLogin.setOnClickListener { openLogin() }
        binding.recoverySubmitButton.setOnClickListener { sendRecoveryLink() }
        setRequiredLabel(binding.recoveryEmailLabel, R.string.password_recovery_email_label)
    }

    private fun sendRecoveryLink() {
        val email = binding.recoveryEmailInput.text?.toString().orEmpty().trim().lowercase()
        binding.recoveryEmailInput.error = null

        when {
            email.isBlank() -> showFieldError(
                binding.recoveryEmailInput,
                R.string.password_recovery_email_required
            )
            !PatternsCompat.EMAIL_ADDRESS.matcher(email).matches() -> showFieldError(
                binding.recoveryEmailInput,
                R.string.password_recovery_email_invalid
            )
            else -> submitRecovery(email)
        }
    }

    private fun submitRecovery(email: String) {
        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                withTimeout(RECOVERY_TIMEOUT_MS) {
                    auth.setLanguageCode("pt-BR")
                    auth.sendPasswordResetEmail(email, PasswordResetActionSettings.forContext(this@PasswordRecoveryActivity, isOrganization)).await()
                }
            }.onSuccess {
                setLoading(false)
                startActivity(
                    PasswordRecoverySentActivity.newIntent(
                        context = this@PasswordRecoveryActivity,
                        email = email,
                        isOrganization = isOrganization
                    )
                )
                finish()
            }.onFailure { error ->
                setLoading(false)
                showRecoveryError(error)
            }
        }
    }

    private fun showRecoveryError(error: Throwable) {
        val firebaseCode = (error as? FirebaseAuthException)?.errorCode
        val messageRes = when {
            error is FirebaseNetworkException || firebaseCode == "ERROR_NETWORK_REQUEST_FAILED" ->
                R.string.password_recovery_network_error
            error is TimeoutCancellationException -> R.string.password_recovery_timeout
            else -> R.string.password_recovery_error
        }
        showMessage(
            titleRes = R.string.password_recovery_error_title,
            messageRes = messageRes
        )
    }

    private fun showFieldError(field: EditText, messageRes: Int) {
        field.error = getString(messageRes)
        field.requestFocus()
        showMessage(
            titleRes = R.string.password_recovery_error_title,
            messageRes = messageRes
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.recoverySubmitButton.isEnabled = !loading
        binding.recoveryHeader.backButton.isEnabled = !loading
        binding.recoveryEmailInput.isEnabled = !loading
        binding.recoveryBackToLogin.isEnabled = !loading
        binding.recoverySubmitButton.setText(
            if (loading) R.string.password_recovery_loading
            else R.string.password_recovery_submit
        )
        binding.recoveryLoadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.recoveryLoadingSilhouette.startAnimation()
        else binding.recoveryLoadingSilhouette.stopAnimation()
    }

    private fun showMessage(titleRes: Int, messageRes: Int) {
        messageDialog?.dismiss()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogAuthMessageBinding.inflate(layoutInflater)
        dialogBinding.dialogAuthTitle.setText(titleRes)
        dialogBinding.dialogAuthMessage.setText(messageRes)
        dialogBinding.dialogAuthButton.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            if (messageDialog === dialog) messageDialog = null
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
        messageDialog = dialog
        dialog.show()
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

    private fun openLogin() {
        if (!isTaskRoot) {
            finish()
            return
        }
        startActivity(LoginActivity.newIntent(this, isOrganization))
        finish()
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val EXTRA_ORGANIZATION = "extra_organization"
        private const val RECOVERY_TIMEOUT_MS = 15_000L

        fun newIntent(context: Context, isOrganization: Boolean): Intent =
            Intent(context, PasswordRecoveryActivity::class.java)
                .putExtra(EXTRA_ORGANIZATION, isOrganization)
    }
}
