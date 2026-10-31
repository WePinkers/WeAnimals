package com.example.weanimals.user.lostandfound.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityAdjustLocationBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class AdjustLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdjustLocationBinding
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted || coarseGranted) {
                fetchCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Permissão de localização negada. Preencha o endereço manualmente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdjustLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenHeader.headerTitle.text = "Onde encontrou o animal?"

        setupSpinner()
        prefillFromIntent()
        setupListeners()
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.brazil_states,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spState.adapter = adapter

        // Default to SP (index 24)
        val defaultPosition = adapter.getPosition("SP")
        if (defaultPosition >= 0) {
            binding.spState.setSelection(defaultPosition)
        }
    }

    private fun prefillFromIntent() {
        intent.getStringExtra(EXTRA_STREET)?.let { binding.etStreet.setText(it) }
        intent.getStringExtra(EXTRA_NUMBER)?.let { binding.etNumber.setText(it) }
        intent.getStringExtra(EXTRA_NEIGHBORHOOD)?.let { binding.etNeighborhood.setText(it) }
        intent.getStringExtra(EXTRA_CITY)?.let { binding.etCity.setText(it) }
        intent.getStringExtra(EXTRA_COMPLEMENT)?.let { binding.etComplement.setText(it) }

        intent.getStringExtra(EXTRA_STATE)?.let { state ->
            val adapter = binding.spState.adapter as? ArrayAdapter<String>
            val pos = (0 until (adapter?.count ?: 0)).firstOrNull { adapter?.getItem(it) == state }
            if (pos != null) binding.spState.setSelection(pos)
        }
    }

    private fun setupListeners() {
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.btnUseCurrentLocation.setOnClickListener {
            checkAndRequestLocationPermission()
        }

        binding.btnConfirmLocation.setOnClickListener {
            confirmLocation()
        }
    }

    private fun checkAndRequestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchCurrentLocation() {
        binding.pbLocationLoading.visibility = View.VISIBLE
        binding.btnUseCurrentLocation.isEnabled = false

        lifecycleScope.launch {
            try {
                val cancellationToken = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationToken.token
                ).await() ?: fusedLocationClient.lastLocation.await()

                if (location != null) {
                    val address = reverseGeocode(location.latitude, location.longitude)
                    if (address != null) {
                        populateAddressFields(address)
                        Toast.makeText(
                            this@AdjustLocationActivity,
                            "Endereço obtido com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@AdjustLocationActivity,
                            "Não foi possível identificar o endereço. Preencha manualmente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@AdjustLocationActivity,
                        "GPS inativo ou localização indisponível. Preencha manualmente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdjustLocationActivity,
                    "Falha ao obter localização. Preencha manualmente.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.pbLocationLoading.visibility = View.GONE
                binding.btnUseCurrentLocation.isEnabled = true
            }
        }
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(applicationContext, Locale.forLanguageTag("pt-BR"))

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }
    }

    private fun populateAddressFields(address: Address) {
        val street = address.thoroughfare.orEmpty()
        val number = address.subThoroughfare.orEmpty()
        val neighborhood = address.subLocality.orEmpty()
        val city = address.locality.orEmpty().ifBlank { address.subAdminArea.orEmpty() }
        val state = address.adminArea.orEmpty()

        if (street.isNotBlank()) binding.etStreet.setText(street)
        if (number.isNotBlank()) binding.etNumber.setText(number)
        if (neighborhood.isNotBlank()) binding.etNeighborhood.setText(neighborhood)
        if (city.isNotBlank()) binding.etCity.setText(city)

        if (state.isNotBlank()) {
            val adapter = binding.spState.adapter as? ArrayAdapter<String>
            val pos = (0 until (adapter?.count ?: 0)).firstOrNull {
                adapter?.getItem(it)?.equals(state, ignoreCase = true) == true ||
                        state.contains(adapter?.getItem(it).orEmpty(), ignoreCase = true)
            }
            if (pos != null) binding.spState.setSelection(pos)
        }
    }

    private fun confirmLocation() {
        val street = binding.etStreet.text.toString().trim()
        val number = binding.etNumber.text.toString().trim()
        val neighborhood = binding.etNeighborhood.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.spState.selectedItem?.toString().orEmpty()
        val complement = binding.etComplement.text.toString().trim()

        var hasError = false

        if (street.isBlank()) {
            binding.etStreet.error = "Informe a rua ou avenida"
            hasError = true
        }

        if (neighborhood.isBlank()) {
            binding.etNeighborhood.error = "Informe o bairro"
            hasError = true
        }

        if (city.isBlank()) {
            binding.etCity.error = "Informe a cidade"
            hasError = true
        }

        if (hasError) return

        val resultIntent = Intent().apply {
            putExtra(EXTRA_STREET, street)
            putExtra(EXTRA_NUMBER, number)
            putExtra(EXTRA_NEIGHBORHOOD, neighborhood)
            putExtra(EXTRA_CITY, city)
            putExtra(EXTRA_STATE, state)
            putExtra(EXTRA_COMPLEMENT, complement)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_STREET = "extra_street"
        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_NEIGHBORHOOD = "extra_neighborhood"
        const val EXTRA_CITY = "extra_city"
        const val EXTRA_STATE = "extra_state"
        const val EXTRA_COMPLEMENT = "extra_complement"
    }
}
