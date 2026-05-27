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
 * Explain Like I'm 5 profile
 */
object ELI5Profile : InteractionProfile {
    override val id: String = "eli5"
    override val displayName: String = "ELI5"
    override val shortName: String = "ELI5"
    override val iconRes: Int = R.drawable.ic_assistant_spark
    override val description: String = "Explains complex topics in simple terms using analogies."
    override val category: ProfileCategory = ProfileCategory.GENERAL

    override fun buildSystemPrompt(context: ProfileContext): String {
        return """
            Explain topics like I'm five years old.
            No lingo. Use analogies.
            Short sentences, short paragraphs.
            Friendly and warm.
        """.trimIndent()
    }

    override fun preprocessContext(source: ContextSource): ProcessedContext {
        return ProcessedContext("", null, ContextPriority.SUPPLEMENTARY, 2000)
    }

    override fun enabledTools(): Set<String> = setOf("web_search")

    override fun reasoningConfig(): ReasoningConfig = ReasoningConfig(true, ThinkingBudget.MINIMAL, true)

    override fun formatResponse(raw: String, context: ProfileContext): String = raw

    override val defaultVerbosity: Verbosity = Verbosity.CONCISE

    override fun suggestedActions(context: ProfileContext): List<SuggestedAction> {
        return listOf(
            SuggestedAction("Example", "Give me an example"),
            SuggestedAction("More technical", "Now explain it more technically")
        )
    }

    override val memoryPolicy: MemoryPolicy = MemoryPolicy.SESSION_ONLY
    override val requiredCapabilities: Set<ModelCapability> = emptySet()
}
