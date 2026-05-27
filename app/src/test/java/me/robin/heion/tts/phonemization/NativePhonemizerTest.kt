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

import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Config(sdk = [33])
class NativePhonemizerTest {

    @Test
    fun testConcurrentAccess() {
        val executor = Executors.newFixedThreadPool(4)
        
        // This test attempts to trigger the mutex crash or race conditions in the native layer by hammering it from multiple threads.
        repeat(100) {
            executor.submit {
                try {
                    NativePhonemizer.espeakInitialize("/tmp/fake_data")
                    NativePhonemizer.espeakTextToPhonemes("Hello world", "en-us")
                } catch (e: Exception) {
                }
            }
        }
        
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }
}
