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

package me.robin.heion.tts.inference

import android.util.Log

/**
 * Tokenizer for Kokoro TTS.
 * Maps IPA phonemes to token IDs based on the Kokoro vocabulary.
 */
object KokoroTokenizer {
    private const val TAG = "KokoroTokenizer"
    
    // Official Kokoro v1.0 vocabulary from its repository config.json
    private val tokenMap = mapOf(
        ';' to 1L, ':' to 2L, ',' to 3L, '.' to 4L, '!' to 5L, '?' to 6L,
        '—' to 9L, '…' to 10L, '"' to 11L, '(' to 12L, ')' to 13L,
        '“' to 14L, '”' to 15L, ' ' to 16L, '\u0303' to 17L, 'ʣ' to 18L,
        'ʥ' to 19L, 'ʦ' to 20L, 'ʨ' to 21L, 'ᵝ' to 22L, '\uAB67' to 23L,
        'A' to 24L, 'I' to 25L, 'O' to 31L, 'Q' to 33L, 'S' to 35L,
        'T' to 36L, 'W' to 39L, 'Y' to 41L, 'ᵊ' to 42L, 'a' to 43L,
        'b' to 44L, 'c' to 45L, 'd' to 46L, 'e' to 47L, 'f' to 48L,
        'h' to 50L, 'i' to 51L, 'j' to 52L, 'k' to 53L, 'l' to 54L,
        'm' to 55L, 'n' to 56L, 'o' to 57L, 'p' to 58L, 'q' to 59L,
        'r' to 60L, 's' to 61L, 't' to 62L, 'u' to 63L, 'v' to 64L,
        'w' to 65L, 'x' to 66L, 'y' to 67L, 'z' to 68L, 'ɑ' to 69L,
        'ɐ' to 70L, 'ɒ' to 71L, 'æ' to 72L, 'β' to 75L, 'ɔ' to 76L,
        'ɕ' to 77L, 'ç' to 78L, 'ɖ' to 80L, 'ð' to 81L, 'ʤ' to 82L,
        'ə' to 83L, 'ɚ' to 85L, 'ɛ' to 86L, 'ɜ' to 87L, 'ɟ' to 90L,
        'ɡ' to 92L, 'ɥ' to 99L, 'ɨ' to 101L, 'ɪ' to 102L, 'ʝ' to 103L,
        'ɯ' to 110L, 'ɰ' to 111L, 'ŋ' to 112L, 'ɳ' to 113L, 'ɲ' to 114L,
        'ɴ' to 115L, 'ø' to 116L, 'ɸ' to 118L, 'θ' to 119L, 'œ' to 120L,
        'ɹ' to 123L, 'ɾ' to 125L, 'ɻ' to 126L, 'ʁ' to 128L, 'ɽ' to 129L,
        'ʂ' to 130L, 'ʃ' to 131L, 'ʈ' to 132L, 'ʧ' to 133L, 'ʊ' to 135L,
        'ʋ' to 136L, 'ʌ' to 138L, 'ɣ' to 139L, 'ɤ' to 140L, 'χ' to 142L,
        'ʎ' to 143L, 'ʒ' to 147L, 'ʔ' to 148L, 'ˈ' to 156L, 'ˌ' to 157L,
        'ː' to 158L, 'ʰ' to 162L, 'ʲ' to 164L, '↓' to 169L, '→' to 171L,
        '↗' to 172L, '↘' to 173L, 'ᵻ' to 177L
    )

    /**
     * Tokenizes a phoneme string into a LongArray of token IDs.
     * Wraps the sequence with pad token 0 at the start and end.
     */
    fun tokenize(phonemes: String): LongArray {
        val tokens = mutableListOf<Long>()
        
        tokens.add(0L)

        for (char in phonemes) {
            val id = tokenMap[char]
            if (id != null) {
                tokens.add(id)
            } else if (char.isWhitespace()) {
                // Map any whitespace (newline, tab, etc) to space (16L)
                if (tokens.lastOrNull() != 16L) {
                    tokens.add(16L)
                }
            } else {
                // Insert a space to maintain rhythm if a character that is definitely not in the vocab is encountered
                Log.w(TAG, "Unknown phoneme character: '$char' (U+${"%04X".format(char.code)}), using space fallback")
                if (tokens.lastOrNull() != 16L) {
                    tokens.add(16L)
                }
            }
        }

        tokens.add(0L)
        
        Log.v(TAG, "Phoneme string: \"$phonemes\"")
        Log.v(TAG, "Generated token IDs: ${tokens.joinToString(", ")}")
        
        return tokens.toLongArray()
    }
}
