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

import android.graphics.Bitmap
import java.io.File

sealed class ChatTurn {

    data class System(
        val text: String
    ) : ChatTurn()

    data class User(
        val text: String? = null,
        val audioFile: File? = null,
        val imageBitmap: Bitmap? = null
    ) : ChatTurn()

    data class Assistant(
        val text: String? = null,
        val thought: String? = null,
        val toolCalls: List<ToolCallInfo>? = null,
        val modelName: String? = null
    ) : ChatTurn()

    data class ToolResponse(
        val name: String,
        val result: String
    ) : ChatTurn()
}

data class ToolCallInfo(
    val name: String,
    val arguments: Map<String, Any?>
)
