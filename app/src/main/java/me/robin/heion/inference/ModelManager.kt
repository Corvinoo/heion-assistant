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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.robin.heion.assistant.profiles.ModelCapability
import me.robin.heion.settings.SettingsRepository

object ModelManager {

    private const val TAG = "ModelManager"

    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var backend: InferenceBackend? = null

    private val loadMutex = Mutex()
    private val inferenceMutex = Mutex()

    private var loadingJob: Deferred<Unit>? = null
    private var releaseJob: Job? = null

    val isLoaded: Boolean
        get() = backend != null

    fun getModelName(): String {
        return backend?.getModelName() ?: "No Model"
    }

    /**
     * Returns the capabilities of the currently loaded model.
     * If no model is loaded, returns an empty set.
     */
    fun currentCapabilities(): Set<ModelCapability> {
        if (!isLoaded) return emptySet()
        return setOf(
            ModelCapability.VISION,
            ModelCapability.THINKING,
            ModelCapability.FUNCTION_CALLING,
            ModelCapability.AUDIO
        )
    }

    suspend fun ensureLoaded(context: Context) {
        cancelScheduledRelease()

        if (backend != null) return

        val appContext = context.applicationContext
        val settings = SettingsRepository(appContext)
        val savedModelPath = settings.getLlmModelPath() ?: ""

        val job = loadMutex.withLock {
            if (backend != null) return
            loadingJob?.let { return@withLock it }

            val newJob = modelScope.async {
                Log.d(TAG, "Loading model: ${if (savedModelPath.isEmpty()) "bundled" else savedModelPath}")

                val newBackend = LiteRTBackend(appContext)
                newBackend.load(savedModelPath)

                backend = newBackend
            }

            loadingJob = newJob
            newJob
        }

        try {
            job.await()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load model", t)
            backend = null
            throw t
        } finally {
            loadMutex.withLock {
                if (loadingJob === job) {
                    loadingJob = null
                }
            }
        }
    }

    suspend fun infer(
        systemInstruction: String,
        history: List<ChatTurn>,
        stopSequences: List<String> = emptyList(),
        onToken: (String) -> Unit
    ) {
        inferenceMutex.withLock {
            val currentBackend = backend
            if (currentBackend == null) {
                onToken("Error: Model not loaded.")
                return
            }

            currentBackend.infer(
                systemInstruction = systemInstruction,
                history = history,
                stopSequences = stopSequences,
                onToken = onToken
            )
            Log.d(TAG, "Inference completed")
        }
    }

    fun release() {
        cancelScheduledRelease()
        modelScope.launch {
            inferenceMutex.withLock {
                backend?.release()
                backend = null
                loadingJob = null
                Log.d(TAG, "Model released")
            }
        }
    }

    fun scheduleRelease(delayMillis: Long) {
        cancelScheduledRelease()
        releaseJob = modelScope.launch {
            Log.d(TAG, "Scheduling model release in ${delayMillis}ms")
            delay(delayMillis)
            release()
        }
    }

    private fun cancelScheduledRelease() {
        releaseJob?.cancel()
        releaseJob = null
    }

}
