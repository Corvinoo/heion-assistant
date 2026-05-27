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

import me.robin.heion.R

/**
 * The default conversational assistant profile.
 */
object DefaultAssistantProfile : InteractionProfile {

    override val id: String = "default"
    override val displayName: String = "Assistant"
    override val shortName: String = "Assistant"
    override val iconRes: Int = R.drawable.ic_assistant_spark
    override val description: String = "Conversational, balanced assistant for general tasks."
    override val category: ProfileCategory = ProfileCategory.GENERAL

    override fun buildSystemPrompt(context: ProfileContext): String {
        return "";
    }

    override fun preprocessContext(source: ContextSource): ProcessedContext {
        // No special preprocessing for the default profile beyond standard passing
        return ProcessedContext(
            textContent = "", // This will be handled by the fusion pipeline for stubs
            imageBitmap = null,
            priority = ContextPriority.PRIMARY,
            tokenBudget = 4000
        )
    }

    override fun enabledTools(): Set<String> {
        return emptySet()
    }

    override fun reasoningConfig(): ReasoningConfig {
        return ReasoningConfig(
            thinkingEnabled = false,
            thinkingBudget = ThinkingBudget.MINIMAL,
            selfCritiqueEnabled = false
        )
    }

    override fun formatResponse(raw: String, context: ProfileContext): String {
        return raw
    }

    override val defaultVerbosity: Verbosity = Verbosity.BALANCED

    override fun suggestedActions(context: ProfileContext): List<SuggestedAction> {
        // Suggested actions should be dynamically generated in an actual implementation
        if (context.conversationHistory.isEmpty()) {
            return listOf(
                SuggestedAction("Summarize screen", "Can you summarize what's on my screen?"),
                SuggestedAction("Help me write", "Help me write a message based on this screen.")
            )
        }
        return emptyList()
    }

    override val memoryPolicy: MemoryPolicy = MemoryPolicy.SESSION_ONLY

    override val requiredCapabilities: Set<ModelCapability> = emptySet()
}
