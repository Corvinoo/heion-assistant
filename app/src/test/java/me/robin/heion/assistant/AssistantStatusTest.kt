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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AssistantStatusTest {

    @Test
    fun `getLabel returns correct strings for simple states`() {
        assertEquals("", AssistantStatus.Idle.getLabel())
        assertEquals("Loading model...", AssistantStatus.LoadingModel.getLabel())
        assertEquals("Routing to profile...", AssistantStatus.Routing.getLabel())
        assertEquals("Preparing to write...", AssistantStatus.Preparing.getLabel())
        assertEquals("Looking at images...", AssistantStatus.LookingAtImages.getLabel())
        assertEquals("Searching online...", AssistantStatus.SearchingOnline.getLabel())
        assertEquals("Using tools...", AssistantStatus.UsingTools.getLabel())
    }

    @Test
    fun `getLabel returns correct string for Error state`() {
        val status = AssistantStatus.Error("Network failure")
        assertEquals("Error: Network failure", status.getLabel())
    }

    @Test
    fun `getLabel returns correct string for Thinking state`() {
        assertEquals("Thinking...", AssistantStatus.Thinking().getLabel())
        
        // AssistantStatus.kt uses Locale.US internally for string formatting
        assertEquals("Thought for 1.5s", AssistantStatus.Thinking("thought", 1500L).getLabel())
        assertEquals("Thought for 0.1s", AssistantStatus.Thinking("thought", 100L).getLabel())
    }

    @Test
    fun `getLabel returns correct string for Streaming state`() {
        assertEquals("Generating...", AssistantStatus.Streaming("some text").getLabel())
        assertEquals("Thought for 2.0s", AssistantStatus.Streaming("text", "thought", 2000L).getLabel())
    }
}
