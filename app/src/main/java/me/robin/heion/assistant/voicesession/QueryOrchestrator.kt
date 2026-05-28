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

package me.robin.heion.assistant.voicesession

import android.content.Context
import kotlinx.coroutines.CancellationException
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.agent.AgentLoop
import me.robin.heion.assistant.overlay.OverlayController
import me.robin.heion.inference.ModelManager
import me.robin.heion.nlp.LanguageDetector

class QueryOrchestrator(
    private val context: Context,
    private val agentLoop: AgentLoop,
    private val overlayController: OverlayController,
    private val languageDetector: LanguageDetector
) {
    suspend fun runQuery(
        request: QueryRequest,
        executionContext: QueryExecutionContext,
        onToken: (String) -> Unit = {},
        onStatusUpdate: (AssistantStatus) -> Unit
    ): QueryResult {
        try {
            val initialStatus = if (ModelManager.isLoaded) AssistantStatus.Preparing else AssistantStatus.LoadingModel
            overlayController.setAssistantStatus(initialStatus)
            onStatusUpdate(initialStatus)

            ModelManager.ensureLoaded(context.applicationContext)

            if (initialStatus == AssistantStatus.LoadingModel) {
                overlayController.setAssistantStatus(AssistantStatus.Preparing)
                onStatusUpdate(AssistantStatus.Preparing)
            }

            val output = agentLoop.run(
                userQuery = request.query,
                historyToAppend = executionContext.history,
                screenshot = if (request.includeScreenshot) executionContext.snapshot.screenshot else null,
                audioFile = request.audioFile,
                screenText = if (request.includeScreenText) executionContext.snapshot.screenText else null,
                onToken = onToken,
                onStatusUpdate = { status ->
                    overlayController.setAssistantStatus(status)
                    onStatusUpdate(status)
                }
            )

            // Detect language for TTS
            val langGuess = languageDetector.detect(output)

            return QueryResult.Success(output, langGuess?.languageTag)
        } catch (e: CancellationException) {
            return QueryResult.Cancelled
        } catch (e: Exception) {
            overlayController.appendToken("\n[Error: ${e.message ?: "unknown"}]")
            return QueryResult.Error(e.message ?: "unknown")
        }
    }
}
