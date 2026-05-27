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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantSessionManager {

    private val _currentStatus = MutableStateFlow<AssistantStatus>(AssistantStatus.Idle)
    val currentStatus: StateFlow<AssistantStatus> = _currentStatus.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    var isScreenshotPending = false
    var hasAttachedScreenshot = false

    private var currentAssistantText = StringBuilder()
    private var currentThoughtText = StringBuilder()
    private var thinkingStartTime = 0L
    private var thinkingTimeMs: Long? = null

    fun startSession() {
        _isGenerating.value = true
        resetInternalState()
        updateStatus(AssistantStatus.Preparing)
    }

    fun updateStatus(status: AssistantStatus) {
        _currentStatus.value = status
        
        when (status) {
            is AssistantStatus.Thinking -> {
                if (thinkingStartTime == 0L) {
                    thinkingStartTime = System.currentTimeMillis()
                }
                if (status.thought.isNotEmpty()) {
                    currentThoughtText.clear()
                    currentThoughtText.append(status.thought)
                }
                if (status.timeMs != null) {
                    thinkingTimeMs = status.timeMs
                }
            }
            is AssistantStatus.Streaming -> {
                if (status.thought != null && status.thought.isNotEmpty()) {
                    currentThoughtText.clear()
                    currentThoughtText.append(status.thought)
                }
                if (status.thinkingTimeMs != null) {
                    thinkingTimeMs = status.thinkingTimeMs
                }
            }
            else -> {}
        }
    }

    fun appendToken(token: String) {
        currentAssistantText.append(token)
        updateStatus(AssistantStatus.Streaming(
            text = currentAssistantText.toString(),
            thought = currentThoughtText.toString().takeIf { it.isNotEmpty() },
            thinkingTimeMs = thinkingTimeMs
        ))
    }

    fun endSession() {
        _isGenerating.value = false
        updateStatus(AssistantStatus.Idle)
    }

    fun clearAttachmentState() {
        isScreenshotPending = false
        hasAttachedScreenshot = false
    }

    private fun resetInternalState() {
        currentAssistantText.clear()
        currentThoughtText.clear()
        thinkingStartTime = 0L
        thinkingTimeMs = null
    }

    fun getThinkingText(): String? = currentThoughtText.toString().takeIf { it.isNotEmpty() }
    fun getThinkingTimeMs(): Long? = thinkingTimeMs
}
