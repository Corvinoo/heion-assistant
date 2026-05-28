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
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class LiteRTBackend(private val context: Context) : InferenceBackend {

    private var engine: Engine? = null
    private var loadedModelName: String = "Unknown"

    override fun getModelName(): String = loadedModelName

    @OptIn(ExperimentalApi::class)
    override fun load(modelPath: String) {

        val actualPath = if (modelPath.isNotEmpty()) {
            if (modelPath.startsWith("content://")) {
                copyUriToInternalStorage(Uri.parse(modelPath))
            } else {
                modelPath
            }
        } else {
            ModelDiscovery.obtainDebugModel(context)?.absolutePath
                ?: ModelDiscovery.obtainDownloadedModel(context)?.absolutePath
                ?: copyBundledModelToInternalStorage()
        }

        loadedModelName = cleanModelName(File(actualPath).name)


        engine = try {
            Log.d(TAG, "Trying engine initialization with GPU")
            val config = EngineConfig(
                modelPath = actualPath,
                backend = Backend.GPU(),
                visionBackend = Backend.CPU(),
                audioBackend = Backend.CPU(),
                maxNumImages = 1,
                cacheDir = context.cacheDir.path
            )
            Engine(config).also { it.initialize() }
        } catch (t: Throwable) {
            Log.w(TAG, "GPU initialization failed, falling back to CPU", t)
            try {
                val config = EngineConfig(
                    modelPath = actualPath,
                    backend = Backend.CPU(),
                    visionBackend = Backend.CPU(),
                    audioBackend = Backend.CPU(),
                    maxNumTokens = 2048,
                    maxNumImages = 1,
                    cacheDir = context.cacheDir.path
                )
                Engine(config).also { it.initialize() }
            } catch (t2: Throwable) {
                Log.e(TAG, "CPU initialization failed as well", t2)
                null
            }
        } ?: throw IllegalStateException(
            "Failed to initialize model from path: $actualPath"
        )

    }

    private fun copyUriToInternalStorage(uri: Uri): String {
        val fileName = getFileName(uri) ?: "custom_model.litertlm"
        val targetFile = File(context.filesDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Failed to open input stream for URI: $uri")

        return targetFile.absolutePath
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun copyBundledModelToInternalStorage(): String {
        val modelName = ModelDiscovery.obtainAssetModel(context)
            ?: throw IllegalStateException("No LiteRT model found in app assets (models/ folder)")

        val targetFile = File(context.filesDir, modelName)
        if (!targetFile.exists() || targetFile.length() == 0L) {
            try {
                context.assets.open("models/$modelName").use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy bundled model $modelName", e)
                throw e
            }
        }
        Log.d(TAG, "Auto-detected bundled model: $modelName")
        return targetFile.absolutePath
    }

    override suspend fun infer(
        systemInstruction: String,
        history: List<ChatTurn>,
        stopSequences: List<String>,
        onToken: (String) -> Unit
    ) {
        val currentEngine = engine ?: return

        withContext(Dispatchers.IO) {
            val initialMessages = history.dropLast(1).map { it.toMessage() }
            val lastTurn = history.lastOrNull() ?: return@withContext

            val config = ConversationConfig(
                systemInstruction = Contents.of(systemInstruction),
                initialMessages = initialMessages
            )

            try {
                val field = config.javaClass.getDeclaredField("enableThinking")
                field.isAccessible = true
                field.set(config, true)
                Log.d(TAG, "Enabled thinking in ConversationConfig via reflection")
            } catch (_: Exception) {
                try {
                    config.javaClass.methods
                        .find { it.name == "setEnableThinking" }
                        ?.invoke(config, true)
                    Log.d(TAG, "Enabled thinking in ConversationConfig via method reflection")
                } catch (_: Exception) {
                }
            }

            currentEngine.createConversation(config).use { conversation ->
                val lastMessage = lastTurn.toMessage()
                
                val fullRawAccumulator = StringBuilder()
                var lastEmittedLength = 0
                var stopDetected = false
                var thoughtClosed = false

                conversation.sendMessageAsync(lastMessage)
                    .collect { message: Message ->
                        ensureActive()
                        if (stopDetected) return@collect

                        val thoughtDelta = try {
                            val channelsField = message.javaClass.getDeclaredField("channels")
                            channelsField.isAccessible = true
                            val channels = channelsField.get(message) as? Map<*, *>
                            channels?.get("thought")?.toString().orEmpty()
                        } catch (_: Exception) {
                            ""
                        }

                        val contentDelta = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }

                        if (thoughtDelta.isNotEmpty()) {
                            if (fullRawAccumulator.isEmpty()) {
                                fullRawAccumulator.append("<|channel>thought\n")
                            }
                            fullRawAccumulator.append(thoughtDelta)
                        }

                        if (contentDelta.isNotEmpty()) {
                            if (!thoughtClosed && fullRawAccumulator.contains("<|channel>thought")) {
                                fullRawAccumulator.append("<channel|>\n")
                                thoughtClosed = true
                            }
                            fullRawAccumulator.append(contentDelta)
                        }

                        val currentTotalText = fullRawAccumulator.toString()

                        for (stopSeq in stopSequences) {
                            val stopIndex = currentTotalText.indexOf(stopSeq)
                            if (stopIndex >= 0) {
                                stopDetected = true
                                val textBeforeStop = currentTotalText.substring(0, stopIndex)
                                val finalDelta = if (textBeforeStop.length > lastEmittedLength) {
                                    textBeforeStop.substring(lastEmittedLength)
                                } else {
                                    ""
                                }
                                if (finalDelta.isNotEmpty()) {
                                    withContext(Dispatchers.Main) { onToken(finalDelta) }
                                }
                                return@collect
                            }
                        }

                        if (currentTotalText.length > lastEmittedLength) {
                            val delta = currentTotalText.substring(lastEmittedLength)
                            withContext(Dispatchers.Main) { onToken(delta) }
                            lastEmittedLength = currentTotalText.length
                        }
                    }
            }
        }
    }

    override fun release() {
        engine?.close()
        engine = null
    }

    private fun ChatTurn.toMessage(): Message {
        return when (this) {
            is ChatTurn.User -> {
                val contents = buildList {
                    if (audioFile != null) {
                        add(Content.AudioFile(audioFile.absolutePath))
                    }
                    if (imageBitmap != null) {
                        add(Content.ImageBytes(imageBitmap.toJpegBytes()))
                    }
                    if (!text.isNullOrBlank()) {
                        add(Content.Text(text))
                    }
                }
                Message.user(Contents.of(contents))
            }
            is ChatTurn.Assistant -> {
                val channels = if (thought != null) mapOf("thought" to thought) else emptyMap()
                Message.model(Contents.of(text ?: ""), emptyList(), channels)
            }
            is ChatTurn.ToolResponse -> {
                Message.tool(Contents.of(result))
            }
            is ChatTurn.System -> Message.system(text)
        }
    }

    companion object {
        private const val TAG = "LiteRTBackend"

        private fun cleanModelName(fileName: String): String {
            return when {
                fileName.contains("E4B", ignoreCase = true) -> "Gemma 4 E4B"
                fileName.contains("E2B", ignoreCase = true) -> "Gemma 4 E2B"
                fileName.contains("gemma", ignoreCase = true) -> "Gemma 4"
                else -> fileName.removeSuffix(".litertlm")
            }
        }
    }
}

private fun Bitmap.toJpegBytes(quality: Int = 95): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return stream.toByteArray()
}
