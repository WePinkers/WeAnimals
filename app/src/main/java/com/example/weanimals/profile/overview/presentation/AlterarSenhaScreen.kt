package com.example.weanimals.profile.overview.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AlterarSenhaScreen(
    viewModel: AlterarSenhaViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onSenhaAlterada: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.erroGeral, uiState.mensagemSnackbar) {
        val mensagem = uiState.erroGeral ?: uiState.mensagemSnackbar
        if (!mensagem.isNullOrBlank()) {
            snackbarHostState.showSnackbar(mensagem)
            viewModel.mensagemSnackbarExibida()
        }
    }

    LaunchedEffect(uiState.sucesso) {
        if (uiState.sucesso) {
            onSenhaAlterada()
        }
    }

    AlterarSenhaContent(
        onBackClick = onBackClick,
        onEsqueciSenhaClick = { viewModel.enviarEmailRedefinicao() },
        onSalvarClick = { senhaAtual, novaSenha ->
            viewModel.alterarSenha(senhaAtual, novaSenha)
        },
        isLoading = uiState.isLoading,
        erroSenhaAtual = uiState.erroSenhaAtual,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlterarSenhaContent(
    onBackClick: () -> Unit = {},
    onEsqueciSenhaClick: () -> Unit = {},
    onSalvarClick: (senhaAtual: String, novaSenha: String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    erroSenhaAtual: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var senhaAtual by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }

    var senhaAtualVisivel by remember { mutableStateOf(false) }
    var novaSenhaVisivel by remember { mutableStateOf(false) }
    var confirmarSenhaVisivel by remember { mutableStateOf(false) }

    var erroSenhaAtualLocal by remember { mutableStateOf<String?>(null) }
    var erroNovaSenha by remember { mutableStateOf<String?>(null) }
    var erroConfirmarSenha by remember { mutableStateOf<String?>(null) }

    // Requisitos da nova senha
    val req8Char = novaSenha.length >= 8
    val reqUpper = novaSenha.any { it.isUpperCase() }
    val reqNumber = novaSenha.any { it.isDigit() }
    val reqSymbol = novaSenha.any { !it.isLetterOrDigit() }

    val isRequisitosCumpridos = req8Char && reqUpper && reqNumber

    // Pontuação de força da senha (0 a 4)
    val score = (if (req8Char) 1 else 0) +
            (if (reqUpper) 1 else 0) +
            (if (reqNumber) 1 else 0) +
            (if (reqSymbol) 1 else 0)

    val corForca = if (score <= 1) Color(0xFFB45C33) else Color(0xFF1F4D2C)
    val textoForca = when {
        novaSenha.isEmpty() -> ""
        score <= 1 -> "Senha fraca"
        score == 2 -> "Senha razoável"
        score == 3 -> "Senha boa"
        else -> "Senha forte"
    }

    val erroSenhaAtualExibido = erroSenhaAtualLocal ?: erroSenhaAtual

    fun validarESalvar() {
        var valido = true

        if (senhaAtual.isBlank()) {
            erroSenhaAtualLocal = "Digite sua senha atual."
            valido = false
        }

        if (!isRequisitosCumpridos) {
            erroNovaSenha = "A nova senha não atende aos requisitos."
            valido = false
        } else if (novaSenha == senhaAtual && senhaAtual.isNotBlank()) {
            erroNovaSenha = "A nova senha precisa ser diferente da atual."
            valido = false
        }

        if (confirmarSenha != novaSenha) {
            erroConfirmarSenha = "As senhas não coincidem."
            valido = false
        }

        if (valido && !isLoading) {
            onSalvarClick(senhaAtual, novaSenha)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF7F2E7),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alterar senha",
                        color = Color(0xFF1A1A18),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color(0xFF1A1A18)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Campo "Senha atual"
            CampoComLabel(
                label = "Senha atual",
                valor = senhaAtual,
                onValorChange = {
                    senhaAtual = it
                    erroSenhaAtualLocal = null
                },
                senhaVisivel = senhaAtualVisivel,
                onToggleVisibilidade = { senhaAtualVisivel = !senhaAtualVisivel },
                erroText = erroSenhaAtualExibido
            )

            // Texto "Esqueci minha senha"
            Text(
                text = "Esqueci minha senha",
                fontSize = 12.sp,
                color = Color(0xFFB45C33),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .clickable { onEsqueciSenhaClick() }
            )

            // 2. Campo "Nova senha"
            CampoComLabel(
                label = "Nova senha",
                valor = novaSenha,
                onValorChange = {
                    novaSenha = it
                    erroNovaSenha = null
                },
                senhaVisivel = novaSenhaVisivel,
                onToggleVisibilidade = { novaSenhaVisivel = !novaSenhaVisivel },
                erroText = erroNovaSenha
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de Força da Senha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..4) {
                    val preenchido = i <= score && novaSenha.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (preenchido) corForca else Color(0xFFD8D3C4))
                    )
                }
            }

            if (textoForca.isNotEmpty()) {
                Text(
                    text = textoForca,
                    fontSize = 12.sp,
                    color = corForca,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Card de Requisitos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFE9D8))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinhaRequisito(cumprido = req8Char, texto = "Pelo menos 8 caracteres")
                    LinhaRequisito(cumprido = reqUpper, texto = "Uma letra maiúscula")
                    LinhaRequisito(cumprido = reqNumber, texto = "Um número")
                    LinhaRequisito(cumprido = reqSymbol, texto = "Um símbolo (opcional)")
                }
            }

            // 3. Campo "Confirmar nova senha"
            CampoComLabel(
                label = "Confirmar nova senha",
                valor = confirmarSenha,
                onValorChange = {
                    confirmarSenha = it
                    erroConfirmarSenha = null
                },
                senhaVisivel = confirmarSenhaVisivel,
                onToggleVisibilidade = { confirmarSenhaVisivel = !confirmarSenhaVisivel },
                erroText = erroConfirmarSenha
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botão "Salvar nova senha"
            Button(
                onClick = { validarESalvar() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB45C33),
                    disabledContainerColor = Color(0xFFB45C33).copy(alpha = 0.7f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFFFFF8ED),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Salvar nova senha",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFF8ED)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CampoComLabel(
    label: String,
    valor: String,
    onValorChange: (String) -> Unit,
    senhaVisivel: Boolean,
    onToggleVisibilidade: () -> Unit,
    erroText: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A18),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = valor,
            onValueChange = onValorChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibilidade) {
                    Icon(
                        imageVector = if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                        tint = Color(0xFF8C8880)
                    )
                }
            },
            isError = !erroText.isNullOrBlank(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFFDF7),
                unfocusedContainerColor = Color(0xFFFFFDF7),
                disabledContainerColor = Color(0xFFFFFDF7),
                errorContainerColor = Color(0xFFFFFDF7),
                focusedBorderColor = if (!erroText.isNullOrBlank()) Color(0xFFA32D2D) else Color(0xFFB45C33),
                unfocusedBorderColor = if (!erroText.isNullOrBlank()) Color(0xFFA32D2D) else Color(0xFFD8D3C4),
                errorBorderColor = Color(0xFFA32D2D),
                focusedTextColor = Color(0xFF1A1A18),
                unfocusedTextColor = Color(0xFF1A1A18)
            ),
            singleLine = true
        )

        if (!erroText.isNullOrBlank()) {
            Text(
                text = erroText,
                fontSize = 12.sp,
                color = Color(0xFFA32D2D),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun LinhaRequisito(cumprido: Boolean, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (cumprido) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF1F4D2C),
                modifier = Modifier.size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8C8880))
            )
        }
        Text(
            text = texto,
            fontSize = 12.sp,
            color = if (cumprido) Color(0xFF1F4D2C) else Color(0xFF1A1A18)
        )
    }
}
