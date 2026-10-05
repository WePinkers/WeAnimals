package com.example.weanimals.lostandfound.presentation

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityReportLostPetBinding
import com.example.weanimals.databinding.ItemLostPetMatchBinding
import com.example.weanimals.lostandfound.domain.AnimalSize
import com.example.weanimals.lostandfound.domain.AnimalSpecies
import com.example.weanimals.lostandfound.domain.PetMatch
import com.example.weanimals.lostandfound.presenter.LostAndFoundContract
import com.example.weanimals.lostandfound.presenter.LostAndFoundPresenter
import com.example.weanimals.lostandfound.presenter.LostAndFoundState

class ReportLostPetActivity : AppCompatActivity(), LostAndFoundContract.View {
    private lateinit var binding: ActivityReportLostPetBinding
    private val presenter: LostAndFoundPresenter = LostAndFoundPresenter()
    private var selectedPhotoUri: Uri? = null
    private var proofPhotoUri: Uri? = null

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        selectedPhotoUri = uri
        val preview = binding.screenContainer.findViewById<ImageView>(R.id.photo_preview)
        val placeholder = binding.screenContainer.findViewById<TextView>(R.id.photo_placeholder)
        preview?.setImageURI(uri)
        preview?.visibility = View.VISIBLE
        placeholder?.visibility = View.GONE
    }

    private val proofPhotoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        proofPhotoUri = uri
        val preview = binding.screenContainer.findViewById<ImageView>(R.id.proof_photo_preview)
        val placeholder = binding.screenContainer.findViewById<TextView>(R.id.proof_photo_placeholder)
        preview?.setImageURI(uri)
        preview?.visibility = View.VISIBLE
        placeholder?.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportLostPetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        presenter.attachView(this)
        presenter.start()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun render(state: LostAndFoundState) {
        binding.screenContainer.removeAllViews()
        when (state.step) {
            LostAndFoundState.Step.FORM -> renderForm(state)
            LostAndFoundState.Step.MATCHES -> renderMatches(state)
            LostAndFoundState.Step.DETAILS -> renderDetails(state)
            LostAndFoundState.Step.VERIFICATION -> renderVerification()
            LostAndFoundState.Step.WAITING -> renderWaiting(state)
            LostAndFoundState.Step.REPORT_SENT -> renderReportSent()
        }
    }

    private fun renderForm(state: LostAndFoundState) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.screen_lost_pet_form, binding.screenContainer, false)
        binding.screenContainer.addView(view)

        view.findViewById<View>(R.id.back_button).setOnClickListener { finish() }
        view.findViewById<View>(R.id.photo_area).setOnClickListener {
            photoPicker.launch("image/*")
        }
        view.findViewById<View>(R.id.search_matches_button).setOnClickListener {
            val species = if (view.findViewById<android.widget.RadioButton>(R.id.species_cat).isChecked) {
                AnimalSpecies.CAT
            } else {
                AnimalSpecies.DOG
            }
            val size = when {
                view.findViewById<android.widget.RadioButton>(R.id.size_small).isChecked -> AnimalSize.SMALL
                view.findViewById<android.widget.RadioButton>(R.id.size_large).isChecked -> AnimalSize.LARGE
                else -> AnimalSize.MEDIUM
            }
            presenter.submitForm(species, size)
        }
    }

    private fun renderMatches(state: LostAndFoundState) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.screen_lost_pet_matches, binding.screenContainer, false)
        binding.screenContainer.addView(view)
        view.findViewById<View>(R.id.back_button).setOnClickListener { presenter.goBack() }
        view.findViewById<TextView>(R.id.matches_description).text =
            "Encontramos possíveis correspondências para ${if (state.species == AnimalSpecies.DOG) "cachorro" else "gato"}."

        val container = view.findViewById<LinearLayout>(R.id.matches_container)
        mockMatches(state.species).forEach { match ->
            val matchBinding = ItemLostPetMatchBinding.inflate(
                layoutInflater,
                container,
                false
            )
            matchBinding.matchScore.text = "${match.matchPercentage}% compatível"
            matchBinding.matchTitle.text = match.title
            matchBinding.matchSubtitle.text = match.subtitle
            matchBinding.matchTags.text = match.tags.joinToString("  ·  ") { it.label }
            matchBinding.viewMatchButton.setOnClickListener { presenter.openMatch(match) }
            container.addView(matchBinding.root)
        }
    }

    private fun renderDetails(state: LostAndFoundState) {
        val match = state.selectedMatch ?: return presenter.goBack()
        val view = LayoutInflater.from(this)
            .inflate(R.layout.screen_lost_pet_details, binding.screenContainer, false)
        binding.screenContainer.addView(view)
        view.findViewById<View>(R.id.back_button).setOnClickListener { presenter.goBack() }
        view.findViewById<TextView>(R.id.details_title).text = match.title
        view.findViewById<TextView>(R.id.details_match).text =
            "${match.matchPercentage}% de compatibilidade"
        view.findViewById<TextView>(R.id.details_features).text =
            match.tags.joinToString("\n") { "• ${it.label}" }
        view.findViewById<View>(R.id.confirm_owner_button).setOnClickListener {
            presenter.openVerification()
        }
        view.findViewById<View>(R.id.report_match_button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Denunciar anúncio")
                .setMessage("Conte para nós por que este anúncio precisa ser verificado.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar") { _, _ -> presenter.submitReport() }
                .show()
        }
    }

    private fun renderVerification() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.screen_lost_pet_verification, binding.screenContainer, false)
        binding.screenContainer.addView(view)
        view.findViewById<View>(R.id.back_button).setOnClickListener { presenter.goBack() }
        view.findViewById<View>(R.id.proof_photo_area).setOnClickListener {
            proofPhotoPicker.launch("image/*")
        }
        view.findViewById<View>(R.id.send_verification_button).setOnClickListener {
            val name = view.findViewById<android.widget.EditText>(R.id.pet_name).text.toString().trim()
            val mark = view.findViewById<android.widget.EditText>(R.id.pet_mark).text.toString().trim()
            val error = view.findViewById<TextView>(R.id.verification_error)
            if (name.isBlank() || mark.isBlank()) {
                error.text = "Preencha as duas respostas para continuar."
                error.visibility = View.VISIBLE
                return@setOnClickListener
            }
            presenter.submitVerification(
                buildList {
                    add("Nome: $name")
                    add("Sinal: $mark")
                    if (proofPhotoUri != null) add("Foto de comprovação anexada")
                }
            )
        }
    }

    private fun renderWaiting(state: LostAndFoundState) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.screen_lost_pet_waiting, binding.screenContainer, false)
        binding.screenContainer.addView(view)
        view.findViewById<TextView>(R.id.answers_summary).text = state.answers.joinToString("\n")
        view.findViewById<View>(R.id.back_to_matches_button).setOnClickListener { presenter.goBack() }
    }

    private fun renderReportSent() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.screen_lost_pet_sent, binding.screenContainer, false)
        binding.screenContainer.addView(view)
        view.findViewById<View>(R.id.understood_button).setOnClickListener { presenter.goBack() }
    }

    private fun mockMatches(species: AnimalSpecies): List<PetMatch> =
        com.example.weanimals.lostandfound.domain.mockMatchesFor(species)
}
