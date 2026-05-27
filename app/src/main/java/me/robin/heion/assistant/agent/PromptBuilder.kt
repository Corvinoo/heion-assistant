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
import me.robin.heion.assistant.profiles.*
import me.robin.heion.assistant.tools.ToolDefinition
import me.robin.heion.inference.ChatTurn
import java.io.File

data class InferenceInput(
    val systemInstruction: String,
    val history: List<ChatTurn>
)

object PromptBuilder {

    private const val STR = "<|\"|>"          // string delimiter
    private const val SOT = "<|tool>"
    private const val EOT = "<tool|>"
    private const val TOOL_CALL_START = "<|tool_call>"
    private const val TOOL_CALL_END = "<tool_call|>"
    private const val TOOL_RESPONSE_START = "<|tool_response>"
    private const val TOOL_RESPONSE_END = "<tool_response|>"
    private const val THINK = "<|think|>"
    fun build(
        history: List<ConversationMessage>,
        profile: InteractionProfile,
        profileContext: ProfileContext,
        tools: List<ToolDefinition>,
        screenText: String? = null,
        currentAudioFile: File? = null,
        currentImage: Bitmap? = null
    ): InferenceInput {

        // -- System Instruction --
        val systemInstruction = buildString {

            val reasoning = profile.reasoningConfig()
            if (reasoning.thinkingEnabled) {
                append("$THINK\n")
            }

            append(profile.buildSystemPrompt(profileContext))
            append("\n")

            if (!screenText.isNullOrBlank()) {
                append("\nThe user's current screen contains:\n<screen>\n$screenText\n</screen>\n")
            }

            // Serialize tool definitions into system instruction
            val enabledToolNames = profile.enabledTools()
            val toolsToInclude = tools.filter { it.name in enabledToolNames }

            if (toolsToInclude.isNotEmpty()) {
                append("\n$SOT\n[")
                toolsToInclude.forEachIndexed { i, tool ->
                    append("{\"name\":$STR${tool.name}$STR,")
                    append("\"description\":$STR${tool.description}$STR,")
                    append("\"parameters\":{")
                    append("\"type\":${STR}object$STR,")
                    append("\"properties\":{")
                    tool.parameters.entries.forEachIndexed { pi, (pName, pDef) ->
                        append("\"$pName\":{")
                        append("\"type\":$STR${pDef.type}$STR,")
                        append("\"description\":$STR${pDef.description}$STR")
                        if (pDef.enum != null) {
                            val enumList = pDef.enum.joinToString(",") { "$STR$it$STR" }
                            append(",\"enum\":[$enumList]")
                        }
                        append("}")
                        if (pi < tool.parameters.size - 1) append(",")
                    }
                    append("},")
                    val reqList = tool.required.joinToString(",") { "$STR$it$STR" }
                    append("\"required\":[$reqList]")
                    append("}}")
                    if (i < toolsToInclude.size - 1) append(",")
                }
                append("]\n$EOT\n")
            }
        }

        // -- Conversation history --
        val chatTurns = history.mapIndexed { index, msg ->
            val isLast = index == history.size - 1
            when (msg) {
                is ConversationMessage.User -> {
                    ChatTurn.User(
                        text = msg.text,
                        // Attach current media to the very last user message
                        audioFile = if (isLast) currentAudioFile else null,
                        imageBitmap = if (isLast) currentImage ?: msg.image else msg.image
                    )
                }
                is ConversationMessage.Model -> {
                    ChatTurn.Assistant(
                        text = msg.text,
                        thought = msg.thinkingText
                    )
                }
                is ConversationMessage.ToolCall -> {
                    // Tool protocol remains textual inside the structured turn
                    val argsJson = msg.args.entries.joinToString(",") {
                        "\"${it.key}\":$STR${it.value}$STR"
                    }
                    ChatTurn.Assistant(
                        text = "$TOOL_CALL_START{\"name\":$STR${msg.name}$STR,\"args\":{$argsJson}}$TOOL_CALL_END"
                    )
                }
                is ConversationMessage.ToolResult -> {
                    // Tool protocol remains textual
                    ChatTurn.ToolResponse(
                        name = msg.name,
                        result = "$TOOL_RESPONSE_START${msg.result}$TOOL_RESPONSE_END"
                    )
                }
            }
        }

        return InferenceInput(systemInstruction, chatTurns)
    }
}
