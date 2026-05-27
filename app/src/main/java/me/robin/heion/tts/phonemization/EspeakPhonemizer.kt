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

class EspeakPhonemizer(private val dataDir: File) : Phonemizer {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            Log.i("EspeakPhonemizer", "Initializing espeak-ng-data with dir: ${dataDir.absolutePath}")
            val res = NativePhonemizer.espeakInitialize(dataDir.absolutePath)
            if (res >= 0) {
                Log.i("EspeakPhonemizer", "espeak-ng-data initialized successfully (rate: $res)")
                initialized = true
            } else {
                Log.e("EspeakPhonemizer", "Failed to initialize espeak-ng-data: error $res")
                throw RuntimeException("Failed to initialize espeak-ng-data: error $res")
            }
        }
    }

    override fun phonemize(text: String, language: String): PhonemeResult {
        ensureInitialized()
        val voice = when (language.lowercase()) {
            "en", "en-us" -> "en-us"
            "en-gb" -> "en-gb"
            "it" -> "it"
            else -> "en-us"
        }

        // Preserving punctuation and spaces for Kokoro, espeak-ng's IPA output often strips them or joins words

        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            val char = text[i]
            
            // If it's a character that should be handled directly (punctuation/space)
            if (char.isWhitespace() || isKokoroPunctuation(char)) {
                result.append(char)
                i++
                continue
            }
            
            // Otherwise, it's part of a word, finding the word boundary
            val start = i
            while (i < text.length && !text[i].isWhitespace() && !isKokoroPunctuation(text[i])) {
                i++
            }
            val word = text.substring(start, i)
            if (word.isNotEmpty()) {
                val ph = NativePhonemizer.espeakTextToPhonemes(word, voice)
                if (ph != null) {
                    result.append(ph)
                }
            }
        }
            
        return PhonemeResult(result.toString(), language)
    }

    private fun isKokoroPunctuation(c: Char): Boolean {
        // Punctuation characters present in Kokoro vocabulary
        return ";:,.!?—…\"()“”".contains(c)
    }

    override fun supportsLanguage(language: String): Boolean { //todo: expand
        return language.lowercase() in listOf("en", "en-us", "en-gb", "it")
    }
}
