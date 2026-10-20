package com.example.weanimals.profile.overview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AlterarSenhaResult {
    object Sucesso : AlterarSenhaResult()
    object SenhaAtualIncorreta : AlterarSenhaResult()
    object MuitasTentativas : AlterarSenhaResult()
    object SemConexao : AlterarSenhaResult()
    object ErroGenerico : AlterarSenhaResult()
}

data class AlterarSenhaUiState(
    val isLoading: Boolean = false,
    val erroSenhaAtual: String? = null,
    val erroGeral: String? = null,
    val sucesso: Boolean = false,
    val mensagemSnackbar: String? = null
)

class AlterarSenhaViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlterarSenhaUiState())
    val uiState: StateFlow<AlterarSenhaUiState> = _uiState.asStateFlow()

    fun alterarSenha(senhaAtual: String, novaSenha: String) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrBlank()) {
            _uiState.update { it.copy(erroGeral = "Não foi possível alterar a senha. Tente de novo.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, erroSenhaAtual = null, erroGeral = null) }

        viewModelScope.launch {
            val resultado = executeAlterarSenha(email, senhaAtual, novaSenha)
            when (resultado) {
                AlterarSenhaResult.Sucesso -> {
                    _uiState.update { it.copy(isLoading = false, sucesso = true) }
                }
                AlterarSenhaResult.SenhaAtualIncorreta -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            erroSenhaAtual = "Senha atual incorreta."
                        )
                    }
                }
                AlterarSenhaResult.MuitasTentativas -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            erroGeral = "Muitas tentativas. Tente de novo em alguns minutos."
                        )
                    }
                }
                AlterarSenhaResult.SemConexao -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            erroGeral = "Sem conexão. Verifique sua internet e tente de novo."
                        )
                    }
                }
                AlterarSenhaResult.ErroGenerico -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            erroGeral = "Não foi possível alterar a senha. Tente de novo."
                        )
                    }
                }
            }
        }
    }

    private suspend fun executeAlterarSenha(
        email: String,
        senhaAtual: String,
        novaSenha: String
    ): AlterarSenhaResult {
        return try {
            val user = auth.currentUser ?: return AlterarSenhaResult.ErroGenerico
            val credential = EmailAuthProvider.getCredential(email, senhaAtual)
            user.reauthenticate(credential).await()
            user.updatePassword(novaSenha).await()
            AlterarSenhaResult.Sucesso
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AlterarSenhaResult.SenhaAtualIncorreta
        } catch (e: FirebaseTooManyRequestsException) {
            AlterarSenhaResult.MuitasTentativas
        } catch (e: FirebaseNetworkException) {
            AlterarSenhaResult.SemConexao
        } catch (e: Exception) {
            AlterarSenhaResult.ErroGenerico
        }
    }

    fun enviarEmailRedefinicao() {
        val user = auth.currentUser
        val email = user?.email

        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(erroGeral = "Não foi possível identificar seu e-mail.") }
            return
        }

        viewModelScope.launch {
            try {
                auth.setLanguageCode("pt-BR")
                auth.sendPasswordResetEmail(email).await()
                _uiState.update {
                    it.copy(mensagemSnackbar = "Enviamos um link de redefinição para o seu e-mail.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(erroGeral = "Não foi possível enviar o e-mail de redefinição.")
                }
            }
        }
    }

    fun mensagemSnackbarExibida() {
        _uiState.update { it.copy(mensagemSnackbar = null, erroGeral = null) }
    }
}
