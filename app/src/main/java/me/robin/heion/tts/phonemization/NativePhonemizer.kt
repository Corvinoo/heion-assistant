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

import androidx.annotation.Keep
import android.util.Log

/**
 * JNI Bridge for espeak-ng-data and OpenJTalk
 */
@Keep
object NativePhonemizer {
    private const val TAG = "NativePhonemizer"

    init {
        try {
            System.loadLibrary("phonemizer_jni")
            Log.i(TAG, "Native phonemizer library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native phonemizer library", e)
        }
    }


    // Initializes espeak-ng-data with the given data path
    external fun espeakInitialize(dataPath: String): Int

    // Converts text to IPA phonemes using espeak-ng-data
    external fun espeakTextToPhonemes(text: String, voice: String): String?

    // Initializes OpenJTalk with dictionary and voice paths || WIP
    external fun openJTalkInitialize(dictPath: String, voicePath: String): Int

    // Converts Japanese text to phonetic labels using OpenJTalk || WIP
    external fun openJTalkTextToPhonemes(text: String): String?

    // Releases native resources
    external fun release()
}
