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

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AssistantSessionManagerTest {

    private lateinit var sessionManager: AssistantSessionManager

    @BeforeEach
    fun setUp() {
        sessionManager = AssistantSessionManager()
    }

    @Test
    fun `startSession sets state to Preparing and isGenerating to true`() = runTest {
        sessionManager.isGenerating.test {
            assertEquals(false, awaitItem())
            sessionManager.startSession()
            assertEquals(true, awaitItem())
        }
        assertEquals(AssistantStatus.Preparing, sessionManager.currentStatus.value)
    }

    @Test
    fun `updateStatus Thinking updates internal thinking text`() {
        sessionManager.updateStatus(AssistantStatus.Thinking(thought = "I am thinking", timeMs = 500L))
        
        assertEquals("I am thinking", sessionManager.getThinkingText())
        assertEquals(500L, sessionManager.getThinkingTimeMs())
    }

    @Test
    fun `appendToken updates status to Streaming with accumulated text`() = runTest {
        sessionManager.currentStatus.test {
            assertEquals(AssistantStatus.Idle, awaitItem())
            
            sessionManager.appendToken("Hello")
            val status1 = awaitItem() as AssistantStatus.Streaming
            assertEquals("Hello", status1.text)
            
            sessionManager.appendToken(" world")
            val status2 = awaitItem() as AssistantStatus.Streaming
            assertEquals("Hello world", status2.text)
        }
    }

    @Test
    fun `appendToken preserves thinking info`() {
        sessionManager.updateStatus(AssistantStatus.Thinking(thought = "Logic", timeMs = 1000L))
        sessionManager.appendToken("Result")
        
        val status = sessionManager.currentStatus.value as AssistantStatus.Streaming
        assertEquals("Logic", status.thought)
        assertEquals(1000L, status.thinkingTimeMs)
    }

    @Test
    fun `endSession resets isGenerating and sets status to Idle`() = runTest {
        sessionManager.startSession()
        
        sessionManager.isGenerating.test {
            assertEquals(true, awaitItem())
            sessionManager.endSession()
            assertEquals(false, awaitItem())
        }
        assertEquals(AssistantStatus.Idle, sessionManager.currentStatus.value)
    }

    @Test
    fun `clearAttachmentState resets screenshot flags`() {
        sessionManager.isScreenshotPending = true
        sessionManager.hasAttachedScreenshot = true
        
        sessionManager.clearAttachmentState()
        
        assertEquals(false, sessionManager.isScreenshotPending)
        assertEquals(false, sessionManager.hasAttachedScreenshot)
    }
}
