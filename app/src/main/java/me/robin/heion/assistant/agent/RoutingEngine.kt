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

package me.robin.heion.assistant.agent

import me.robin.heion.assistant.profiles.InteractionProfile
import me.robin.heion.assistant.profiles.ProfileRegistry

/**
 * RoutingEngine handles the preliminary profile matching when the user is in "auto" mode.
 * It uses a set of matchers to decide which profile is best suited for the query.
 */
object RoutingEngine {

    private val matchers = listOf(
        // Translator patterns
        ProfileMatcher("translator", listOf(
            Regex("(?i).*translate.*"),
            Regex("(?i).*in.*english.*"),
            Regex("(?i).*what.*does.*mean.*")
        )),
        // ELI5 patterns
        ProfileMatcher("eli5", listOf(
            Regex("(?i).*explain.*like.*i'm.*5.*"),
            Regex("(?i).*simple.*explanation.*"),
            Regex("(?i).*ELI5.*")
        )),
        // Screen Analysis patterns
        ProfileMatcher("screen_analysis", listOf(
            Regex("(?i).*analyze.*screen.*"),
            Regex("(?i).*what.*am.*i.*looking.*at.*"),
            Regex("(?i).*summarize.*this.*page.*")
        )),
        ProfileMatcher("dictionary", listOf(
            Regex("(?i).*meaning.*"),
            Regex("(?i).*define.*"),
            Regex("(?i)^\\w+$")
        ))
    )

    fun route(query: String): InteractionProfile? {
        val profileId = matchers.firstOrNull { it.matches(query) }?.profileId
        return profileId?.let { ProfileRegistry.getById(it) }
    }

    private data class ProfileMatcher(
        val profileId: String,
        val patterns: List<Regex>
    ) {
        fun matches(query: String): Boolean = patterns.any { it.containsMatchIn(query) }
    }
}
