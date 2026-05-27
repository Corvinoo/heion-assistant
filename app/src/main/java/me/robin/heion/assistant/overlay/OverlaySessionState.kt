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

package me.robin.heion.assistant.overlay

class OverlaySessionState {

    enum class State {
        COLLAPSED,
        EXPANDED
    }

    var state = State.COLLAPSED

    var isFirstToken = true
    var isGenerating = false
    var isScreenshotPending = false
    var hasAttachedScreenshot = false

    val assistantText = StringBuilder()
    val thoughtText = StringBuilder()
    var isThinking = false
    var thinkingStartTime = 0L
    var thinkingTimeMs: Long? = null

    fun resetAssistantStream() {
        isFirstToken = true
        assistantText.clear()
        thoughtText.clear()
        isThinking = false
        thinkingStartTime = 0L
        thinkingTimeMs = null
    }

    fun clearAttachmentState() {
        isScreenshotPending = false
        hasAttachedScreenshot = false
    }
}