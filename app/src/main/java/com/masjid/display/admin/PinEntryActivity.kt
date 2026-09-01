package com.masjid.display.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.masjid.display.core.util.PinHasher
import com.masjid.display.data.local.AppDatabase
import com.masjid.display.data.repository.DisplayConfigRepository
import com.masjid.display.databinding.ActivityPinEntryBinding
import kotlinx.coroutines.launch

class PinEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinEntryBinding
    private lateinit var displayConfigRepository: DisplayConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayConfigRepository = DisplayConfigRepository(
            AppDatabase.getInstance(applicationContext).displayConfigDao()
        )

        binding.btnConfirmPin.setOnClickListener { verifyPin() }
    }

    private fun verifyPin() {
        val enteredPin = binding.inputPin.text.toString()
        lifecycleScope.launch {
            val config = displayConfigRepository.get()
            if (PinHasher.matches(enteredPin, config.adminPinHash)) {
                startActivity(Intent(this@PinEntryActivity, AdminSettingsActivity::class.java))
                finish()
            } else {
                binding.textPinError.isVisible = true
            }
        }
    }
}
