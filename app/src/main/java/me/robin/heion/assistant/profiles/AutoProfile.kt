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


object AutoProfile : InteractionProfile {

    override val id: String = "auto"
    override val displayName: String = "Auto"
    override val shortName: String = "Auto"
    override val iconRes: Int = R.drawable.ic_assistant_spark
    override val description: String = "Automatically selects the best profile for your request."
    override val category: ProfileCategory = ProfileCategory.GENERAL

    override fun buildSystemPrompt(context: ProfileContext): String {
        // This is a placeholder. The AgentLoop performs routing before building the prompt.
        // If it somehow falls through, it behaves like the default assistant.
        return DefaultAssistantProfile.buildSystemPrompt(context)
    }

    override fun preprocessContext(source: ContextSource): ProcessedContext {
        return DefaultAssistantProfile.preprocessContext(source)
    }

    override fun enabledTools(): Set<String> {
        return DefaultAssistantProfile.enabledTools()
    }

    override fun reasoningConfig(): ReasoningConfig {
        return DefaultAssistantProfile.reasoningConfig()
    }

    override fun formatResponse(raw: String, context: ProfileContext): String {
        return raw
    }

    override val defaultVerbosity: Verbosity = Verbosity.BALANCED

    override fun suggestedActions(context: ProfileContext): List<SuggestedAction> {
        return DefaultAssistantProfile.suggestedActions(context)
    }

    override val memoryPolicy: MemoryPolicy = MemoryPolicy.SESSION_ONLY

    override val requiredCapabilities: Set<ModelCapability> = emptySet()
}
