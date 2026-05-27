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

data class ToolParameter(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, ToolParameter>,
    val required: List<String>,
    val handler: suspend (Map<String, Any>) -> String
)

object ToolRegistry {

    private val _tools = mutableListOf<ToolDefinition>()
    val tools: List<ToolDefinition> get() = _tools

    fun register(tool: ToolDefinition) {
        if (_tools.none { it.name == tool.name }) {
            _tools.add(tool)
        }
    }

    fun findByName(name: String) = _tools.firstOrNull { it.name == name }

    init {
        register(WebSearchTool.definition)
        
        register(ToolDefinition(
            name = "run_code",
            description = "Execute a Python code snippet and return stdout. Use when the user asks for calculations, data transformations, or scripted actions.",
            parameters = mapOf(
                "code" to ToolParameter("string", "The code to execute"),
                "language" to ToolParameter("string", "Python", enum = listOf("python"))
            ),
            required = listOf("code", "language"),
            handler = { """{"output":"Code execution is simulated."}""" }
        ))
    }
}
