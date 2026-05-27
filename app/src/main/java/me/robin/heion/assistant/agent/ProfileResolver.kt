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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.profiles.InteractionProfile
import me.robin.heion.assistant.profiles.ProfileManager
import me.robin.heion.assistant.profiles.ProfileRegistry

class ProfileResolver(
    private val profileManager: ProfileManager
) {
    suspend fun resolve(
        userQuery: String,
        onStatusUpdate: (AssistantStatus) -> Unit
    ): InteractionProfile {
        val currentProfile = profileManager.pendingProfile.value
            ?: profileManager.activeProfile.value
            ?: ProfileRegistry.getById("default")
            ?: error("Default profile not found")

        if (currentProfile.id != "auto") {
            profileManager.commitSwitch()
            return currentProfile
        }

        onStatusUpdate(AssistantStatus.Routing)

        val routedProfile = runCatching {
            RoutingEngine.route(userQuery)
        }.getOrNull()

        val fallback = ProfileRegistry.getById("default") ?: currentProfile
        val activeProfile = routedProfile ?: fallback

        if (routedProfile != null) {
            withContext(Dispatchers.Main) {
                profileManager.forceSwitch(routedProfile.id)
            }
        }
        
        profileManager.commitSwitch()
        return activeProfile
    }
}
