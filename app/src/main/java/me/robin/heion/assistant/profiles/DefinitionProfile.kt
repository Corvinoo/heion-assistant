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

object DefinitionProfile : InteractionProfile {
    override val id: String = "dictionary"
    override val displayName: String = "Dictionary"
    override val shortName: String = "Dictionary"
    override val iconRes: Int = R.drawable.ic_assistant_spark
    override val description: String = "A dictionary for definitions"
    override val category: ProfileCategory = ProfileCategory.UTILITY

    override fun buildSystemPrompt(context: ProfileContext): String {
        return """
            You are an a multilingual dictionary.
            ALWAYS structure your answer like an online dictionary.
            You are NOT conversational, treat every user input like a dictionary query.
        """.trimIndent()
    }


    override fun preprocessContext(source: ContextSource): ProcessedContext {
        return ProcessedContext(
            textContent = "",
            imageBitmap = null,
            priority = ContextPriority.PRIMARY,
            tokenBudget = 2000
        )
    }

    override fun enabledTools(): Set<String> {
        return setOf("web_search")
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
        return emptyList()
    }

    override val memoryPolicy: MemoryPolicy = MemoryPolicy.SESSION_ONLY

    override val requiredCapabilities: Set<ModelCapability> = emptySet()
}