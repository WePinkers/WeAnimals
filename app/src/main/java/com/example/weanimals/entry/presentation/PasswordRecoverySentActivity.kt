package com.example.weanimals.entry.presentation

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.entry.auth.PasswordResetActionSettings
import com.example.weanimals.databinding.ActivityPasswordRecoverySentBinding
import com.example.weanimals.databinding.DialogAuthMessageBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class PasswordRecoverySentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordRecoverySentBinding
    private var messageDialog: Dialog? = null
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val email: String
        get() = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
    private val isOrganization: Boolean
        get() = intent.getBooleanExtra(EXTRA_ORGANIZATION, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityPasswordRecoverySentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sentHeader.headerTitle.setText(R.string.password_recovery_sent_title)
        binding.sentHeader.backButton.setOnClickListener { openLogin() }
        binding.sentBackToLogin.paintFlags =
            binding.sentBackToLogin.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.sentBackToLogin.setOnClickListener { openLogin() }
        binding.sentResendButton.setOnClickListener { resendRecoveryLink() }
        renderMessage()
    }

    private fun renderMessage() {
        val message = getString(R.string.password_recovery_sent_message, email)
        val styledMessage = SpannableString(message)
        val emailStart = message.indexOf(email)
        if (emailStart >= 0) {
            styledMessage.setSpan(
                StyleSpan(Typeface.BOLD),
                emailStart,
                emailStart + email.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            styledMessage.setSpan(
                ForegroundColorSpan(getColor(R.color.ink)),
                emailStart,
                emailStart + email.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.sentMessage.text = styledMessage
    }

    private fun resendRecoveryLink() {
        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                withTimeout(RECOVERY_TIMEOUT_MS) {
                    auth.setLanguageCode("pt-BR")
                    auth.sendPasswordResetEmail(email, PasswordResetActionSettings.forContext(this@PasswordRecoverySentActivity, isOrganization)).await()
                }
            }.onSuccess {
                setLoading(false)
                showMessage(
                    titleRes = R.string.password_recovery_resent_title,
                    messageRes = R.string.password_recovery_resent_message
                )
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

    private fun setLoading(loading: Boolean) {
        binding.sentResendButton.isEnabled = !loading
        binding.sentHeader.backButton.isEnabled = !loading
        binding.sentBackToLogin.isEnabled = !loading
        binding.sentResendButton.setText(
            if (loading) R.string.password_recovery_resending
            else R.string.password_recovery_resend
        )
        binding.sentLoadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.sentLoadingSilhouette.startAnimation()
        else binding.sentLoadingSilhouette.stopAnimation()
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

    private fun openLogin() {
        startActivity(
            LoginActivity.newIntent(this, isOrganization).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
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
        private const val EXTRA_EMAIL = "extra_email"
        private const val EXTRA_ORGANIZATION = "extra_organization"
        private const val RECOVERY_TIMEOUT_MS = 15_000L

        fun newIntent(context: Context, email: String, isOrganization: Boolean): Intent =
            Intent(context, PasswordRecoverySentActivity::class.java)
                .putExtra(EXTRA_EMAIL, email)
                .putExtra(EXTRA_ORGANIZATION, isOrganization)
    }
}