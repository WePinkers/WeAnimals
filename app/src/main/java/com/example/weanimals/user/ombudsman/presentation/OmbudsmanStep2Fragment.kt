package com.example.weanimals.user.ombudsman.presentation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weanimals.databinding.FragmentOmbudsmanStep2Binding
import com.example.weanimals.user.ombudsman.domain.ManifestationType

class OmbudsmanStep2Fragment : Fragment() {

    private var _binding: FragmentOmbudsmanStep2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: ManifestationViewModel by lazy {
        ViewModelProvider(requireActivity())[ManifestationViewModel::class.java]
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "evidencia_anexada.jpg"
            viewModel.formData.attachedFileName = fileName
            binding.txtAttachedFile.text = "Anexado: $fileName"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOmbudsmanStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubjectSpinner()
        setupDescriptionInput()

        binding.containerAttachFile.setOnClickListener {
            filePickerLauncher.launch("image/*")
        }

        binding.btnNextStep2.setOnClickListener {
            val desc = binding.inputDescription.text.toString().trim()
            if (desc.isBlank()) {
                binding.inputDescription.error = "Digite a descrição"
                Toast.makeText(requireContext(), "Por favor, informe a descrição do ocorrido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.formData.description = desc
            viewModel.formData.subject = binding.spinnerSubject.selectedItem?.toString().orEmpty()
            viewModel.nextStep()
        }
    }

    private fun setupSubjectSpinner() {
        val subjects = when (viewModel.formData.type) {
            ManifestationType.RECLAMACAO -> listOf(
                "Problema técnico no app",
                "ONG não compareceu / não atendeu",
                "Problema na Comunidade"
            )
            ManifestationType.DENUNCIA -> listOf(
                "Suposto golpe / cobrança indevida",
                "Campanha suspeita",
                "Denúncia falsa",
                "Mensagens ofensivas",
                "Achados e perdidos"
            )
            ManifestationType.SOLICITACAO -> listOf(
                "Atualização de dados",
                "Contestação de decisão",
                "Esclarecimento"
            )
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, subjects)
        binding.spinnerSubject.adapter = adapter
    }

    private fun setupDescriptionInput() {
        binding.inputDescription.doAfterTextChanged { text ->
            val count = text?.length ?: 0
            binding.charCounter.text = "$count/1000"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
