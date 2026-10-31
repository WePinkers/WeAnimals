package com.example.weanimals.user.lostandfound.presentation

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivitySightingFormBinding
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class SightingFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySightingFormBinding
    private var currentAd: FoundPetAd? = null

    // State Step 1
    private var selectedSituation: String? = null // "WITH_REPORTER" or "SIGHTED_ONLY"
    private var selectedWhen: String? = null
    private var selectedWhenDate: String? = null
    private var addressStreet: String? = null
    private var addressNumber: String? = null
    private var addressNeighborhood: String? = null
    private var addressCity: String? = null
    private var addressState: String? = null
    private var addressComplement: String? = null

    // State Step 2
    private var photoUri: Uri? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                photoUri = uri
                loadPhotoPreview(uri)
            }
        }

    private val adjustLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data!!
                addressStreet = data.getStringExtra(AdjustLocationActivity.EXTRA_STREET)
                addressNumber = data.getStringExtra(AdjustLocationActivity.EXTRA_NUMBER)
                addressNeighborhood = data.getStringExtra(AdjustLocationActivity.EXTRA_NEIGHBORHOOD)
                addressCity = data.getStringExtra(AdjustLocationActivity.EXTRA_CITY)
                addressState = data.getStringExtra(AdjustLocationActivity.EXTRA_STATE)
                addressComplement = data.getStringExtra(AdjustLocationActivity.EXTRA_COMPLEMENT)

                updateLocationCardInStep1()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySightingFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adId = intent.getStringExtra(EXTRA_AD_ID)
        if (adId.isNullOrBlank()) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentAd = LostAndFoundRepository.getAdById(this, adId)
        if (currentAd == null) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeader()
        setupStep1()
        setupStep2()
    }

    private fun setupHeader() {
        binding.includeHeader.headerTitleTv.text = "Relatar possível encontro"
        binding.includeHeader.headerBackBtn.setOnClickListener {
            if (binding.viewFlipper.displayedChild == 1) {
                showStep1()
            } else {
                finish()
            }
        }
    }

    private fun showStep1() {
        binding.viewFlipper.displayedChild = 0
        binding.progressSegment1.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_beige)
        binding.tvProgressStep.text = "Passo 1 de 2: informações do encontro"
    }

    private fun showStep2() {
        binding.viewFlipper.displayedChild = 1
        binding.progressSegment1.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.tvProgressStep.text = "Passo 2 de 2: identificação"
    }

    private fun setupStep1() {
        val step1 = binding.step1Layout

        // Situação do animal
        val activeBg = R.drawable.bg_capsule_selected
        val inactiveBg = R.drawable.bg_capsule_unselected
        val activeColor = getColor(R.color.white)
        val inactiveColor = getColor(R.color.ink)

        step1.chipSituationWithMe.setOnClickListener {
            selectedSituation = "WITH_REPORTER"
            step1.chipSituationWithMe.setBackgroundResource(activeBg)
            step1.chipSituationWithMe.setTextColor(activeColor)
            step1.chipSituationSighted.setBackgroundResource(inactiveBg)
            step1.chipSituationSighted.setTextColor(inactiveColor)
        }

        step1.chipSituationSighted.setOnClickListener {
            selectedSituation = "SIGHTED_ONLY"
            step1.chipSituationSighted.setBackgroundResource(activeBg)
            step1.chipSituationSighted.setTextColor(activeColor)
            step1.chipSituationWithMe.setBackgroundResource(inactiveBg)
            step1.chipSituationWithMe.setTextColor(inactiveColor)
        }

        // Quando
        val whenChips = listOf(step1.chipSightingNow, step1.chipSightingToday, step1.chipSightingYesterday, step1.chipSightingOther)
        setupCapsuleGroup(whenChips) { selection ->
            selectedWhen = selection
            if (selection == "Outro dia") {
                showDatePickerDialog()
            }
        }

        // Adjust Location
        step1.btnSightingAdjustLocation.setOnClickListener {
            openAdjustLocationScreen()
        }

        step1.btnSightingContinueStep1.setOnClickListener {
            if (selectedSituation == null) {
                Toast.makeText(this, "Informe se você está com o animal ou se apenas o avistou.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedWhen == null) {
                Toast.makeText(this, "Selecione quando você encontrou/avistou o animal.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showStep2()
        }
    }

    private fun setupStep2() {
        val step2 = binding.step2Layout

        step2.layoutAddSightingPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        step2.btnSightingBackStep2.setOnClickListener { showStep1() }

        step2.btnSightingSubmit.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val characteristics = step2.etSightingCharacteristics.text.toString().trim()
            val notes = step2.etSightingNotes.text.toString().trim()

            if (characteristics.isBlank()) {
                step2.etSightingCharacteristics.error = "Informe as marcas ou características observadas."
                Toast.makeText(this, "Informe as marcas ou características observadas.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentProfileName = getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("user_name", null)
                ?: FirebaseAuth.getInstance().currentUser?.displayName
                ?: "Usuário"

            val situationText = if (selectedSituation == "WITH_REPORTER") "Animal está com o relator" else "Apenas avistado na rua"

            val specialMarkFull = listOfNotNull(
                "Situação: $situationText",
                "Marcas observadas: $characteristics",
                notes.takeIf { it.isNotBlank() }?.let { "Observações: $it" }
            ).joinToString(" · ")

            val req = TutorVerificationRequest(
                adId = ad.id,
                requesterUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user",
                requesterName = currentProfileName,
                ownerUserId = ad.ownerUserId,
                petName = "Avistamento ($situationText)",
                specialMark = specialMarkFull,
                photoUri = photoUri?.toString()
            )

            LostAndFoundRepository.saveVerificationRequest(this, req)

            Toast.makeText(this, "Relato enviado ao tutor com sucesso!", Toast.LENGTH_LONG).show()

            val intent = Intent(this, SubmittedAnswersActivity::class.java).apply {
                putExtra(SubmittedAnswersActivity.EXTRA_REQUEST_ID, req.id)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun openAdjustLocationScreen() {
        val intent = Intent(this, AdjustLocationActivity::class.java).apply {
            putExtra(AdjustLocationActivity.EXTRA_STREET, addressStreet)
            putExtra(AdjustLocationActivity.EXTRA_NUMBER, addressNumber)
            putExtra(AdjustLocationActivity.EXTRA_NEIGHBORHOOD, addressNeighborhood)
            putExtra(AdjustLocationActivity.EXTRA_CITY, addressCity)
            putExtra(AdjustLocationActivity.EXTRA_STATE, addressState)
            putExtra(AdjustLocationActivity.EXTRA_COMPLEMENT, addressComplement)
        }
        adjustLocationLauncher.launch(intent)
    }

    private fun updateLocationCardInStep1() {
        val step1 = binding.step1Layout
        if (!addressStreet.isNullOrBlank()) {
            val numText = if (addressNumber.isNullOrBlank()) "" else ", $addressNumber"
            step1.tvSightingLocationTitle.text = "$addressStreet$numText"

            val neighText = if (addressNeighborhood.isNullOrBlank()) "" else "$addressNeighborhood · "
            val stateText = if (addressState.isNullOrBlank()) "" else " - $addressState"
            step1.tvSightingLocationSubtitle.text = "$neighText${addressCity.orEmpty()}$stateText"
        }
    }

    private fun loadPhotoPreview(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = ImageThumbnailLoader.loadThumbnail(this@SightingFormActivity, uri.toString(), 400, 300)
            if (bitmap != null) {
                binding.step2Layout.ivSightingPhotoPreview.setImageBitmap(bitmap)
                binding.step2Layout.ivSightingPhotoPreview.visibility = View.VISIBLE
            }
        }
    }

    private fun setupCapsuleGroup(chips: List<TextView>, onSelected: (String) -> Unit) {
        val activeBg = R.drawable.bg_capsule_selected
        val inactiveBg = R.drawable.bg_capsule_unselected
        val activeColor = getColor(R.color.white)
        val inactiveColor = getColor(R.color.ink)

        for (chip in chips) {
            chip.setOnClickListener {
                for (c in chips) {
                    val isSelected = (c == chip)
                    c.setBackgroundResource(if (isSelected) activeBg else inactiveBg)
                    c.setTextColor(if (isSelected) activeColor else inactiveColor)
                }
                onSelected(chip.text.toString())
            }
        }
    }

    private fun showDatePickerDialog() {
        val cal = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedWhenDate = "$dayOfMonth/${month + 1}/$year"
                binding.step1Layout.chipSightingOther.text = selectedWhenDate
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
