package com.example.weanimals.profile.overview.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var exibeSucesso by remember { mutableStateOf(false) }

            if (exibeSucesso) {
                SenhaAlteradaScreen(
                    onVoltarPerfilClick = { finish() }
                )
            } else {
                AlterarSenhaScreen(
                    onBackClick = { finish() },
                    onSenhaAlterada = { exibeSucesso = true }
                )
            }
        }
    }
}
