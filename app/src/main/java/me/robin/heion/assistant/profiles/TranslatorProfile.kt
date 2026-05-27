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
 * Translator profile
 */
object TranslatorProfile : InteractionProfile {
    override val id: String = "translator"
    override val displayName: String = "Translate"
    override val shortName: String = "Translate"
    override val iconRes: Int = R.drawable.ic_assistant_spark
    override val description: String = "Minimal overhead, high fidelity translation."
    override val category: ProfileCategory = ProfileCategory.LANGUAGE

    override fun buildSystemPrompt(context: ProfileContext): String {
        return """
            You are a professional English Translator. 
            Detect source language automatically.
            Provide translation only, no preamble.
            If multiple interpretations exist, present them as numbered alternatives.
        """.trimIndent()
    }

    override fun preprocessContext(source: ContextSource): ProcessedContext {
        return ProcessedContext("", null, ContextPriority.PRIMARY, 3000)
    }

    override fun enabledTools(): Set<String> = emptySet()

    override fun reasoningConfig(): ReasoningConfig = ReasoningConfig(false, ThinkingBudget.MINIMAL, false)

    override fun formatResponse(raw: String, context: ProfileContext): String = raw

    override val defaultVerbosity: Verbosity = Verbosity.CONCISE

    override fun suggestedActions(context: ProfileContext): List<SuggestedAction> {
        return listOf(
            SuggestedAction("More formal", "Make it more formal"),
            SuggestedAction("More casual", "Make it more casual")
        )
    }

    override val memoryPolicy: MemoryPolicy = MemoryPolicy.SESSION_ONLY
    override val requiredCapabilities: Set<ModelCapability> = emptySet()
}
