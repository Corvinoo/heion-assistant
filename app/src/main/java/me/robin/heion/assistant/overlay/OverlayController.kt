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

package me.robin.heion.assistant.overlay

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.profiles.ConversationMessage
import me.robin.heion.assistant.profiles.ProfileManager
import me.robin.heion.assistant.repository.ConversationRepository
import me.robin.heion.assistant.repository.MessageFactory
import me.robin.heion.databinding.OverlayMainBinding
import me.robin.heion.inference.ModelManager

class OverlayController(
    context: Context,
    profileManager: ProfileManager,
    private val conversationRepository: ConversationRepository,
    onQuery: (String, Boolean, Boolean) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    onSetFocusable: (Boolean) -> Unit
) {
    private val binding = OverlayMainBinding.inflate(LayoutInflater.from(context))
    private val renderer = OverlayRenderer(context, binding)
    private val coordinator = OverlayCoordinator(
        renderer = renderer,
        binding = binding,
        profileManager = profileManager,
        conversationRepository = conversationRepository,
        onQuery = onQuery,
        onStop = onStop,
        onDismiss = onDismiss,
        onSetFocusable = onSetFocusable
    )

    fun initialize() {
        coordinator.initialize()
    }

    fun getRootView(): View = renderer.getRootView()

    fun getHistory(): List<ConversationMessage> = conversationRepository.getHistory()

    fun isExpanded(): Boolean = renderer.isExpanded()

    fun appendToken(token: String) {
        conversationRepository.updateLastMessage(
            predicate = { it is ConversationMessage.Model },
            update = { 
                val model = it as ConversationMessage.Model
                model.copy(
                    text = model.text + token,
                    isThoughtExpanded = model.isThoughtExpanded // Preserve expansion state
                )
            }
        )
    }

    fun setAssistantStatus(status: AssistantStatus) {
        val last = conversationRepository.getHistory().lastOrNull()
        if (last is ConversationMessage.Model) {
            conversationRepository.updateLastMessage(
                predicate = { it is ConversationMessage.Model },
                update = { 
                    val model = it as ConversationMessage.Model
                    when (status) {
                        is AssistantStatus.Thinking -> model.copy(
                            status = status,
                            thinkingText = status.thought.takeIf { it.isNotEmpty() } ?: model.thinkingText,
                            thinkingTimeMs = status.timeMs ?: model.thinkingTimeMs,
                            isThoughtExpanded = model.isThoughtExpanded // Preserve
                        )
                        is AssistantStatus.Streaming -> model.copy(
                            status = status,
                            text = status.text.takeIf { it.isNotEmpty() } ?: model.text,
                            thinkingText = status.thought ?: model.thinkingText,
                            thinkingTimeMs = status.thinkingTimeMs ?: model.thinkingTimeMs,
                            isThoughtExpanded = model.isThoughtExpanded // Preserve
                        )
                        else -> model.copy(
                            status = status,
                            isThoughtExpanded = model.isThoughtExpanded // Preserve
                        )
                    }
                }
            )
        } else if (status !is AssistantStatus.Idle) {
            conversationRepository.addMessage(
                MessageFactory.createModelMessage(
                    status = status,
                    modelName = ModelManager.getModelName()
                )
            )
        }
    }

    fun setIsGenerating(generating: Boolean) {
        coordinator.isGenerating = generating
        if (!generating) {
            setAssistantStatus(AssistantStatus.Idle)
        }
        renderer.updateSendButtonIcon(generating, binding.mainInput.text.isNullOrEmpty())
    }

    fun setOnVoiceModeDisabled(listener: () -> Unit) {
        coordinator.setOnVoiceModeDisabled(listener)
    }

    fun setIsVoiceRecording(active: Boolean) {
        renderer.updateVoiceRecordingUI(active)
    }

    fun addUserMessage(query: String, audioDurationMs: Long? = null) {
        coordinator.lockMode()
        conversationRepository.addMessage(
            MessageFactory.createUserMessage(text = query, audioDurationMs = audioDurationMs)
        )
    }

    fun getTouchableRegion(screenWidth: Int, screenHeight: Int): Rect {
        return renderer.getTouchableRegion(screenWidth, screenHeight)
    }

    fun expand(forceKeyboard: Boolean = false) {
        coordinator.expand(forceKeyboard)
    }

    fun dismiss() {
        coordinator.dismiss()
    }

    fun destroy() {
        coordinator.destroy()
    }
}
