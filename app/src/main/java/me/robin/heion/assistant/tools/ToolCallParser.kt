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

package me.robin.heion.assistant.tools

import org.json.JSONException
import org.json.JSONObject

object ToolCallParser {

    // LLM emits this exact pattern when requesting a tool
    private val TOOL_CALL_REGEX = Regex(
        "<\\|tool_call>(.*?)<tool_call\\|>",
        RegexOption.DOT_MATCHES_ALL
    )

    private const val STR_DELIM = "<|\"|>"

    data class ParsedToolCall(val name: String, val args: Map<String, Any>)

    fun detect(modelOutput: String): ParsedToolCall? {
        val match = TOOL_CALL_REGEX.find(modelOutput) ?: return null
        
        // LLM uses <|""|> as a string delimiter inside JSON
        val rawJson = match.groupValues[1].trim()
        val sanitizedJson = rawJson.replace(STR_DELIM, "\"")
        
        return try {
            val obj = JSONObject(sanitizedJson)
            val name = obj.getString("name")
            val argsObj = obj.optJSONObject("args") ?: JSONObject()
            val args = mutableMapOf<String, Any>()
            argsObj.keys().forEach { k ->
                args[k] = argsObj.get(k)
            }
            ParsedToolCall(name, args)
        } catch (e: JSONException) {
            null // model generated malformed JSON
        }
    }

    fun isToolCall(modelOutput: String) = TOOL_CALL_REGEX.containsMatchIn(modelOutput)
    
    fun stripToolCall(modelOutput: String): String {
        return modelOutput.replace(TOOL_CALL_REGEX, "").trim()
    }
}
