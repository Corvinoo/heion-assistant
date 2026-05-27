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

class AssistantTurnStream(
    private val thoughtOpenTag: String,
    private val thoughtCloseTag: String,
    private val toolCallOpenTag: String
) {
    private val buffer = StringBuilder()
    private var lastEmittedVisibleText = ""
    private var thinkingStartedAt: Long? = null
    private var thinkingDurationMs: Long? = null

    fun append(token: String): AssistantTurnSnapshot {
        buffer.append(token)
        return snapshot(final = false)
    }

    fun finish(): AssistantTurnSnapshot {
        return snapshot(final = true)
    }

    private fun snapshot(final: Boolean): AssistantTurnSnapshot {
        val raw = buffer.toString()

        val thoughtRange = findThoughtRange(raw, final)
        val thoughtText = thoughtRange?.let {
            raw.substring(it.contentStart, it.contentEnd).trim()
        }

        if (thoughtRange != null && thinkingStartedAt == null) {
            thinkingStartedAt = System.currentTimeMillis()
        }

        if (thinkingDurationMs == null && thinkingStartedAt != null && (final || thoughtRange?.isComplete == true)) {
            thinkingDurationMs = (System.currentTimeMillis() - thinkingStartedAt!!)
                .coerceAtLeast(100L)
        }

        val withoutThoughts = removeThoughtBlocks(raw, final)
        
        // Strip other channel tags like <|channel>content
        val visibleText = withoutThoughts
            .replace("<|channel>content", "")
            .replace("<channel|>", "") // Close tag for any channel
            .substringBefore(toolCallOpenTag)
            .trim()

        val delta =
            if (visibleText.length > lastEmittedVisibleText.length &&
                visibleText.startsWith(lastEmittedVisibleText)
            ) {
                visibleText.substring(lastEmittedVisibleText.length)
            } else {
                ""
            }

        if (delta.isNotEmpty()) {
            lastEmittedVisibleText = visibleText
        }

        return AssistantTurnSnapshot(
            rawOutput = raw,
            visibleText = visibleText,
            visibleTextDelta = delta,
            thoughtText = thoughtText,
            thinkingTimeMs = thinkingDurationMs
        )
    }

    private fun removeThoughtBlocks(text: String, final: Boolean): String {
        var result = text

        while (true) {
            val range = findThoughtRange(result, final) ?: break
            result = buildString {
                append(result.substring(0, range.openStart))
                append(result.substring(range.contentEnd))
            }
        }

        return result
    }

    private fun findThoughtRange(
        text: String,
        final: Boolean
    ): ThoughtRange? {
        val openStart = text.indexOf(thoughtOpenTag)
        if (openStart == -1) return null

        val contentStart = openStart + thoughtOpenTag.length
        val closeIndex = text.indexOf(thoughtCloseTag, contentStart)

        if (closeIndex != -1) {
            return ThoughtRange(
                openStart = openStart,
                openEnd = contentStart,
                contentStart = contentStart,
                contentEnd = closeIndex,
                isComplete = true
            )
        }

        val implicitEnd = findImplicitEnd(text, contentStart, final)
        if (implicitEnd != -1) {
            return ThoughtRange(
                openStart = openStart,
                openEnd = contentStart,
                contentStart = contentStart,
                contentEnd = implicitEnd,
                isComplete = true
            )
        }

        return ThoughtRange(
            openStart = openStart,
            openEnd = contentStart,
            contentStart = contentStart,
            contentEnd = text.length,
            isComplete = false
        )
    }



    private fun findFirstCloseTagIndex(text: String, startIndex: Int): Int {
        var bestIndex = -1
        for (tag in thoughtCloseTag) {
            val idx = text.indexOf(tag, startIndex)
            if (idx != -1 && (bestIndex == -1 || idx < bestIndex)) {
                bestIndex = idx
            }
        }
        return bestIndex
    }

    private fun findImplicitEnd(text: String, startIndex: Int, final: Boolean): Int {
        if (final) {
            return text.length
        }

        val nextBlockIndex = text.indexOf("<|", startIndex)
        if (nextBlockIndex == -1) {
            return -1
        }

        val startsWithOpenTag =  text.startsWith(thoughtOpenTag, nextBlockIndex)
        return if (startsWithOpenTag) -1 else nextBlockIndex
    }
}


private data class ThoughtRange(
    val openStart: Int,
    val openEnd: Int,
    val contentStart: Int,
    val contentEnd: Int,
    val isComplete: Boolean
)
