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

package me.robin.heion.tts.phonemization

import android.util.Log
import java.io.File

// WIP
class OpenJTalkPhonemizer(
    private val dictDir: File,
    private val voiceFile: File
) : Phonemizer {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            if (!dictDir.exists() || !voiceFile.exists()) {
                // If files are missing, we don't crash, but we stay uninitialized or return empty
                Log.w("OpenJTalkPhonemizer", "OpenJTalk files missing: ${dictDir.absolutePath} or ${voiceFile.absolutePath}")
            }

            val res = NativePhonemizer.openJTalkInitialize(
                dictDir.absolutePath,
                voiceFile.absolutePath
            )
            if (res == 0) {
                initialized = true
            } else {
                throw RuntimeException("Failed to initialize OpenJTalk: error $res")
            }
        }
    }

    override fun phonemize(text: String, language: String): PhonemeResult {
        ensureInitialized()
        val phonemes = NativePhonemizer.openJTalkTextToPhonemes(text)
            ?: throw RuntimeException("OpenJTalk phonemization failed for: $text")
            
        return PhonemeResult(phonemes, "ja")
    }

    override fun supportsLanguage(language: String): Boolean {
        return language.lowercase() == "ja" || language.lowercase() == "jp"
    }
}
