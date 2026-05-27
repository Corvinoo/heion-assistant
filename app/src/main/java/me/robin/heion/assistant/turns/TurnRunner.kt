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

package me.robin.heion.assistant.turns

import android.graphics.Bitmap
import java.io.File
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.StreamingResponseParser
import me.robin.heion.assistant.agent.ParsedTurn
import me.robin.heion.assistant.agent.PromptBuilder
import me.robin.heion.assistant.profiles.ConversationMessage
import me.robin.heion.assistant.profiles.InteractionProfile
import me.robin.heion.assistant.profiles.ModelCapability
import me.robin.heion.assistant.profiles.ProfileContext
import me.robin.heion.assistant.profiles.SessionMetadata
import me.robin.heion.assistant.tools.ToolDefinition
import me.robin.heion.inference.ModelManager
import java.util.UUID

class TurnRunner(
    private val modelManager: ModelManager
) {
    companion object {
        private const val TOOL_CALL_OPEN = "<|tool_call>"
        private const val TOOL_CALL_CLOSE = "<tool_call|>"
        private const val TURN_END = "<turn|>"

        private const val THOUGHT_OPEN_TAG = "<|channel>thought"
        private const val THOUGHT_CLOSE_TAG = "<channel|>"
    }

    suspend fun runSingleTurn(
        userQuery: String,
        history: List<ConversationMessage>,
        activeProfile: InteractionProfile,
        screenshot: Bitmap?,
        audioFile: File?,
        screenText: String?,
        tools: List<ToolDefinition>,
        onToken: (String) -> Unit,
        onStatusUpdate: (AssistantStatus) -> Unit
    ): ParsedTurn {
        val profileContext = ProfileContext(
            userQuery = userQuery,
            conversationHistory = history,
            activeContextSources = emptyList(),
            sessionMetadata = SessionMetadata(
                sessionId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis()
            ),
            modelCapabilities = setOf(
                ModelCapability.VISION,
                ModelCapability.FUNCTION_CALLING,
                ModelCapability.THINKING,
                ModelCapability.AUDIO
            )
        )

        val inferenceInput = PromptBuilder.build(
            history = history,
            profile = activeProfile,
            profileContext = profileContext,
            tools = tools,
            screenText = screenText,
            currentAudioFile = audioFile,
            currentImage = screenshot
        )

        val parser = StreamingResponseParser(
            thoughtOpenTag = THOUGHT_OPEN_TAG,
            thoughtCloseTag = THOUGHT_CLOSE_TAG,
            toolCallOpenTag = TOOL_CALL_OPEN
        )

        val thinkingEnabled = activeProfile.reasoningConfig().thinkingEnabled
        if (thinkingEnabled) {
            onStatusUpdate(AssistantStatus.Thinking("", null))
        } else {
            onStatusUpdate(AssistantStatus.Preparing)
        }

        modelManager.infer(
            systemInstruction = inferenceInput.systemInstruction,
            history = inferenceInput.history,
            stopSequences = listOf(TOOL_CALL_CLOSE, TURN_END),
            onToken = { token ->
                val snapshot = parser.append(token)

                if (snapshot.visibleTextDelta.isNotEmpty()) {
                    onToken(snapshot.visibleTextDelta)
                }

                onStatusUpdate(
                    AssistantStatus.Streaming(
                        text = snapshot.visibleText,
                        thought = snapshot.thoughtText,
                        thinkingTimeMs = snapshot.thinkingTimeMs
                    )
                )
            }
        )

        return parser.finish()
    }
}
