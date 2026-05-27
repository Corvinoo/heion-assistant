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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * WebSearchTool provides a structured way to search the web for RAG || WIP
 */
object WebSearchTool {

    val definition = ToolDefinition(
        name = "web_search",
        description = "Search the web for current information. Use ONLY when the user asks about facts, news, or anything that may have changed recently.",
        parameters = mapOf(
            "query" to ToolParameter("string", "The exact search query string")
        ),
        required = listOf("query"),
        handler = { args ->
            val query = args["query"] as? String ?: ""
            execute(query)
        }
    )

    /**
     * Executes the search and returns a JSON string of results.
     */
    suspend fun execute(query: String): String = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext error("Empty query")
        
        // Preliminary implementation: returning a placeholder or simple simulated result
        // Real scraping logic will be integrated later.
        
        try {
            // Simulated results for now
            val results = JSONArray().apply {
                put(JSONObject().apply {
                    put("title", "Simulated Search Result for '$query'")
                    put("url", "https://example.com")
                    put("snippet", "This is a placeholder result for the web search tool.")
                })
            }
            
            JSONObject().put("results", results).toString()
        } catch (e: Exception) {
            error(e.message ?: "Unknown error")
        }
    }

    private fun error(message: String): String {
        return JSONObject().put("error", message).toString()
    }
}
