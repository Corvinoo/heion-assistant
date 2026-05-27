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

package me.robin.heion.assistant

import android.app.assist.AssistStructure
import android.util.Log

/**
 * Utility to extract text content from the system's AssistStructure
 * This is part of the "context layer" of the assistant
 */
object AssistStructureParser {

    private const val TAG = "AssistStructureParser"

    /**
     * Traverses the AssistStructure and returns a concatenated string of all visible text
     */
    fun extractText(structure: AssistStructure): String {
        val sb = StringBuilder()
        val windowCount = structure.windowNodeCount
        
        Log.d(TAG, "Parsing structure with $windowCount windows")
        
        for (i in 0 until windowCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverse(windowNode.rootViewNode, sb)
        }
        
        return sb.toString().trim()
    }

    private fun traverse(node: AssistStructure.ViewNode, sb: StringBuilder) {
        // Skip nodes that are marked as sensitive (e.g., passwords)
        if (node.isAssistBlocked) return

        // Extract text
        val text = node.text
        if (!text.isNullOrBlank()) {
            sb.append(text).append(" ")
        }

        // Extract content description (useful for images/buttons without text)
        val contentDesc = node.contentDescription
        if (!contentDesc.isNullOrBlank()) {
            sb.append(contentDesc).append(" ")
        }

        // Recursively traverse children
        val childCount = node.childCount
        for (i in 0 until childCount) {
            traverse(node.getChildAt(i), sb)
        }
    }
}
