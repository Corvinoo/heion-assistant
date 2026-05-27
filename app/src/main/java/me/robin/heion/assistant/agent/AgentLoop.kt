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

package me.robin.heion.assistant.agent

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.profiles.ConversationMessage
import me.robin.heion.assistant.tools.ToolExecutor
import me.robin.heion.assistant.tools.ToolRegistry
import me.robin.heion.assistant.turns.TurnRunner
import me.robin.heion.inference.ModelManager

class AgentLoop(
    private val profileResolver: ProfileResolver,
    private val turnRunner: TurnRunner,
    private val toolExecutor: ToolExecutor,
    private val toolRegistry: ToolRegistry = ToolRegistry
) {
    companion object {
        private const val MAX_TOOL_ROUNDS = 5
    }

    suspend fun run(
        userQuery: String,
        historyToAppend: List<ConversationMessage>,
        screenshot: Bitmap?,
        audioFile: java.io.File? = null,
        screenText: String?,
        onToken: (String) -> Unit,
        onStatusUpdate: (AssistantStatus) -> Unit
    ): String = withContext(Dispatchers.Default) {
        val history = historyToAppend.toMutableList()

        val activeProfile = profileResolver.resolve(userQuery, onStatusUpdate)

        appendUserMessageIfNeeded(
            history = history,
            userQuery = userQuery,
            screenshot = screenshot,
            profileId = activeProfile.id
        )

        var lastAssistantText = ""

        repeat(MAX_TOOL_ROUNDS) {
            val turnResult = turnRunner.runSingleTurn(
                userQuery = userQuery,
                history = history,
                activeProfile = activeProfile,
                screenshot = screenshot,
                audioFile = audioFile,
                screenText = screenText,
                tools = toolRegistry.tools,
                onToken = onToken,
                onStatusUpdate = onStatusUpdate
            )

            if (turnResult.rawOutput.isBlank()) {
                return@withContext lastAssistantText
            }

            if (turnResult.toolCall == null) {
                history.add(
                    ConversationMessage.Model(
                        text = turnResult.finalText,
                        profileId = activeProfile.id,
                        thinkingText = turnResult.thoughtText,
                        thinkingTimeMs = turnResult.thinkingTimeMs,
                        isThoughtExpanded = turnResult.thoughtText != null,
                        modelName = ModelManager.getModelName()
                    )
                )
                return@withContext turnResult.finalText
            }

            // If there's leading text before the tool call, record it as a model message
            if (turnResult.leadingText.isNotBlank()) {
                history.add(
                    ConversationMessage.Model(
                        text = turnResult.leadingText,
                        profileId = activeProfile.id,
                        thinkingText = turnResult.thoughtText,
                        thinkingTimeMs = turnResult.thinkingTimeMs,
                        isThoughtExpanded = turnResult.thoughtText != null,
                        modelName = ModelManager.getModelName()
                    )
                )
            }

            // Record the tool call itself
            history.add(
                ConversationMessage.ToolCall(
                    name = turnResult.toolCall.name,
                    args = turnResult.toolCall.args,
                    profileId = activeProfile.id
                )
            )

            onStatusUpdate(AssistantStatus.UsingTools)

            val toolResult = toolExecutor.execute(turnResult.toolCall)
            
            // Record the tool result
            history.add(
                ConversationMessage.ToolResult(
                    name = turnResult.toolCall.name,
                    result = toolResult,
                    profileId = activeProfile.id
                )
            )

            onStatusUpdate(AssistantStatus.Preparing)
        }

        lastAssistantText.ifBlank {
            history.lastOrNull { it is ConversationMessage.Model }
                ?.let { it as ConversationMessage.Model }
                ?.text
                .orEmpty()
        }
    }

    private fun appendUserMessageIfNeeded(
        history: MutableList<ConversationMessage>,
        userQuery: String,
        screenshot: Bitmap?,
        profileId: String
    ) {
        val lastMessage = history.lastOrNull()
        val shouldAppend = lastMessage !is ConversationMessage.User ||
                lastMessage.text != userQuery

        if (shouldAppend) {
            history.add(
                ConversationMessage.User(
                    text = userQuery,
                    image = screenshot,
                    profileId = profileId
                )
            )
        }
    }
}
