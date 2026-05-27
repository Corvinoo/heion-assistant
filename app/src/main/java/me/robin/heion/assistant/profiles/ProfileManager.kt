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

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.robin.heion.inference.ModelManager

/**
 * Manages the active profile state and handles switching between profiles.
 */
class ProfileManager(
    private val registry: ProfileRegistry,
    private val prefs: SharedPreferences
) {
    private val _activeProfile = MutableStateFlow<InteractionProfile?>(null)
    val activeProfile: StateFlow<InteractionProfile?> = _activeProfile.asStateFlow()

    private val _pendingProfile = MutableStateFlow<InteractionProfile?>(null)
    val pendingProfile: StateFlow<InteractionProfile?> = _pendingProfile.asStateFlow()

    private val _manualSwitchEvent = MutableStateFlow<Boolean>(false)
    val manualSwitchEvent: StateFlow<Boolean> = _manualSwitchEvent.asStateFlow()

    companion object {
        private const val PREF_ACTIVE_PROFILE = "active_profile_id"
        private const val DEFAULT_PROFILE_ID = "default"
    }

    /**
     * Restores the active profile from persistent storage
     */
    fun restoreFromPrefs() {
        val savedId = prefs.getString(PREF_ACTIVE_PROFILE, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
        val profile = registry.getById(savedId) ?: registry.getById(DEFAULT_PROFILE_ID)
        _activeProfile.value = profile
        _pendingProfile.value = profile
    }

    /**
     * Stages a profile switch. The switch is applied to [activeProfile] only after a request is dispatched
     */
    suspend fun switchTo(profileId: String): SwitchResult {
        val profile = registry.getById(profileId)
            ?: return SwitchResult.Error("Profile not found: $profileId")

        val modelCaps = ModelManager.currentCapabilities()
        val missingCaps = profile.requiredCapabilities - modelCaps
        if (missingCaps.isNotEmpty()) {
            return SwitchResult.IncompatibleModel(missingCaps)
        }

        if (_pendingProfile.value?.id != profileId) {
            _manualSwitchEvent.value = true
        }

        _pendingProfile.value = profile
        // Persistence is updated when the switch is staged
        prefs.edit().putString(PREF_ACTIVE_PROFILE, profileId).apply()
        return SwitchResult.Success
    }

    fun consumeManualSwitchEvent() {
        _manualSwitchEvent.value = false
    }

    /**
     * Commits the pending profile switch. Should be called after an inference request is dispatched.
     */
    fun commitSwitch() {
        _activeProfile.value = _pendingProfile.value
    }

    /**
     * Overrides the current active and pending profile. Used for automatic routing
     * when in "Auto" mode to reflect the selected specialized profile in the UI.
     */
    fun forceSwitch(profileId: String) {
        val profile = registry.getById(profileId) ?: return
        _pendingProfile.value = profile
        _activeProfile.value = profile
    }
}

sealed class SwitchResult {
    object Success : SwitchResult()
    data class Error(val message: String) : SwitchResult()
    data class IncompatibleModel(val missing: Set<ModelCapability>) : SwitchResult()
}
