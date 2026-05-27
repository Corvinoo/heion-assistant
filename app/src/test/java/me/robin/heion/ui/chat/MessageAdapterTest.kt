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

package me.robin.heion.ui.chat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.profiles.ConversationMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.noties.markwon.Markwon

class MessageAdapterTest {

    private lateinit var context: Context
    private lateinit var markwon: Markwon
    private lateinit var adapter: MessageAdapter

    @BeforeEach
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        markwon = mockk(relaxed = true)
        adapter = MessageAdapter(markwon)
    }

    @Test
    fun `addMessage increases item count`() {
        val message = ConversationMessage.User("Hello")
        adapter.addMessage(message)
        assertEquals(1, adapter.itemCount)
        assertEquals(message, adapter.getMessages()[0])
    }

    @Test
    fun `updateLastMessageStatus updates the status of the last model message`() {
        adapter.addMessage(ConversationMessage.User("Question"))
        adapter.addMessage(ConversationMessage.Model(""))
        
        adapter.updateLastMessageStatus(AssistantStatus.Thinking("Thinking...", null))
        
        val lastMessage = adapter.getLastMessage() as ConversationMessage.Model
        assertTrue(lastMessage.status is AssistantStatus.Thinking)
    }

    @Test
    fun `updateLastMessageThinking updates thinking fields of the last model message`() {
        adapter.addMessage(ConversationMessage.Model(""))
        
        adapter.updateLastMessageThinking("My thoughts", 1234L)
        
        val lastMessage = adapter.getLastMessage() as ConversationMessage.Model
        assertEquals("My thoughts", lastMessage.thinkingText)
        assertEquals(1234L, lastMessage.thinkingTimeMs)
    }

    @Test
    fun `toggleThoughtExpansion flips the isThoughtExpanded flag`() {
        adapter.addMessage(ConversationMessage.Model(""))
        assertFalse((adapter.getMessages()[0] as ConversationMessage.Model).isThoughtExpanded)
        
        adapter.toggleThoughtExpansion(0)
        assertTrue((adapter.getMessages()[0] as ConversationMessage.Model).isThoughtExpanded)
        
        adapter.toggleThoughtExpansion(0)
        assertFalse((adapter.getMessages()[0] as ConversationMessage.Model).isThoughtExpanded)
    }

    @Test
    fun `updateLastMessage appends text to the last model message`() {
        adapter.addMessage(ConversationMessage.Model("Hello"))
        
        adapter.updateLastMessage(" world")
        
        val lastMessage = adapter.getLastMessage() as ConversationMessage.Model
        assertEquals("Hello world", lastMessage.text)
    }

    @Test
    fun `updateLastMessage handles cumulative streams`() {
        adapter.addMessage(ConversationMessage.Model("Hello"))
        
        // If the chunk starts with the existing text, it's cumulative
        adapter.updateLastMessage("Hello world")
        
        val lastMessage = adapter.getLastMessage() as ConversationMessage.Model
        assertEquals("Hello world", lastMessage.text)
    }

    @Test
    fun `clear removes all messages`() {
        adapter.addMessage(ConversationMessage.User("1"))
        adapter.addMessage(ConversationMessage.User("2"))
        assertEquals(2, adapter.itemCount)
        
        adapter.clear()
        assertEquals(0, adapter.itemCount)
    }
}
