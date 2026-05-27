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

package me.robin.heion.tts.playback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

@Config(sdk = [33])
class AudioPlayerTest {

    @Test
    fun `test normalization of out-of-bounds audio`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = AudioPlayer(context)
        
        // Generate a 1kHz sine wave at 24kHz sample rate with peak amplitude 2.5f
        val sampleRate = 24000
        val frequency = 1000.0
        val peakAmplitude = 2.5f
        val durationSeconds = 0.01
        val numSamples = (sampleRate * durationSeconds).toInt()
        
        val inputBuffer = FloatArray(numSamples) { i ->
            (peakAmplitude * sin(2.0 * PI * frequency * i / sampleRate)).toFloat()
        }
        
        // Process normalization
        val normalized = player.normalize(inputBuffer)
        
        // Assertion: No values outside [-1.0, 1.0]
        for (sample in normalized) {
            assertTrue(sample in -1.0f..1.0f, "Sample $sample is out of bounds")
        }
        
        // Assertion: Peak is exactly 1.0
        val maxAbs = normalized.maxOf { abs(it) }
        assertEquals(1.0f, maxAbs, 0.0001f, "Peak amplitude should be normalized to 1.0")
    }

    @Test
    fun `test resampling logic 24kHz to 48kHz maintains pitch`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = AudioPlayer(context)
        
        // Input: 4 samples representing a simple cycle at 24kHz
        val input = floatArrayOf(0.0f, 1.0f, 0.0f, -1.0f)
        val fromRate = 24000
        val toRate = 48000
        
        val output = player.resample(input, fromRate, toRate)
        
        // Output should be twice the size
        assertEquals(input.size * 2, output.size, "Output size should be doubled for 2x upsampling")
        
        // Check interpolated values
        // i=0: pos=0.0 -> data[0]=0.0
        // i=1: pos=0.5 -> 0.5 * data[0] + 0.5 * data[1] = 0.5
        // i=2: pos=1.0 -> data[1]=1.0
        // i=3: pos=1.5 -> 0.5 * data[1] + 0.5 * data[2] = 0.5
        // i=4: pos=2.0 -> data[2]=0.0
        // i=5: pos=2.5 -> 0.5 * data[2] + 0.5 * data[3] = -0.5
        // i=6: pos=3.0 -> data[3]=-1.0
        
        val expected = floatArrayOf(0.0f, 0.5f, 1.0f, 0.5f, 0.0f, -0.5f, -1.0f, -1.0f)
        for (i in 0 until 7) {
            assertEquals(expected[i], output[i], 0.0001f, "Sample at index $i is incorrect")
        }
    }
}
