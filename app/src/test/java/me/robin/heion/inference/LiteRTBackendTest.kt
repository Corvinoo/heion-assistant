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

package me.robin.heion.inference

import android.content.Context
import android.media.MediaMetadataRetriever
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class LiteRTBackendTest {

    private lateinit var context: Context
    private lateinit var backend: LiteRTBackend

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        backend = LiteRTBackend(context)
        mockkConstructor(MediaMetadataRetriever::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

//    @Test
//    fun `validateAudioDuration throws IllegalArgumentException when duration exceeds 30 seconds`() {
//        val audioFile = File.createTempFile("test_audio", ".mp3")
//
//        every { anyConstructed<MediaMetadataRetriever>().setDataSource(audioFile.absolutePath) } returns Unit
//        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) } returns "31000"
//        every { anyConstructed<MediaMetadataRetriever>().release() } returns Unit
//
//        assertThrows(IllegalArgumentException::class.java) {
//            backend.validateAudioDuration(audioFile)
//        }
//    }
//
//    @Test
//    fun `validateAudioDuration does not throw when duration is within 30 seconds`() {
//        val audioFile = File.createTempFile("test_audio", ".mp3")
//
//        every { anyConstructed<MediaMetadataRetriever>().setDataSource(audioFile.absolutePath) } returns Unit
//        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) } returns "25000"
//        every { anyConstructed<MediaMetadataRetriever>().release() } returns Unit
//
//        backend.validateAudioDuration(audioFile)
//    }
}
