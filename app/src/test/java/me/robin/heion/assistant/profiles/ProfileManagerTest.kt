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
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.robin.heion.inference.ModelManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProfileManagerTest {

    private lateinit var registry: ProfileRegistry
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var profileManager: ProfileManager

    private val defaultProfile = mockk<InteractionProfile> {
        every { id } returns "default"
        every { requiredCapabilities } returns emptySet()
    }

    private val specializedProfile = mockk<InteractionProfile> {
        every { id } returns "specialized"
        every { requiredCapabilities } returns setOf(ModelCapability.VISION)
    }

    @BeforeEach
    fun setUp() {
        registry = mockk()
        prefs = mockk(relaxed = true)
        prefsEditor = mockk(relaxed = true)

        every { prefs.edit() } returns prefsEditor
        every { registry.getById(any()) } returns null
        every { registry.getById("default") } returns defaultProfile
        every { registry.getById("specialized") } returns specializedProfile

        mockkObject(ModelManager)
        every { ModelManager.currentCapabilities() } returns emptySet()

        profileManager = ProfileManager(registry, prefs)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `restoreFromPrefs sets active profile from saved id`() = runTest {
        every { prefs.getString("active_profile_id", "default") } returns "specialized"
        
        profileManager.restoreFromPrefs()

        assertEquals("specialized", profileManager.activeProfile.value?.id)
        assertEquals("specialized", profileManager.pendingProfile.value?.id)
    }

    @Test
    fun `restoreFromPrefs defaults to 'default' if saved id not found`() = runTest {
        every { prefs.getString("active_profile_id", "default") } returns "missing"
        
        profileManager.restoreFromPrefs()

        assertEquals("default", profileManager.activeProfile.value?.id)
    }

    @Test
    fun `switchTo compatible profile updates pending and saves to prefs`() = runTest {
        every { ModelManager.currentCapabilities() } returns setOf(ModelCapability.VISION)
        
        val result = profileManager.switchTo("specialized")

        assertTrue(result is SwitchResult.Success)
        assertEquals("specialized", profileManager.pendingProfile.value?.id)
        verify { prefsEditor.putString("active_profile_id", "specialized") }
    }

    @Test
    fun `switchTo incompatible profile returns error and does not update pending`() = runTest {
        every { ModelManager.currentCapabilities() } returns emptySet()
        
        val result = profileManager.switchTo("specialized")

        assertTrue(result is SwitchResult.IncompatibleModel)
        assertEquals(null, profileManager.pendingProfile.value)
    }

    @Test
    fun `commitSwitch updates active profile to pending`() = runTest {
        every { ModelManager.currentCapabilities() } returns setOf(ModelCapability.VISION)
        profileManager.switchTo("specialized")
        
        profileManager.commitSwitch()

        assertEquals("specialized", profileManager.activeProfile.value?.id)
    }

    @Test
    fun `forceSwitch updates both active and pending immediately`() = runTest {
        profileManager.forceSwitch("specialized")

        assertEquals("specialized", profileManager.activeProfile.value?.id)
        assertEquals("specialized", profileManager.pendingProfile.value?.id)
    }

    @Test
    fun `manualSwitchEvent is triggered on switch to different profile`() = runTest {
        profileManager.manualSwitchEvent.test {
            assertEquals(false, awaitItem())
            
            profileManager.switchTo("default")
            assertEquals(true, awaitItem())
            
            profileManager.consumeManualSwitchEvent()
            assertEquals(false, awaitItem())
        }
    }
}
