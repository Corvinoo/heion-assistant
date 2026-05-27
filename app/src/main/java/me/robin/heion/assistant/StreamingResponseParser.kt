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

import me.robin.heion.assistant.agent.AssistantTurnSnapshot
import me.robin.heion.assistant.agent.AssistantTurnStream
import me.robin.heion.assistant.agent.ParsedTurn
import me.robin.heion.assistant.tools.ToolCallParser

class StreamingResponseParser(
    private val thoughtOpenTag: String,
    private val thoughtCloseTag: String,
    private val toolCallOpenTag: String
) {
    private val stream = AssistantTurnStream(
        thoughtOpenTag = thoughtOpenTag,
        thoughtCloseTag = thoughtCloseTag,
        toolCallOpenTag = toolCallOpenTag
    )

    fun append(token: String): AssistantTurnSnapshot {
        return stream.append(token)
    }

    fun finish(): ParsedTurn {
        val finalSnapshot = stream.finish()
        val rawOutput = finalSnapshot.rawOutput.trim()

        if (rawOutput.isBlank()) {
            return ParsedTurn.blank()
        }

        val toolCall = ToolCallParser.detect(rawOutput)
        val finalText = stripThoughts(rawOutput)

        return if (toolCall != null) {
            val leadingText = ToolCallParser.stripToolCall(rawOutput)
                .let { stripThoughts(it) }

            ParsedTurn(
                rawOutput = rawOutput,
                finalText = finalText,
                leadingText = leadingText,
                thoughtText = finalSnapshot.thoughtText,
                thinkingTimeMs = finalSnapshot.thinkingTimeMs,
                toolCall = toolCall
            )
        } else {
            ParsedTurn(
                rawOutput = rawOutput,
                finalText = finalText,
                leadingText = finalText,
                thoughtText = finalSnapshot.thoughtText,
                thinkingTimeMs = finalSnapshot.thinkingTimeMs,
                toolCall = null
            )
        }
    }

    private fun stripThoughts(text: String): String {
        return text.replace(thoughtOpenTag, "")
            .replace(thoughtCloseTag, "")
            .replace("<|channel>thought", "") // just in case
            .replace("<|channel>content", "")
            .trim()
    }
}
