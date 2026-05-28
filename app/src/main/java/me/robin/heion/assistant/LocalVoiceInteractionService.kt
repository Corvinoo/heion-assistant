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

package me.robin.heion.assistant

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.robin.heion.inference.ModelManager
import me.robin.heion.settings.SettingsRepository

class LocalVoiceInteractionService : VoiceInteractionService() {

    private var isReady = false
    private var pendingShowSession = false

    override fun onCreate() {
        super.onCreate()
        Log.d("LocalAssistant", "VoiceInteractionService created")
    }

    override fun onReady() {
        super.onReady()
        isReady = true
        Log.d("LocalAssistant", "VoiceInteractionService ready")

        val settings = SettingsRepository(applicationContext)

        if (settings.hasAcceptedDisclaimer() && settings.getModelReleaseTimeout() == 16) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    ModelManager.ensureLoaded(applicationContext)
                } catch (e: Exception) {
                    Log.e("LocalAssistant", "Model preload failed", e)
                }
            }
        } else {
            // Ensure any previously loaded model is released if setting changed
            ModelManager.release()
        }

        if (pendingShowSession) {
            Log.d("LocalAssistant", "Processing pending trigger")
            triggerAssistant()
            pendingShowSession = false
        }
    }

    private fun triggerAssistant() {
        showSession(
            Bundle(),
            VoiceInteractionSession.SHOW_WITH_ASSIST or
                    VoiceInteractionSession.SHOW_WITH_SCREENSHOT
        )
    }

    override fun onShutdown() {
        super.onShutdown()
        isReady = false
        ModelManager.release()
        Log.d("LocalAssistant", "VoiceInteractionService shutdown")
    }
    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()

        Log.d("LocalAssistant", "Launch from keyguard")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "me.robin.heion.TRIGGER_ASSISTANT") {
            Log.d("LocalAssistant", "Manual trigger received")
            if (isReady) {
                triggerAssistant()
            } else {
                Log.d("LocalAssistant", "Service not ready yet, pending trigger")
                pendingShowSession = true
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

}