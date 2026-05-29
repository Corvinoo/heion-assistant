/*
 *  Copyright (C) 2026 Corvinoo
 *  This file is part of Heion Cloudless Assistant
 *
 * Heion Cloudless Assistant is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Heion Cloudless Assistant is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Heion Cloudless Assistant. If not, see <https://www.gnu.org/licenses/>.
 *
 * This program is subject to additional terms, experimental software disclaimers,
 * and trademark limitations pursuant to Section 7 of the GNU GPLv3.
 * See the README and first-launch notice for details.
 */

package me.robin.heion

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import me.robin.heion.databinding.ActivityMainBinding
import me.robin.heion.inference.ModelManager
import me.robin.heion.settings.SettingsRepository
import me.robin.heion.setup.AssistantSetupManager
import me.robin.heion.setup.PermissionManager
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsRepository
    private var disclaimerDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SettingsRepository(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentDisclaimerHash = calculateDisclaimerHash(DISCLAIMER_MESSAGE)
        val acceptedHash = settings.getAcceptedDisclaimerHash()

        if (!settings.hasAcceptedDisclaimer() || currentDisclaimerHash != acceptedHash) {
            showDisclaimerDialog(currentDisclaimerHash)
        }

        setupUi()
    }

    private fun calculateDisclaimerHash(message: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(message.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showDisclaimerDialog(newHash: String) {
        if (isFinishing) return

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = DISCLAIMER_MESSAGE
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            textSize = 14f
        }
        scrollView.addView(textView)

        disclaimerDialog = AlertDialog.Builder(this)
            .setTitle("IMPORTANT DISCLAIMER & TERMS OF USE")
            .setView(scrollView)
            .setCancelable(false)
            .setPositiveButton("I Acknowledge") { _, _ ->
                settings.setAcceptedDisclaimer(true)
                settings.setAcceptedDisclaimerHash(newHash)
            }
            .setNegativeButton("Exit the app") { _, _ ->
                finish()
            }
            .create()

        disclaimerDialog?.show()

        val acceptButton = disclaimerDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
        acceptButton?.isEnabled = false

        val checkScroll = {
            val child = scrollView.getChildAt(0)
            if (child != null) {
                val isAtBottom = child.bottom <= (scrollView.height + scrollView.scrollY)
                if (isAtBottom) {
                    acceptButton?.isEnabled = true
                }
            }
        }

        scrollView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                checkScroll()
            }
        })

        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }
    }

    override fun onDestroy() {
        disclaimerDialog?.dismiss()
        super.onDestroy()
    }

    @OptIn(ExperimentalApi::class)
    private fun setupUi() {
        updateStatusText()

        binding.btnGrantOverlay.setOnClickListener {
            PermissionManager.requestOverlayPermission(this)
        }

        binding.btnGrantAudio.setOnClickListener {
            PermissionManager.requestAudioPermission(this)
        }

        binding.btnSetAssistant.setOnClickListener {
            AssistantSetupManager.openAssistantSettings(this)
        }

        binding.btnTriggerAssistant.setOnClickListener {
            if (!AssistantSetupManager.isDefaultAssistant(this)) {
                Toast.makeText(this, "Please set Heion as your default assistant app first.", Toast.LENGTH_LONG).show()
                AssistantSetupManager.openAssistantSettings(this)
                return@setOnClickListener
            }

            val intent = android.content.Intent(this, me.robin.heion.assistant.LocalVoiceInteractionService::class.java)
            intent.action = "me.robin.heion.TRIGGER_ASSISTANT"
            try {
                startService(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to start assistant service", e)
                Toast.makeText(this, "Failed to start assistant: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val currentTimeout = settings.getModelReleaseTimeout()
        binding.seekModelRelease.progress = currentTimeout
        updateReleaseLabel(currentTimeout)

        binding.seekModelRelease.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.setModelReleaseTimeout(progress)
                    updateReleaseLabel(progress)

                    //todo: test if timed release is working with app in background
                    if (progress == 0) {
                        ModelManager.release()
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })


        binding.speculativeDecodingToggle.setOnCheckedChangeListener { _, isChecked ->
            settings.setSpeculativeDecoding(isChecked)
            ExperimentalFlags.enableSpeculativeDecoding = isChecked
            Log.d(TAG, "Speculative decoding toggle state changed to: $isChecked")
            Log.d(TAG, "changes will be applied and next engine restart")
        }

        binding.ttsToggle.isChecked = settings.isTtsEnabled()
        binding.ttsToggle.setOnCheckedChangeListener { _, isChecked ->
            settings.setTtsEnabled(isChecked)
            Log.d(TAG, "TTS toggle state changed to: $isChecked")
        }

        binding.btnTtsDebug.setOnClickListener {
            val intent = android.content.Intent(this, me.robin.heion.tts.TtsDebugActivity::class.java)
            startActivity(intent)
        }

        binding.btnLicenses.setOnClickListener {
            val intent = android.content.Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }

        binding.btnManageModels.setOnClickListener {
            val intent = android.content.Intent(this, me.robin.heion.inference.ModelLibraryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateReleaseLabel(minutes: Int) {
        val text = when (minutes) {
            0 -> "Model release timeout: Instant"
            16 -> "Model release timeout: Unlimited"
            1 -> "Model release timeout: 1 minute"
            else -> "Model release timeout: $minutes minutes"
        }
        binding.txtModelReleaseLabel.text = text
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
    }

    private fun updateStatusText() {
        val overlayGranted = PermissionManager.hasOverlayPermission(this)
        val audioGranted = PermissionManager.hasAudioPermission(this)
        val isDefaultAssistant = AssistantSetupManager.isDefaultAssistant(this)

        binding.txtOverlayStatus.text =
            if (overlayGranted) {
                "Overlay permission granted"
            } else {
                "Overlay permission missing"
            }

        binding.txtAudioStatus.text =
            if (audioGranted) {
                "Audio permission granted"
            } else {
                "Audio permission missing"
            }

        binding.txtAssistantStatus.text =
            if (isDefaultAssistant) {
                "Heion is the default assistant"
            } else {
                "Heion is not the default assistant"
            }
    }

    companion object {
        private val DISCLAIMER_MESSAGE = """
            By using this application, you acknowledge that:

            1. EXPERIMENTAL SOFTWARE & WARRANTY DISCLAIMER
            This software is provided "AS IS". This application is experimental and currently in development, may be unstable, and may behave unpredictably.
            To the maximum extent permitted by applicable law the developers assume no responsibility or liability for any damages, including any data loss, device malfunctions, hardware overheating, physical component damage, or financial losses by using this application.
            You are solely responsible for any consequences resulting from installation, execution, or reliance on the software or its outputs.

            2. TRADEMARK & NON-ASSOCIATION NOTICE
            All product names, logos, brands, and trademarks featured or referred to within this software and its documentation are the property of their respective trademark holders.
            These trademark holders are not affiliated with, nor do they sponsor, endorse, or approve this software or its creators.
            
            3. AI OUTPUT DISCLAIMER
            AI-generated content may be incorrect, incomplete, or inappropriate. 
            Outputs are not guaranteed and must not be relied upon without independent verification.
            The developers assume no responsibility for user-generated inputs or prompts, and any legal liability arising from the generation of harmful, illegal, or infringing content rests solely with the user.
            
            4. THIRD-PARTY TERMS OF CONDUCT
            This application allows you to interact with external model weights (e.g., Google's Gemma).
            You acknowledge that using such models may subject you to separate, independent terms of use and acceptable use policies set by their respective creators, which you are responsible for following.
               
            5. INTERNET CONNECTIVITY  
            This application can make use of an internet connection to automatically download necessary external dependencies required for its proper functionality. 
            Standard internet data rates, carrier fees, and usage charges from your internet service provider may apply during these downloads.
            Alternatively, an option to manually load all required dependencies in the software without downloading them from the application is also provided.
        """.trimIndent()
    }
}
