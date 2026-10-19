package com.example.weanimals.lostandfound.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.FragmentLostStep3Binding
import com.example.weanimals.reporting.report.domain.NewReport
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.triage.domain.TriageClassification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LostStep3Fragment : Fragment() {

    private var _binding: FragmentLostStep3Binding? = null
    private val binding get() = _binding!!
    private val viewModel: LostAnimalViewModel by lazy {
        ViewModelProvider(requireActivity())[LostAnimalViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill date and time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        binding.inputDate.setText(dateFormat.format(now))
        binding.inputTime.setText(timeFormat.format(now))

        binding.btnBackStep3.setOnClickListener {
            viewModel.previousStep()
        }

        binding.btnAdjustLocation.setOnClickListener {
            Toast.makeText(requireContext(), "Localização ajustada para a região", Toast.LENGTH_SHORT).show()
        }

        binding.btnSubmitAnnouncement.setOnClickListener {
            submitAnnouncement()
        }
    }

    private fun submitAnnouncement() {
        val form = viewModel.formData
        val dateText = binding.inputDate.text.toString().trim()
        val timeText = binding.inputTime.text.toString().trim()
        val walkArea = binding.inputUsualWalk.text.toString().trim()

        form.date = dateText
        form.time = timeText
        form.usualWalkArea = walkArea

        val descriptionText = buildString {
            append("NOME: ${form.name.ifBlank { "Sem nome" }}\n")
            append("ESPÉCIE: ${form.species} | PORTE: ${form.size} | IDADE: ${form.age}\n")
            append("RAÇA: ${form.breed}\n")
            if (form.distinctiveMarks.isNotBlank()) {
                append("MARCAS: ${form.distinctiveMarks}\n")
            }
            if (form.identification.isNotEmpty()) {
                append("IDENTIFICAÇÃO: ${form.identification.joinToString()}\n")
            }
            append("COMPORTAMENTO: ${form.behavior}\n")
            if (form.secretDetail.isNotBlank()) {
                append("SEGREDO: ${form.secretDetail}\n")
            }
            append("VISTO EM: ${form.lastLocationTitle} (${form.lastLocationAddress}) em $dateText $timeText")
        }

        val newReport = NewReport(
            animalType = form.species,
            urgency = Report.URGENCY_HIGH,
            description = descriptionText,
            address = "${form.lastLocationTitle} — ${form.lastLocationAddress}",
            latitude = -23.5380,
            longitude = -46.6710,
            photoUri = form.photoUri,
            triageClassification = TriageClassification.PRIORITY.storageValue,
            triageAnswers = emptyMap()
        )

        lifecycleScope.launch {
            val appContainer = (requireActivity().application as WeAnimalsApplication).appContainer
            appContainer.reportRepository.submitReport(newReport)
            Toast.makeText(requireContext(), "Anúncio publicado com sucesso!", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
