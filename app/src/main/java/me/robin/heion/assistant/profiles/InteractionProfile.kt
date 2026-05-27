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

package me.robin.heion.assistant.profiles

import android.graphics.Bitmap
import me.robin.heion.assistant.AssistantStatus

/**
 * A profile is a behavioral configuration layer applied on top of the inference stack.
 */
interface InteractionProfile {

    // === Identity ===
    val id: String                          // stable, unique identifier
    val displayName: String                 // shown in profile chip; max 18 chars
    val shortName: String                   // shown when chip is compact; max 8 chars
    val iconRes: Int                        // vector drawable, 20dp, for chip
    val description: String                 // shown in profile picker; 1–2 sentences
    val category: ProfileCategory           // GENERAL, LANGUAGE, ANALYSIS, UTILITY

    // === Prompt Composition ===
    fun buildSystemPrompt(context: ProfileContext): String

    // === Context Preprocessing ===
    // Called before context is attached to the prompt
    // Profiles may compress, reformat, or selectively include context
    fun preprocessContext(source: ContextSource): ProcessedContext

    // === Tool Configuration ===
    // Return a subset of the registered tools
    fun enabledTools(): Set<String>

    // === Reasoning Configuration ===
    fun reasoningConfig(): ReasoningConfig

    // === Response Formatting ===
    // Applied as a post-processing step to the raw model output
    // May inject formatting, strip markdown for cleaner TTS, etc
    fun formatResponse(raw: String, context: ProfileContext): String

    // === Verbosity === || WIP
    val defaultVerbosity: Verbosity         // CONCISE, BALANCED, DETAILED

    // === Suggested Actions === || WIP
    // Contextual quick-action chips shown above the input field
    // in the expanded state. Max 3 items. Empty list = none shown.
    fun suggestedActions(context: ProfileContext): List<SuggestedAction>

    // === Memory Policy === || WIP
    val memoryPolicy: MemoryPolicy          // SESSION_ONLY, PERSISTENT, NONE

    // === Compatibility === || WIP
    // Profiles may declare required model capabilities.
    // ProfileManager validates before activation.
    val requiredCapabilities: Set<ModelCapability>
}

// === Supporting Data Structures ===

data class ReasoningConfig(
    val thinkingEnabled: Boolean,
    val thinkingBudget: ThinkingBudget,     // MINIMAL, STANDARD, DEEP
    val selfCritiqueEnabled: Boolean
)

enum class ThinkingBudget { MINIMAL, STANDARD, DEEP }

data class ProcessedContext(
    val textContent: String,
    val imageBitmap: Bitmap?,
    val priority: ContextPriority,          // PRIMARY, SUPPLEMENTARY, BACKGROUND
    val tokenBudget: Int                    // max tokens this source may consume || check if working
)

enum class ContextPriority { BACKGROUND, SUPPLEMENTARY, PRIMARY }

data class SuggestedAction(
    val label: String,                      // max 20 chars
    val prompt: String,                     // injected as user message when tapped
    val icon: Int? = null                   // optional vector drawable
)

data class ProfileContext(
    val userQuery: String,
    val conversationHistory: List<ConversationMessage>,
    val activeContextSources: List<ContextSource>,
    val sessionMetadata: SessionMetadata,
    val modelCapabilities: Set<ModelCapability>
)

// === Conversation Message ===

sealed class ConversationMessage {
    abstract val profileId: String?
    abstract val timestamp: Long

    data class User(
        val text: String,
        val image: Bitmap? = null,
        val audioDurationMs: Long? = null,
        override val profileId: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ConversationMessage()

    data class Model(
        val text: String,
        val status: AssistantStatus = AssistantStatus.Idle,
        val thinkingText: String? = null,
        val thinkingTimeMs: Long? = null,
        val isThoughtExpanded: Boolean = false,
        val modelName: String? = null,
        override val profileId: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ConversationMessage()

    data class ToolCall(
        val name: String,
        val args: Map<String, Any>,
        override val profileId: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ConversationMessage()

    data class ToolResult(
        val name: String,
        val result: String,
        override val profileId: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ConversationMessage()

    // Helper to get role for prompt building
    val role: String
        get() = when (this) {
            is User -> "user"
            is Model -> "model"
            is ToolCall -> "model" // Model emits tool calls
            is ToolResult -> "tool"
        }
    
    // Helper to get text content for UI or reasoning
    val content: String
        get() = when (this) {
            is User -> text
            is Model -> text
            is ToolCall -> "Tool Call: $name($args)"
            is ToolResult -> result
        }
}

// Placeholder for session metadata
data class SessionMetadata(
    val sessionId: String,
    val timestamp: Long
)

enum class MemoryPolicy { SESSION_ONLY, PERSISTENT, NONE }
enum class Verbosity    { CONCISE, BALANCED, DETAILED }
enum class ProfileCategory { GENERAL, LANGUAGE, ANALYSIS, UTILITY }
enum class ModelCapability { VISION, AUDIO, FUNCTION_CALLING, THINKING }

// === Context Source === || WIP
sealed class ContextSource {
    abstract val id: String
    abstract val capturedAt: Long
    abstract val label: String
}
