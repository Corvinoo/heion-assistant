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

package me.robin.heion.nlp

import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

// old, unused
class TtsLanguageRouter(
    private val textToSpeech: TextToSpeech,
    private val languageDetector: LanguageDetector
) {
    suspend fun speakDetectedLanguage(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = UUID.randomUUID().toString()
    ) {
        val guess = languageDetector.detect(text)
        val locale = guess?.languageTag?.let { Locale.forLanguageTag(it) }

        val chosenLocale = withContext(Dispatchers.Default) {
            locale?.takeIf { isSupported(it) } ?: Locale.getDefault()
        }

        textToSpeech.setLanguage(chosenLocale)
        textToSpeech.speak(text, queueMode, null, utteranceId)
    }

    private fun isSupported(locale: Locale): Boolean {
        return when (textToSpeech.isLanguageAvailable(locale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> true
            else -> false
        }
    }
}