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

import android.content.Context
import android.graphics.Bitmap
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import me.robin.heion.App
import me.robin.heion.assistant.overlay.OverlayController
import me.robin.heion.assistant.voicesession.*
import me.robin.heion.inference.ModelManager
import kotlinx.coroutines.launch

class LocalVoiceSession(context: Context) : VoiceInteractionSession(context) {

    private lateinit var overlayController: OverlayController
    private lateinit var sessionController: VoiceSessionController
    private lateinit var queryOrchestrator: QueryOrchestrator
    
    private val appContainer = (context.applicationContext as App).appContainer
    private var isDismissing = false

    override fun onCreate() {
        super.onCreate()

        if (!appContainer.settingsRepository.hasAcceptedDisclaimer()) {
            Log.w("LocalAssistant", "Assistant triggered but disclaimer not accepted. Finishing session.")
            Toast.makeText(context, "Please accept the terms of use in the Local Assistant app first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configureWindowForIme()

        overlayController = OverlayController(
            context = context,
            profileManager = appContainer.profileManager,
            conversationRepository = appContainer.conversationRepository,
            onQuery = { query, includeScreenshot, includeScreenText ->
                sessionController.handleEvent(SessionEvent.UserQuery(
                    text = query,
                    source = QuerySource.TEXT,
                    includeScreenshot = includeScreenshot,
                    includeScreenText = includeScreenText
                ))
            },
            onStop = {
                sessionController.handleEvent(SessionEvent.Stop)
            },
            onDismiss = { 
                if (!isDismissing) {
                    isDismissing = true
                    sessionController.handleEvent(SessionEvent.Dismiss)
                    finish()
                }
            },
            onSetFocusable = { focusable ->
                if (isDismissing) return@OverlayController
                val window = window?.window
                if (window != null) {
                    if (focusable) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                        window.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        )
                    } else {
                        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                        window.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                        )
                    }
                }

                if (::overlayController.isInitialized) {
                    overlayController.getRootView().post {
                        ViewCompat.requestApplyInsets(overlayController.getRootView())
                    }
                }
            }
        )
        overlayController.initialize()
        
        queryOrchestrator = QueryOrchestrator(
            context = context,
            agentLoop = appContainer.agentLoop,
            overlayController = overlayController,
            languageDetector = appContainer.languageDetector
        )

        sessionController = VoiceSessionController(
            context = context,
            overlayController = overlayController,
            recorderController = appContainer.recorderController,
            contextRepository = appContainer.contextRepository,
            queryOrchestrator = queryOrchestrator,
            cancellationManager = appContainer.cancellationManager,
            ttsRouter = appContainer.ttsRouter,
            streamingTtsPlayer = appContainer.streamingTtsPlayer,
            settingsRepository = appContainer.settingsRepository,
            scope = appContainer.serviceScope
        )

        appContainer.serviceScope.launch {
            try {
                ModelManager.ensureLoaded(context.applicationContext)
            } catch (e: Exception) {
                e.message?.let { Log.d("error", it) }
            }
        }
    }

    private fun configureWindowForIme() {
        window?.let { window ->
            window.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            )
        }
    }

    override fun onCreateContentView(): View {
        return overlayController.getRootView()
    }

    override fun onShow(args: android.os.Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        sessionController.handleEvent(SessionEvent.StartSession)
    }

    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        state.assistStructure?.let { structure ->
            appContainer.contextRepository.updateAssistStructure(structure)
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        appContainer.contextRepository.updateScreenshot(screenshot)
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)

        outInsets.contentInsets.setEmpty()
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION

        if (isDismissing) {
            outInsets.touchableRegion.setEmpty()
            return
        }

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        val rect = overlayController.getTouchableRegion(screenWidth, screenHeight)
        outInsets.touchableRegion.set(rect)
    }

    override fun onCloseSystemDialogs() {
        if (!isDismissing) {
            overlayController.dismiss()
        }
    }

    override fun onHide() {
        super.onHide()
        if (!isDismissing) {
            isDismissing = true
            sessionController.handleEvent(SessionEvent.Dismiss)
            overlayController.dismiss()
        }

        val timeoutMinutes = appContainer.settingsRepository.getModelReleaseTimeout()
        if (timeoutMinutes < 16) {
            val delayMillis = timeoutMinutes.toLong() * 60_000L
            if (delayMillis == 0L) {
                ModelManager.release()
            } else {
                ModelManager.scheduleRelease(delayMillis)
            }
        }
    }
}
