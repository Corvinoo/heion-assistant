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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config

@Config(sdk = [33])
class FastTextLanguageDetectorTest {

    @Test
    fun testLanguageDetection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = FastTextLanguageDetector(context)
        
        // Note: This test might fail in a pure unit test environment if it cannot load the native library or access assets.

        val testText = "Hello world, this is an English sentence."
        val result = detector.detect(testText)
        
        // If native is working, expect a result is expected
        println("LID Result: $result")
    }
}
