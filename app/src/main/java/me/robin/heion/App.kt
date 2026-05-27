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

package me.robin.heion

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import me.robin.heion.assistant.profiles.AutoProfile
import me.robin.heion.assistant.profiles.DefaultAssistantProfile
import me.robin.heion.assistant.profiles.DefinitionProfile
import me.robin.heion.assistant.profiles.ELI5Profile
import me.robin.heion.assistant.profiles.ProfileRegistry
import me.robin.heion.assistant.profiles.TranslatorProfile

import me.robin.heion.di.AppContainer

class App : Application(), ComponentCallbacks2 {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        Log.d("LocalAssistant", "Application started")

        // Register built-in profiles || todo: implement automatic registration
        ProfileRegistry.register(AutoProfile)
        ProfileRegistry.register(DefaultAssistantProfile)
        ProfileRegistry.register(TranslatorProfile)
        ProfileRegistry.register(ELI5Profile)
        ProfileRegistry.register(DefinitionProfile)

        appContainer = AppContainer(this)
    }

    //todo: further implementation needed
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                Log.w("LocalAssistant", "Critical memory pressure")
            }

            TRIM_MEMORY_MODERATE -> {
                Log.d("LocalAssistant", "Moderate memory pressure")
            }
        }
    }
}
