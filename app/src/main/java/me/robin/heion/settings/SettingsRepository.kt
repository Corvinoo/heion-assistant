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

package me.robin.heion.settings

import android.content.Context
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setModelReleaseTimeout(minutes: Int) {
        prefs.edit()
            .putInt(KEY_MODEL_RELEASE_TIMEOUT, minutes)
            .apply()
    }

    fun getModelReleaseTimeout(): Int {
        return prefs.getInt(KEY_MODEL_RELEASE_TIMEOUT, 0)
    }

    fun saveLlmModelPath(path: String?) {
        prefs.edit()
            .putString(KEY_LLM_MODEL_PATH, path)
            .apply()
    }

    fun getLlmModelPath(): String? {
        return prefs.getString(KEY_LLM_MODEL_PATH, null)
    }

    fun saveTtsModelPath(path: String?) {
        prefs.edit()
            .putString(KEY_TTS_MODEL_PATH, path)
            .apply()
    }

    fun getTtsModelPath(): String? {
        return prefs.getString(KEY_TTS_MODEL_PATH, null)
    }

    fun setSpeculativeDecoding(isChecked: Boolean) {
        prefs.edit {
            putBoolean(SPECULATIVE_DECODING, isChecked)
        }
    }

    fun isTtsEnabled(): Boolean {
        return prefs.getBoolean(KEY_TTS_ENABLED, true)
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_TTS_ENABLED, enabled)
        }
    }

    fun hasAcceptedDisclaimer(): Boolean {
        return prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }

    fun setAcceptedDisclaimer(accepted: Boolean) {
        prefs.edit {
            putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted)
        }
    }

    fun getAcceptedDisclaimerHash(): String? {
        return prefs.getString(KEY_DISCLAIMER_HASH, null)
    }

    fun setAcceptedDisclaimerHash(hash: String) {
        prefs.edit {
            putString(KEY_DISCLAIMER_HASH, hash)
        }
    }

    fun hasAcceptedLibraryDisclaimer(): Boolean {
        return prefs.getBoolean(KEY_LIBRARY_DISCLAIMER_ACCEPTED, false)
    }

    fun setAcceptedLibraryDisclaimer(accepted: Boolean) {
        prefs.edit {
            putBoolean(KEY_LIBRARY_DISCLAIMER_ACCEPTED, accepted)
        }
    }

    fun getAcceptedLibraryDisclaimerHash(): String? {
        return prefs.getString(KEY_LIBRARY_DISCLAIMER_HASH, null)
    }

    fun setAcceptedLibraryDisclaimerHash(hash: String) {
        prefs.edit {
            putString(KEY_LIBRARY_DISCLAIMER_HASH, hash)
        }
    }

    companion object {
        private const val PREFS_NAME = "local_assistant"
        private const val KEY_LLM_MODEL_PATH = "llm_model_path"
        private const val KEY_TTS_MODEL_PATH = "tts_model_path"
        private const val KEY_MODEL_RELEASE_TIMEOUT = "model_release_timeout"

        private const val SPECULATIVE_DECODING = "speculative_decoding"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_DISCLAIMER_HASH = "disclaimer_hash"

        private const val KEY_LIBRARY_DISCLAIMER_ACCEPTED = "library_disclaimer_accepted"
        private const val KEY_LIBRARY_DISCLAIMER_HASH = "library_disclaimer_hash"
    }
}
