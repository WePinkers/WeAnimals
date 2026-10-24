package com.example.weanimals.lostandfound.presentation

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityLostPetFormBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class LostPetFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostPetFormBinding

    // State Step 1
    private val selectedPhotos = mutableListOf<Uri>()
    private var selectedSpecies: String? = null
    private var selectedSize: String? = null
    private var selectedAge: String? = null

    // State Step 2
    private var selectedWhen: String? = null
    private var selectedWhenDate: String? = null
    private var addressStreet: String? = null
    private var addressNumber: String? = null
    private var addressNeighborhood: String? = null
    private var addressCity: String? = null
    private var addressState: String? = null
    private var addressComplement: String? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                selectedPhotos.addAll(uris)
                updatePhotoPreviews()
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

                updateLocationCardInStep2()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostPetFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupStep1()
        setupStep2()
        setupStep3()
    }

    private fun setupHeader() {
        binding.btnBackHeader.setOnClickListener {
            when (binding.viewFlipper.displayedChild) {
                1 -> showStep1()
                2 -> showStep2()
                else -> finish()
            }
        }
    }

    private fun showStep1() {
        binding.viewFlipper.displayedChild = 0
        binding.progressSegment1.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_beige)
        binding.progressSegment3.setBackgroundResource(R.drawable.bg_progress_segment_beige)
        binding.tvProgressStep.text = "Passo 1 de 3: o animal"
    }

    private fun showStep2() {
        binding.viewFlipper.displayedChild = 1
        binding.progressSegment1.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment3.setBackgroundResource(R.drawable.bg_progress_segment_beige)
        binding.tvProgressStep.text = "Passo 2 de 3: onde e quando sumiu"
    }

    private fun showStep3() {
        binding.viewFlipper.displayedChild = 2
        binding.progressSegment1.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.progressSegment3.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.tvProgressStep.text = "Passo 3 de 3: características e contatos"
    }

    private fun setupStep1() {
        val step1 = binding.step1Layout

        step1.layoutAddPhotos.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        // ESPÉCIE
        val speciesChips = listOf(step1.chipSpeciesDog, step1.chipSpeciesCat, step1.chipSpeciesOther)
        setupCapsuleGroup(speciesChips) { selection ->
            selectedSpecies = selection
            if (selection == "Outro") {
                step1.layoutCustomSpecies.visibility = View.VISIBLE
            } else {
                step1.layoutCustomSpecies.visibility = View.GONE
                step1.etCustomSpecies.text.clear()
                step1.etCustomSpecies.error = null
            }
        }

        // PORTE
        val sizeChips = listOf(step1.chipSizeSmall, step1.chipSizeMedium, step1.chipSizeLarge)
        setupCapsuleGroup(sizeChips) { selectedSize = it }

        // IDADE
        val ageChips = listOf(step1.chipAgePuppy, step1.chipAgeAdult, step1.chipAgeSenior)
        setupCapsuleGroup(ageChips) { selectedAge = it }

        step1.btnContinueStep1.setOnClickListener {
            val name = step1.etPetName.text.toString().trim()
            if (name.isBlank()) {
                step1.etPetName.error = "Informe o nome do animal."
                Toast.makeText(this, "Informe o nome do animal.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedSpecies == null) {
                Toast.makeText(this, "Selecione a espécie do animal.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedSpecies == "Outro") {
                val custom = step1.etCustomSpecies.text.toString().trim()
                if (custom.isBlank()) {
                    step1.etCustomSpecies.error = "Informe qual é a espécie do animal."
                    Toast.makeText(this, "Informe qual é a espécie do animal.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (selectedSize == null) {
                Toast.makeText(this, "Selecione o porte do animal.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedAge == null) {
                Toast.makeText(this, "Selecione a idade aproximada.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showStep2()
        }
    }

    private fun setupStep2() {
        val step2 = binding.step2Layout

        val whenChips = listOf(step2.chipWhenNow, step2.chipWhenToday, step2.chipWhenYesterday, step2.chipWhenOther)
        setupCapsuleGroup(whenChips) { selection ->
            selectedWhen = selection
            if (selection == "Outro dia") {
                showDatePickerDialog()
            }
        }

        step2.btnAdjustLocation.setOnClickListener {
            openAdjustLocationScreen()
        }

        step2.btnBackStep2.setOnClickListener { showStep1() }
        step2.btnContinueStep2.setOnClickListener { showStep3() }
    }

    private fun setupStep3() {
        val step3 = binding.step3Layout

        step3.btnBackStep3.setOnClickListener { showStep2() }

        step3.btnPublishLost.setOnClickListener {
            val name = binding.step1Layout.etPetName.text.toString().trim()
            val customSpeciesText = binding.step1Layout.etCustomSpecies.text.toString().trim()
            val breedText = binding.step1Layout.etBreed.text.toString().trim()
            val marksText = step3.etMarks.text.toString().trim()

            val finalSpecies = if (selectedSpecies == "Outro") {
                customSpeciesText.ifBlank { "Outro" }
            } else {
                selectedSpecies ?: "Outro"
            }

            val currentProfileName = getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("user_name", null)
                ?: FirebaseAuth.getInstance().currentUser?.displayName
                ?: "Usuário"

            val fullMarks = listOfNotNull(
                breedText.takeIf { it.isNotBlank() }?.let { "Raça: $it" },
                marksText.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            val ad = FoundPetAd(
                category = "PERDIDO",
                publisherName = currentProfileName,
                species = finalSpecies,
                otherType = if (selectedSpecies == "Outro") customSpeciesText else null,
                size = selectedSize,
                ageApprox = selectedAge,
                photoUris = selectedPhotos.map { it.toString() },
                street = addressStreet,
                number = addressNumber,
                neighborhood = addressNeighborhood,
                city = addressCity,
                state = addressState,
                complement = addressComplement,
                whenFound = selectedWhen,
                whenDate = selectedWhenDate,
                marks = fullMarks,
                calledNames = listOf("$name: reagiu"),
                currentSituation = "Procurado"
            )

            LostAndFoundRepository.saveAd(this, ad)

            Toast.makeText(this, "Anúncio de busca publicado com sucesso!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MyAdDetailActivity::class.java).apply {
                putExtra(MyAdDetailActivity.EXTRA_AD_ID, ad.id)
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

    private fun updateLocationCardInStep2() {
        val step2 = binding.step2Layout
        if (!addressStreet.isNullOrBlank()) {
            val numText = if (addressNumber.isNullOrBlank()) "" else ", $addressNumber"
            step2.tvLocationTitle.text = "$addressStreet$numText"

            val neighText = if (addressNeighborhood.isNullOrBlank()) "" else "$addressNeighborhood · "
            val stateText = if (addressState.isNullOrBlank()) "" else " - $addressState"
            step2.tvLocationSubtitle.text = "$neighText${addressCity.orEmpty()}$stateText"
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

    private fun updatePhotoPreviews() {
        val step1 = binding.step1Layout
        val container = step1.containerPhotoPreviews

        if (selectedPhotos.isEmpty()) {
            step1.scrollPhotoPreviews.visibility = View.GONE
            return
        }

        step1.scrollPhotoPreviews.visibility = View.VISIBLE
        container.removeAllViews()

        for (uri in selectedPhotos) {
            val frame = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 16, 0)
            }

            val img = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(160, 160)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }

            val removeText = TextView(this).apply {
                text = "Remover"
                textSize = 12f
                setTextColor(getColor(R.color.alert600))
                setPadding(0, 4, 0, 0)
                setOnClickListener {
                    selectedPhotos.remove(uri)
                    updatePhotoPreviews()
                }
            }

            frame.addView(img)
            frame.addView(removeText)
            container.addView(frame)
        }
    }

    private fun showDatePickerDialog() {
        val cal = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedWhenDate = "$dayOfMonth/${month + 1}/$year"
                binding.step2Layout.chipWhenOther.text = selectedWhenDate
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }
}
