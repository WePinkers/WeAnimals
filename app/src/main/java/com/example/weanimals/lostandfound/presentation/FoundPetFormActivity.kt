package com.example.weanimals.lostandfound.presentation

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityFoundPetFormBinding
import java.util.Calendar

class FoundPetFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoundPetFormBinding

    // State Step 1
    private var selectedSpecies: String? = null
    private var selectedSize: String? = null
    private var selectedAge: String? = null
    private var selectedCondition: String? = null
    private val selectedPhotos = mutableListOf<Uri>()

    // State Step 2
    private var selectedWhen: String? = null
    private var selectedWhenDate: String? = null
    private var selectedCurrentSituation: String? = null
    private val calledNamesList = mutableListOf<CalledNameItem>()

    // Address State
    private var addressStreet: String? = null
    private var addressNumber: String? = null
    private var addressNeighborhood: String? = null
    private var addressCity: String? = null
    private var addressState: String? = null
    private var addressComplement: String? = null

    data class CalledNameItem(var name: String, var reacted: Boolean)

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
        binding = ActivityFoundPetFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupStep1()
        setupStep2()
    }

    private fun setupHeader() {
        binding.btnBackHeader.setOnClickListener {
            if (binding.viewFlipper.displayedChild == 1) {
                showStep1()
            } else {
                finish()
            }
        }
    }

    private fun showStep1() {
        binding.viewFlipper.displayedChild = 0
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_beige)
        binding.tvProgressStep.text = "Passo 1 de 2: o animal"
    }

    private fun showStep2() {
        binding.viewFlipper.displayedChild = 1
        binding.progressSegment2.setBackgroundResource(R.drawable.bg_progress_segment_terracotta)
        binding.tvProgressStep.text = "Passo 2 de 2: onde e como identificar"
    }

    private fun setupStep1() {
        val step1 = binding.step1Layout

        // Photo picker
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

        // IDADE APROXIMADA
        val ageChips = listOf(step1.chipAgePuppy, step1.chipAgeAdult, step1.chipAgeSenior)
        setupCapsuleGroup(ageChips) { selectedAge = it }

        // COMO ELE ESTÁ?
        val conditionChips = listOf(
            step1.chipConditionHealthy,
            step1.chipConditionScared,
            step1.chipConditionInjured,
            step1.chipConditionWeak
        )
        setupCapsuleGroup(conditionChips) { selectedCondition = it }

        // Continuar button
        step1.btnContinueStep1.setOnClickListener {
            if (selectedSpecies == "Outro") {
                val customSpecies = step1.etCustomSpecies.text.toString().trim()
                if (customSpecies.isBlank()) {
                    step1.etCustomSpecies.error = "Informe qual é a espécie do animal."
                    Toast.makeText(this, "Informe qual é a espécie do animal.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            showStep2()
        }
    }

    private fun setupStep2() {
        val step2 = binding.step2Layout

        // ONDE E QUANDO ENCONTROU - Quando
        val whenChips = listOf(step2.chipWhenNow, step2.chipWhenToday, step2.chipWhenYesterday, step2.chipWhenOther)
        setupCapsuleGroup(whenChips) { selection ->
            selectedWhen = selection
            if (selection == "Outro dia") {
                showDatePickerDialog()
            }
        }

        // ONDE ELE ESTÁ AGORA?
        val currentChips = listOf(step2.chipCurrentStreet, step2.chipCurrentWithMe)
        setupCapsuleGroup(currentChips) { selectedCurrentSituation = it }

        // Adjust Location Button
        step2.btnAdjustLocation.setOnClickListener {
            openAdjustLocationScreen()
        }

        // Add Called Name Button
        step2.btnAddCalledName.setOnClickListener {
            showAddNameDialog()
        }

        // Voltar Button
        step2.btnBackStep2.setOnClickListener {
            showStep1()
        }

        // Publicar Button
        step2.btnPublish.setOnClickListener {
            val customSpeciesText = binding.step1Layout.etCustomSpecies.text.toString().trim()
            val finalSpecies = if (selectedSpecies == "Outro") {
                customSpeciesText.ifBlank { "Outro" }
            } else {
                selectedSpecies ?: "Outro"
            }

            val currentProfileName = getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("user_name", null)
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
                ?: "Usuário"

            val ad = com.example.weanimals.lostandfound.model.FoundPetAd(
                category = "ENCONTRADO",
                publisherName = currentProfileName,
                species = finalSpecies,
                otherType = if (selectedSpecies == "Outro") customSpeciesText else null,
                size = selectedSize,
                ageApprox = selectedAge,
                condition = selectedCondition,
                photoUris = selectedPhotos.map { it.toString() },
                street = addressStreet,
                number = addressNumber,
                neighborhood = addressNeighborhood,
                city = addressCity,
                state = addressState,
                complement = addressComplement,
                whenFound = selectedWhen,
                whenDate = selectedWhenDate,
                marks = step2.etMarks.text?.toString()?.trim(),
                calledNames = calledNamesList.map { "${it.name}: ${if (it.reacted) "reagiu" else "não reagiu"}" },
                currentSituation = selectedCurrentSituation ?: "Comigo"
            )

            com.example.weanimals.lostandfound.repository.LostAndFoundRepository.saveAd(this, ad)

            Toast.makeText(this, "Anúncio publicado com sucesso!", Toast.LENGTH_SHORT).show()

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

    private fun showAddNameDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_called_name, null)
        val etName = view.findViewById<EditText>(R.id.et_called_name)
        val rgReaction = view.findViewById<RadioGroup>(R.id.rg_reaction)

        AlertDialog.Builder(this)
            .setTitle("Adicionar nome tentado")
            .setView(view)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotBlank()) {
                    val reacted = (rgReaction.checkedRadioButtonId == R.id.rb_reacted)
                    calledNamesList.add(CalledNameItem(name, reacted))
                    renderCalledNames()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renderCalledNames() {
        val container = binding.step2Layout.containerCalledNames
        container.removeAllViews()

        for (item in calledNamesList) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40)
                if (item.reacted) {
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(getColor(R.color.pine800))
                } else {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(getColor(R.color.alert600))
                }
            }

            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 16
                }
                text = if (item.reacted) "${item.name}: reagiu" else "${item.name}: não reagiu"
                textSize = 14f
                setTextColor(getColor(R.color.ink))
            }

            val btnDelete = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(36, 36)
                setImageResource(R.drawable.ic_close)
                setColorFilter(getColor(R.color.muted))
                setOnClickListener {
                    calledNamesList.remove(item)
                    renderCalledNames()
                }
            }

            row.addView(icon)
            row.addView(tv)
            row.addView(btnDelete)
            container.addView(row)
        }
    }
}
