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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class AgentLoopThoughtTest {

    private val THOUGHT_OPEN = "<|channel>thought\n"
    private val THOUGHT_CLOSE = "<channel|>"

    /**
     * This test simulates the logic inside AgentLoop.onToken to see if it correctly
     * extracts thoughts and regular text from a streaming Gemma 4 output.
     */
    @Test
    fun `test thought extraction during streaming`() {
        val fullOutput = "Hello! <|channel>thought\nI should be helpful.\n<channel|>I am here to help."
        
        // --- Simulation of AgentLoop.kt logic ---
        var thoughtStartIdx = -1
        var activeOpenTag = ""
        val THOUGHT_OPEN_TAGS = listOf(THOUGHT_OPEN)
        val THOUGHT_CLOSE_TAGS = listOf(THOUGHT_CLOSE)

        for (tag in THOUGHT_OPEN_TAGS) {
            val idx = fullOutput.indexOf(tag)
            if (idx != -1) {
                thoughtStartIdx = idx
                activeOpenTag = tag
                break
            }
        }

        assertTrue("Thought start tag should be found", thoughtStartIdx != -1)

        var thoughtEndIdx = -1
        for (tag in THOUGHT_CLOSE_TAGS) {
            val idx = fullOutput.indexOf(tag, thoughtStartIdx + activeOpenTag.length)
            if (idx != -1) {
                thoughtEndIdx = idx
                break
            }
        }

        assertTrue("Thought end tag should be found", thoughtEndIdx != -1)

        val extractedThought = fullOutput.substring(thoughtStartIdx + activeOpenTag.length, thoughtEndIdx).trim()
        assertEquals("I should be helpful.", extractedThought)

        // Text extraction logic
        var textWithoutThought = fullOutput
        val openIdx = textWithoutThought.indexOf(activeOpenTag)
        val closeIdx = textWithoutThought.indexOf(THOUGHT_CLOSE, openIdx + activeOpenTag.length)
        
        if (openIdx != -1 && closeIdx != -1) {
            textWithoutThought = textWithoutThought.replaceRange(openIdx, closeIdx + THOUGHT_CLOSE.length, "")
        }

        val cleanedText = textWithoutThought.trim()
        assertEquals("Hello! I am here to help.", cleanedText)
    }

    /**
     * Tests the stripThoughts helper logic.
     */
    @Test
    fun `test stripThoughts logic`() {
        val THOUGHT_OPEN_TAGS = listOf(THOUGHT_OPEN)
        val THOUGHT_CLOSE_TAGS = listOf(THOUGHT_CLOSE)
        val thought = "I should be helpful."
        val modelOutput = "Hello! <|channel>thought\n$thought\n<channel|>I am here to help."

        fun stripThoughts(text: String, thought: String): String {
            var result = text
            THOUGHT_OPEN_TAGS.forEach { result = result.replace(it, "") }
            THOUGHT_CLOSE_TAGS.forEach { result = result.replace(it, "") }
            if (thought.isNotEmpty()) {
                result = result.replace(thought, "")
            }
            return result.trim()
        }

        val result = stripThoughts(modelOutput, thought)
        assertEquals("Hello! I am here to help.", result)
    }
}
