package com.example.weanimals.user.profile.overview.presentation

import android.text.InputType
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private var isCurrentVisible = false
    private var isNewVisible = false
    private var isConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding.changePasswordHeader.headerTitle.text = "Alterar senha"
        binding.changePasswordHeader.backButton.setOnClickListener { finish() }
        setupPasswordToggles()
        binding.newPasswordInput.doAfterTextChanged { updateRequirements() }
        binding.forgotPasswordLink.setOnClickListener { sendResetEmail() }
        binding.changePasswordButton.setOnClickListener { changePassword() }
        binding.backToProfileButton.setOnClickListener { finish() }
        updateRequirements()
    }

    private fun setupPasswordToggles() {
        binding.currentPasswordToggle.setOnClickListener {
            isCurrentVisible = !isCurrentVisible
            updatePasswordVisibility(binding.currentPasswordInput, binding.currentPasswordToggle, isCurrentVisible)
        }
        binding.newPasswordToggle.setOnClickListener {
            isNewVisible = !isNewVisible
            updatePasswordVisibility(binding.newPasswordInput, binding.newPasswordToggle, isNewVisible)
        }
        binding.confirmPasswordToggle.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            updatePasswordVisibility(binding.confirmPasswordInput, binding.confirmPasswordToggle, isConfirmVisible)
        }
    }

    private fun updatePasswordVisibility(input: EditText, toggle: ImageButton, visible: Boolean) {
        val cursorPosition = input.selectionStart.coerceAtLeast(0)
        input.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        input.setSelection(cursorPosition.coerceAtMost(input.text?.length ?: 0))
        toggle.setImageResource(if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
        toggle.contentDescription = if (visible) "Ocultar senha" else "Mostrar senha"
    }

    private fun updateRequirements() {
        val password = binding.newPasswordInput.text?.toString().orEmpty()
        val length = password.length >= 8
        val uppercase = password.any(Char::isUpperCase)
        val number = password.any(Char::isDigit)
        val symbol = password.any { !it.isLetterOrDigit() }
        val score = listOf(length, uppercase, number, symbol).count { it }

        binding.passwordStrengthText.text = when {
            password.isEmpty() -> ""
            score <= 1 -> "Senha fraca"
            score == 2 -> "Senha razoável"
            score == 3 -> "Senha boa"
            else -> "Senha forte"
        }
        val strengthColor = if (score <= 1) R.color.terracotta else R.color.pine800
        binding.passwordStrengthText.setTextColor(ContextCompat.getColor(this, strengthColor))

        updateRequirement(binding.requirementLength, length, "Pelo menos 8 caracteres")
        updateRequirement(binding.requirementUppercase, uppercase, "Uma letra maiúscula")
        updateRequirement(binding.requirementNumber, number, "Um número")
        updateRequirement(binding.requirementSymbol, symbol, "Um símbolo (opcional)")
    }

    private fun updateRequirement(view: TextView, fulfilled: Boolean, text: String) {
        view.text = "${if (fulfilled) "✓" else "○"}  $text"
        view.setTextColor(
            ContextCompat.getColor(this, if (fulfilled) R.color.pine800 else R.color.ink)
        )
    }

    private fun changePassword() {
        val current = binding.currentPasswordInput.text?.toString().orEmpty()
        val newPassword = binding.newPasswordInput.text?.toString().orEmpty()
        val confirmation = binding.confirmPasswordInput.text?.toString().orEmpty()
        val user = FirebaseAuth.getInstance().currentUser

        binding.currentPasswordInput.error = null
        binding.newPasswordInput.error = null
        binding.confirmPasswordInput.error = null

        var valid = true
        if (current.isBlank()) {
            binding.currentPasswordInput.error = "Digite sua senha atual."
            valid = false
        }
        if (newPassword.length < 8 || newPassword.none(Char::isUpperCase) || newPassword.none(Char::isDigit)) {
            binding.newPasswordInput.error = "Use pelo menos 8 caracteres, uma letra maiúscula e um número."
            valid = false
        } else if (newPassword == current) {
            binding.newPasswordInput.error = "A nova senha precisa ser diferente da atual."
            valid = false
        }
        if (confirmation != newPassword) {
            binding.confirmPasswordInput.error = "As senhas não coincidem."
            valid = false
        }
        if (!valid || user == null) return

        val email = user.email
        if (email.isNullOrBlank()) {
            binding.currentPasswordInput.error = "Esta conta não possui senha para alterar."
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(email, current)).await()
                user.updatePassword(newPassword).await()
                binding.changePasswordContent.visibility = android.view.View.GONE
                binding.changePasswordSuccess.visibility = android.view.View.VISIBLE
            } catch (error: Exception) {
                binding.currentPasswordInput.error = "Não foi possível alterar a senha. Confira sua senha atual."
            } finally {
                setLoading(false)
            }
        }
    }

    private fun sendResetEmail() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrBlank()) {
            binding.currentPasswordInput.error = "Não encontramos um e-mail para esta conta."
            return
        }
        lifecycleScope.launch {
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                Toast.makeText(this@ChangePasswordActivity, "Link de redefinição enviado para $email.", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                binding.currentPasswordInput.error = "Não foi possível enviar o link agora."
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.changePasswordButton.isEnabled = !loading
        binding.changePasswordButton.text = if (loading) "Salvando…" else "Salvar nova senha"
    }
}
