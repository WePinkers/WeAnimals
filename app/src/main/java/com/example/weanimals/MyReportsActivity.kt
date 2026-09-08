package com.example.weanimals

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityMyReportsBinding

class MyReportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyReportsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        setupProtocols()
    }

    private fun setupProtocols() {
        // Set specific data for mock protocols
        binding.protocol1.apply {
            protocolNumber.text = "Protocolo #4471"
            protocolDescription.text = getString(R.string.report_type_maus_tratos)
            protocolStatus.text = getString(R.string.status_completed)
        }
        
        binding.protocol2.apply {
            protocolNumber.text = "Protocolo #4462"
            protocolDescription.text = getString(R.string.report_type_agressao)
            protocolStatus.text = getString(R.string.status_completed)
        }
        
        binding.protocol3.apply {
            protocolNumber.text = "Protocolo #4455"
            protocolDescription.text = getString(R.string.report_type_abandono)
            protocolStatus.text = getString(R.string.status_completed)
        }
        
        binding.protocol4.apply {
            protocolNumber.text = "Protocolo #4440"
            protocolDescription.text = getString(R.string.report_type_risco)
            protocolStatus.text = getString(R.string.status_completed)
        }
        
        binding.protocol5.apply {
            protocolNumber.text = "Protocolo #4432"
            protocolDescription.text = getString(R.string.report_type_higiene)
            protocolStatus.text = getString(R.string.status_pending)
        }
        
        binding.protocol6.apply {
            protocolNumber.text = "Protocolo #4410"
            protocolDescription.text = getString(R.string.report_type_envenenamento)
            protocolStatus.text = getString(R.string.status_pending)
        }
    }
}
