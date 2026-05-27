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

package me.robin.heion.assistant.repository

import android.graphics.Bitmap
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.profiles.ConversationMessage

object MessageFactory {
    fun createUserMessage(
        text: String,
        image: Bitmap? = null,
        audioDurationMs: Long? = null,
        profileId: String? = null
    ) = ConversationMessage.User(
        text = text,
        image = image,
        audioDurationMs = audioDurationMs,
        profileId = profileId
    )

    fun createModelMessage(
        text: String = "",
        status: AssistantStatus = AssistantStatus.Idle,
        thinkingText: String? = null,
        thinkingTimeMs: Long? = null,
        modelName: String? = null,
        profileId: String? = null
    ) = ConversationMessage.Model(
        text = text,
        status = status,
        thinkingText = thinkingText,
        thinkingTimeMs = thinkingTimeMs,
        modelName = modelName,
        profileId = profileId
    )
}
