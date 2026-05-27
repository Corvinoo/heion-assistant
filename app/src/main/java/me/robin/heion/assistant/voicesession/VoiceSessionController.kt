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

package me.robin.heion.assistant.voicesession

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.robin.heion.assistant.overlay.OverlayController
import me.robin.heion.settings.SettingsRepository
import me.robin.heion.tts.TtsRouter
import me.robin.heion.tts.streaming.StreamingTtsPlayer

class VoiceSessionController(
    private val context: Context,
    private val overlayController: OverlayController,
    private val recorderController: VoiceRecorderController,
    private val contextRepository: AssistContextRepository,
    private val queryOrchestrator: QueryOrchestrator,
    private val cancellationManager: QueryCancellationManager,
    private val ttsRouter: TtsRouter,
    private val streamingTtsPlayer: StreamingTtsPlayer,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Hidden)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var lastQuerySource: QuerySource = QuerySource.TEXT

    init {
        recorderController.onSilenceDetected = {
            handleEvent(SessionEvent.SilenceDetected)
        }
        
        overlayController.setOnVoiceModeDisabled {
            if (_state.value is SessionState.Recording) {
                stopVoiceMode()
            }
        }
    }

    fun handleEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.StartSession -> {
                transitionTo(SessionState.Idle)
                overlayController.expand(forceKeyboard = false)
                startVoiceMode()
            }
            is SessionEvent.UserQuery -> {
                submitQuery(
                    QueryRequest(
                        query = event.text,
                        includeScreenshot = event.includeScreenshot,
                        includeScreenText = event.includeScreenText,
                        audioFile = event.audioFile,
                        audioDurationMs = event.audioDurationMs
                    ),
                    event.source
                )
            }
            is SessionEvent.SilenceDetected -> {
                if (_state.value is SessionState.Recording) {
                    submitVoiceQuery()
                }
            }
            is SessionEvent.GenerationStarted -> {
                transitionTo(SessionState.Generating)
            }
            is SessionEvent.GenerationFinished -> {
                if (lastQuerySource == QuerySource.VOICE) {
                    transitionTo(SessionState.Restarting)
                    scope.launch {
                        delay(500)
                        if (_state.value is SessionState.Restarting) {
                            startVoiceMode()
                        }
                    }
                } else {
                    transitionTo(SessionState.Idle)
                }
            }
            is SessionEvent.Dismiss -> {
                if (_state.value == SessionState.Hidden) return
                transitionTo(SessionState.Hidden)
                stopInference()
                stopVoiceMode()
            }
            is SessionEvent.Stop -> {
                stopInference()
                transitionTo(SessionState.Idle)
            }
        }
    }

    private fun transitionTo(newState: SessionState) {
        _state.value = newState
        updateUiForState(newState)
    }

    private fun updateUiForState(state: SessionState) {
        when (state) {
            is SessionState.Recording -> {
                overlayController.setIsVoiceRecording(true)
            }
            is SessionState.Generating -> {
                overlayController.setIsGenerating(true)
            }
            is SessionState.Idle -> {
                overlayController.setIsVoiceRecording(false)
                overlayController.setIsGenerating(false)
            }
            is SessionState.Submitting -> {
                overlayController.setIsVoiceRecording(false)
                overlayController.setIsGenerating(true)
            }
            else -> {
                overlayController.setIsVoiceRecording(false)
                overlayController.setIsGenerating(false)
            }
        }
    }

    private fun startVoiceMode() {
        if (!me.robin.heion.setup.PermissionManager.hasAudioPermission(context)) {
            android.util.Log.w("LocalAssistant", "Audio permission missing, cannot start voice mode")
            transitionTo(SessionState.Idle)
            return
        }
        transitionTo(SessionState.Recording)
        recorderController.start()
    }

    private fun stopVoiceMode() {
        recorderController.stop()
        if (_state.value is SessionState.Recording) {
            transitionTo(SessionState.Idle)
        }
    }

    private fun submitVoiceQuery() {
        if (_state.value is SessionState.Submitting) return
        
        recorderController.stop()
        val audioFile = recorderController.getAudioFile()
        
        if (audioFile == null) {
            transitionTo(SessionState.Idle)
            return
        }

        val durationMs = recorderController.getAudioDurationMs()
        handleEvent(SessionEvent.UserQuery(
            text = "Voice Message",
            source = QuerySource.VOICE,
            includeScreenshot = false,
            includeScreenText = false,
            audioFile = audioFile,
            audioDurationMs = durationMs
        ))
    }

    private fun submitQuery(
        request: QueryRequest,
        source: QuerySource
    ) {
        stopInference()
        lastQuerySource = source
        transitionTo(SessionState.Submitting)

        if (request.audioDurationMs != null) {
            overlayController.addUserMessage(request.query, request.audioDurationMs)
        }

        val executionContext = QueryExecutionContext(
            snapshot = contextRepository.getSnapshot(),
            history = overlayController.getHistory()
        )

        val isVoice = source == QuerySource.VOICE && settingsRepository.isTtsEnabled()

        val job = scope.launch {
            handleEvent(SessionEvent.GenerationStarted)
            
            if (isVoice) {
                streamingTtsPlayer.start()
            }

            val result = queryOrchestrator.runQuery(
                request = request,
                executionContext = executionContext,
                onToken = { token ->
                    if (isVoice) {
                        streamingTtsPlayer.onToken(token)
                    }
                },
                onStatusUpdate = { /* status already pushed by orchestrator to overlay */ }
            )

            if (coroutineContext.isActive && result is QueryResult.Success) {
                if (isVoice) {
                    streamingTtsPlayer.finish()
                }
                handleEvent(SessionEvent.GenerationFinished)
            }
        }
        
        cancellationManager.start(job)
    }

    private fun stopInference() {
        cancellationManager.cancelCurrent()
        ttsRouter.stop()
        streamingTtsPlayer.stop()
    }
}
