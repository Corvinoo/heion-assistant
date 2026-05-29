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

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.robin.heion.databinding.ActivityModelLibraryBinding
import me.robin.heion.databinding.ItemModelBinding
import me.robin.heion.databinding.ItemModelHeaderBinding
import me.robin.heion.settings.SettingsRepository
import java.io.File
import java.security.MessageDigest

class ModelLibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelLibraryBinding
    private lateinit var adapter: ModelAdapter
    private lateinit var settings: SettingsRepository

    private val selectModelLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                importModel(it)
            }
        }
    }

    private suspend fun importModel(uri: Uri) {
        val fileName = getFileName(uri) ?: "imported_model"
        val modelsDir = File(getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        
        val targetFile = File(modelsDir, fileName)
        
        binding.importProgressOverlay.visibility = View.VISIBLE
        binding.btnImport.isEnabled = false

        try {
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            if (fileName.endsWith(".litertlm", ignoreCase = true)) {
                settings.saveLlmModelPath(targetFile.absolutePath)
                ModelManager.release()
            } else if (fileName.endsWith(".onnx", ignoreCase = true)) {
                settings.saveTtsModelPath(targetFile.absolutePath)
            }
            
            refreshList()
        } catch (e: Exception) {
            Log.e("ModelLibrary", "Failed to import model", e)
        } finally {
            binding.importProgressOverlay.visibility = View.GONE
            binding.btnImport.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(this)
        binding = ActivityModelLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkLibraryDisclaimer()

        adapter = ModelAdapter(
            onDownload = { model -> ModelDownloadManager.startDownload(this, model) },
            onDelete = { model -> 
                ModelDownloadManager.deleteModel(this, model)
                refreshList()
            },
            onSelect = { model ->
                val modelsDir = File(getExternalFilesDir(null), "models")
                val path = File(modelsDir, model.fileName).absolutePath
                if (model.type == ModelType.LITERT_LLM) {
                    settings.saveLlmModelPath(path)
                    ModelManager.release()
                } else {
                    settings.saveTtsModelPath(path)
                }
                refreshList()
            }
        )

        binding.rvModels.layoutManager = LinearLayoutManager(this)
        binding.rvModels.adapter = adapter

        binding.btnImport.setOnClickListener {
            selectModelLauncher.launch(arrayOf("*/*"))
        }

        refreshList()

        // Observe progress for all downloadable models
        ModelDownloadManager.getDownloadableModels(this).forEach { model ->
            lifecycleScope.launch {
                ModelDownloadManager.getDownloadWorkInfo(this@ModelLibraryActivity, model.id)
                    .collectLatest { workInfo ->
                        adapter.updateWorkInfo(model.id, workInfo)
                    }
            }
        }
    }

    private fun checkLibraryDisclaimer() {
        val currentHash = calculateHash(LIBRARY_DISCLAIMER)
        val acceptedHash = settings.getAcceptedLibraryDisclaimerHash()

        if (!settings.hasAcceptedLibraryDisclaimer() || currentHash != acceptedHash) {
            showDisclaimerDialog(currentHash)
        }
    }

    private fun calculateHash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showDisclaimerDialog(newHash: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Hardware Compatibility")
        builder.setMessage(LIBRARY_DISCLAIMER)
        builder.setCancelable(false)
        builder.setPositiveButton("I understand") { _, _ ->
            settings.setAcceptedLibraryDisclaimer(true)
            settings.setAcceptedLibraryDisclaimerHash(newHash)
        }
        builder.setNegativeButton("Go Back") { _, _ ->
            finish()
        }
        builder.show()
    }

    private fun refreshList() {
        val currentLlmPath = settings.getLlmModelPath() ?: ""
        val currentTtsPath = settings.getTtsModelPath() ?: ""
        val downloadable = ModelDownloadManager.getDownloadableModels(this)
        val items = mutableListOf<LibraryItem>()
        
        val modelsDir = File(getExternalFilesDir(null), "models")
        val filesInDir = modelsDir.listFiles()?.toList() ?: emptyList()

        // LiteRT Models
        items.add(LibraryItem.Header("LiteRT Models (LLM)"))
        
        downloadable.filter { it.type == ModelType.LITERT_LLM }.forEach { model ->
            val isActive = currentLlmPath.contains(model.fileName)
            items.add(LibraryItem.Model(model, isActive))
        }

        filesInDir.filter { file -> 
            file.extension.equals("litertlm", ignoreCase = true) && 
            !downloadable.any { it.fileName == file.name }
        }.forEach { file ->
            val isActive = currentLlmPath == file.absolutePath
            items.add(LibraryItem.CustomModel(file.name, file.absolutePath, ModelType.LITERT_LLM, isActive))
        }

        // ONNX Models
        items.add(LibraryItem.Header("ONNX Models (TTS)"))
        
        downloadable.filter { it.type == ModelType.ONNX_TTS }.forEach { model ->
            val isActive = currentTtsPath.contains(model.fileName)
            items.add(LibraryItem.Model(model, isActive))
        }

        filesInDir.filter { file -> 
            file.extension.equals("onnx", ignoreCase = true) && 
            !downloadable.any { it.fileName == file.name }
        }.forEach { file ->
            val isActive = currentTtsPath == file.absolutePath
            items.add(LibraryItem.CustomModel(file.name, file.absolutePath, ModelType.ONNX_TTS, isActive))
        }

        adapter.submitList(items)

        // Fetch sizes in background for models that don't have them
        downloadable.forEach { model ->
            if (model.sizeBytes == 0L) {
                lifecycleScope.launch {
                    ModelDownloadManager.fetchModelSize(model)
                    adapter.notifyModelChanged(model.id)
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
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

    sealed class LibraryItem {
        data class Header(val title: String) : LibraryItem()
        data class Model(val info: ModelInfo, val isActive: Boolean) : LibraryItem()
        data class CustomModel(val name: String, val path: String, val type: ModelType, val isActive: Boolean) : LibraryItem()
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_MODEL = 1
        const val TYPE_MANUAL = 2

        const val LIBRARY_DISCLAIMER = "Running LLMs locally can demand significant processing power." +
                "\nBefore downloading or importing them, please verify your device meets their minimum requirement to run."
    }

    inner class ModelAdapter(
        private val onDownload: (ModelInfo) -> Unit,
        private val onDelete: (ModelInfo) -> Unit,
        private val onSelect: (ModelInfo) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<LibraryItem> = emptyList()
        private val workInfos = mutableMapOf<String, WorkInfo?>()

        fun submitList(list: List<LibraryItem>) {
            items = list
            notifyDataSetChanged()
        }

        fun updateWorkInfo(modelId: String, workInfo: WorkInfo?) {
            workInfos[modelId] = workInfo
            notifyModelChanged(modelId)
        }

        fun notifyModelChanged(modelId: String) {
            val index = items.indexOfFirst { it is LibraryItem.Model && it.info.id == modelId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is LibraryItem.Header -> TYPE_HEADER
            is LibraryItem.Model -> TYPE_MODEL
            is LibraryItem.CustomModel -> TYPE_MANUAL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_HEADER -> HeaderViewHolder(ItemModelHeaderBinding.inflate(inflater, parent, false))
                TYPE_MODEL -> ModelViewHolder(ItemModelBinding.inflate(inflater, parent, false))
                else -> ManualViewHolder(ItemModelBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is LibraryItem.Header -> (holder as HeaderViewHolder).binding.txtHeader.text = item.title
                is LibraryItem.Model -> {
                    val vh = holder as ModelViewHolder
                    val model = item.info
                    val workInfo = workInfos[model.id]
                    val isReady = ModelDownloadManager.isModelReady(holder.itemView.context, model)
                    
                    vh.binding.txtModelName.text = model.name
                    vh.binding.txtActiveBadge.visibility = if (item.isActive && isReady) View.VISIBLE else View.GONE
                    
                    val sizeStr = if (model.sizeBytes > 0) ModelDownloadManager.formatFileSize(model.sizeBytes) else "Fetching size..."
                    vh.binding.txtModelInfo.text = "File: ${model.fileName} • $sizeStr"

                    when {
                        workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED -> {
                            vh.binding.btnAction.text = "Downloading"
                            vh.binding.btnAction.isEnabled = false
                            vh.binding.btnAction.setTextColor(Color.GRAY)
                            vh.binding.progressModel.visibility = View.VISIBLE
                            val progress = workInfo.progress.getInt("progress", 0)
                            vh.binding.progressModel.progress = progress
                        }
                        isReady -> {
                            vh.binding.progressModel.visibility = View.GONE
                            if (item.isActive) {
                                vh.binding.btnAction.text = "Delete"
                                vh.binding.btnAction.isEnabled = true
                                vh.binding.btnAction.setTextColor(Color.RED)
                                vh.binding.btnAction.setOnClickListener { onDelete(model) }
                            } else {
                                vh.binding.btnAction.text = "Select"
                                vh.binding.btnAction.isEnabled = true
                                vh.binding.btnAction.setTextColor(Color.BLUE)
                                vh.binding.btnAction.setOnClickListener { onSelect(model) }
                            }
                        }
                        else -> {
                            vh.binding.btnAction.text = "Download"
                            vh.binding.btnAction.isEnabled = true
                            vh.binding.btnAction.setTextColor(Color.BLUE)
                            vh.binding.progressModel.visibility = View.GONE
                            vh.binding.btnAction.setOnClickListener { onDownload(model) }
                        }
                    }
                }
                is LibraryItem.CustomModel -> {
                    val vh = holder as ManualViewHolder
                    vh.binding.txtModelName.text = item.name
                    vh.binding.txtActiveBadge.visibility = if (item.isActive) View.VISIBLE else View.GONE
                    
                    val file = File(item.path)
                    val sizeStr = ModelDownloadManager.formatFileSize(file.length())
                    vh.binding.txtModelInfo.text = "Custom Import • $sizeStr"
                    
                    if (item.isActive) {
                        vh.binding.btnAction.text = "Delete"
                        vh.binding.btnAction.setTextColor(Color.RED)
                        vh.binding.btnAction.setOnClickListener {
                            if (item.type == ModelType.LITERT_LLM) {
                                settings.saveLlmModelPath(null)
                                ModelManager.release()
                            } else {
                                settings.saveTtsModelPath(null)
                            }
                            file.delete()
                            refreshList()
                        }
                    } else {
                        vh.binding.btnAction.text = "Select"
                        vh.binding.btnAction.setTextColor(Color.BLUE)
                        vh.binding.btnAction.setOnClickListener {
                            if (item.type == ModelType.LITERT_LLM) {
                                settings.saveLlmModelPath(item.path)
                                ModelManager.release()
                            } else {
                                settings.saveTtsModelPath(item.path)
                            }
                            refreshList()
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        inner class HeaderViewHolder(val binding: ItemModelHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        inner class ModelViewHolder(val binding: ItemModelBinding) : RecyclerView.ViewHolder(binding.root)
        inner class ManualViewHolder(val binding: ItemModelBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
