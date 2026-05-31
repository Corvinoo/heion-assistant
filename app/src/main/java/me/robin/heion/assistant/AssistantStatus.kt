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

sealed class AssistantStatus {

    object Idle : AssistantStatus()
    object LoadingModel : AssistantStatus()
    object Routing : AssistantStatus()
    object Preparing : AssistantStatus()
    
    data class Thinking(
        val thought: String = "",
        val timeMs: Long? = null
    ) : AssistantStatus()

    data class Streaming(
        val text: String = "",
        val thought: String? = null,
        val thinkingTimeMs: Long? = null
    ) : AssistantStatus()

    object LookingAtImages : AssistantStatus()
    object SearchingOnline : AssistantStatus()
    object UsingTools : AssistantStatus()

    data class Error(val message: String) : AssistantStatus()

    fun getLabel(): String = when (this) {
        Idle -> ""
        LoadingModel -> "Loading model..."
        Routing -> "Routing to profile..."
        Preparing -> "Preparing to write..."
        is Thinking -> {
            if (timeMs != null) {
                "Thought for ${String.format(java.util.Locale.US, "%.1f", timeMs / 1000f)}s"
            } else {
                "Thinking..."
            }
        }
        is Streaming -> {
            when {
                thinkingTimeMs != null -> "Thought for ${String.format(java.util.Locale.US, "%.1f", thinkingTimeMs / 1000f)}s"
                thought != null -> "Thinking..."
                else -> "Generating..."
            }
        }
        LookingAtImages -> "Looking at image..."
        SearchingOnline -> "Searching online..."
        UsingTools -> "Using tools..."
        is Error -> "Error: $message"
    }
}
