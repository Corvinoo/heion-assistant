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

package me.robin.heion.assistant.overlay

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.robin.heion.assistant.profiles.ConversationMessage
import me.robin.heion.assistant.profiles.ProfileManager
import me.robin.heion.assistant.repository.ConversationRepository
import me.robin.heion.databinding.OverlayMainBinding

class OverlayCoordinator(
    private val renderer: OverlayRenderer,
    private val binding: OverlayMainBinding,
    private val profileManager: ProfileManager,
    private val conversationRepository: ConversationRepository,
    private val onQuery: (String, Boolean, Boolean) -> Unit,
    private val onStop: () -> Unit,
    private val onDismiss: () -> Unit,
    private val onSetFocusable: (Boolean) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    var isScreenshotPending = false
    var hasAttachedScreenshot = false
    var isGenerating = false
    private var isModeLocked = false

    private var onVoiceModeDisabled: (() -> Unit)? = null
    private var autoScrollScheduled = false
    private var userIsAtBottom = true
    private var isUserDragging = false

    private val autoScrollRunnable = Runnable {
        autoScrollScheduled = false
        if (!isUserDragging && userIsAtBottom) {
            renderer.scrollToBottom()
        }
    }

    fun initialize() {
        setupViews()
        renderer.setupWindowInsets(renderer.getRootView())
        renderer.updateMultimodalButtonsUI(isScreenshotPending, hasAttachedScreenshot)
        onSetFocusable(false)
        
        renderer.adapter.onThoughtExpansionToggled = { index, expanded ->
            conversationRepository.updateMessageAtIndex(index) { msg ->
                if (msg is ConversationMessage.Model) {
                    msg.copy(isThoughtExpanded = expanded)
                } else msg
            }
        }

        observeMessages()
        observeProfileChanges()
    }

    private fun observeMessages() {
        scope.launch {
            conversationRepository.messages.collectLatest { messages ->
                renderer.adapter.setMessages(messages)
                scheduleAutoScroll()
            }
        }
    }

    private fun observeProfileChanges() {
        scope.launch {
            profileManager.manualSwitchEvent.collect { isManual ->
                if (isManual) {
                    conversationRepository.clear()
                    profileManager.consumeManualSwitchEvent()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        with(binding) {
            profileChip.setProfileManager(profileManager)
            btnMultimodal.setIcons(me.robin.heion.R.drawable.ic_screen_search, android.R.drawable.ic_menu_camera)

            btnMultimodal.onModeChangedListener = { isPrimary ->
                if (!isPrimary) {
                    android.widget.Toast.makeText(binding.root.context, "Camera mode stub", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            btnMultimodal.setOnClickListener {
                if (btnMultimodal.isPrimaryMode()) {
                    isScreenshotPending = !isScreenshotPending
                    renderer.updateMultimodalButtonsUI(isScreenshotPending, hasAttachedScreenshot)
                } else {
                    // Camera mode action stub
                    android.util.Log.d("OverlayCoordinator", "Camera mode clicked")
                    if (!isModeLocked) {
                        lockMode()
                    }
                }
            }

            mainInput.setOnTouchListener { _, _ ->
                onSetFocusable(true)
                expand()
                false
            }

            mainInput.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                    submitQuery(v.text.toString())
                    renderer.hideKeyboard(v)
                    true
                } else false
            }

            btnSend.setOnClickListener {
                if (isGenerating && mainInput.text.isNullOrEmpty()) {
                    onStop()
                } else {
                    submitQuery(mainInput.text.toString())
                    renderer.hideKeyboard(it)
                }
            }

            mainInput.doOnTextChanged { text, _, _, _ ->
                renderer.updateSendButtonIcon(isGenerating, text.isNullOrEmpty())
                if (!text.isNullOrEmpty()) {
                    onVoiceModeDisabled?.invoke()
                }
            }

            scrimView.setOnClickListener {
                dismiss()
            }

            messageList.layoutManager = LinearLayoutManager(binding.root.context)
            messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    userIsAtBottom = isAtBottom()
                }
            })
            
            // Restore addOnItemTouchListener behavior
            messageList.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            isUserDragging = true
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            isUserDragging = false
                            userIsAtBottom = isAtBottom()
                        }
                    }
                    return false
                }
            })
        }
    }

    private fun isAtBottom(): Boolean {
        val lm = binding.messageList.layoutManager as? LinearLayoutManager ?: return true
        val lastIndex = renderer.adapter.itemCount - 1
        return lastIndex < 0 || lm.findLastCompletelyVisibleItemPosition() >= lastIndex
    }

    private fun scheduleAutoScroll() {
        if (!userIsAtBottom || isUserDragging || autoScrollScheduled) return
        autoScrollScheduled = true
        binding.messageList.removeCallbacks(autoScrollRunnable)
        binding.messageList.post(autoScrollRunnable)
    }

    fun submitQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        expand()
        
        if (!isModeLocked) {
            lockMode()
        }

        conversationRepository.addMessage(ConversationMessage.User(trimmed))
        
        val includeScreenshot = isScreenshotPending
        onQuery(trimmed, includeScreenshot, false)

        if (includeScreenshot) {
            isScreenshotPending = false
            hasAttachedScreenshot = true
        }
        renderer.updateMultimodalButtonsUI(isScreenshotPending, hasAttachedScreenshot)
        binding.mainInput.text.clear()
    }

    fun expand(forceKeyboard: Boolean = false) {
        onSetFocusable(true)
        if (renderer.isExpanded()) {
            if (forceKeyboard) {
                binding.mainInput.requestFocus()
                renderer.showKeyboard(binding.mainInput)
            }
            return
        }
        renderer.expand()
        binding.mainInput.requestFocus()
        renderer.showKeyboard(binding.mainInput)
    }

    fun dismiss() {
        isScreenshotPending = false
        hasAttachedScreenshot = false
        isModeLocked = false
        binding.btnMultimodal.setLocked(false)
        renderer.updateMultimodalButtonsUI(isScreenshotPending, hasAttachedScreenshot)
        renderer.collapse()
        conversationRepository.clear()
        onDismiss()
    }

    fun destroy() {
        scope.cancel()
    }

    fun lockMode() {
        isModeLocked = true
        binding.btnMultimodal.setLocked(true)
    }

    fun setOnVoiceModeDisabled(listener: () -> Unit) {
        onVoiceModeDisabled = listener
    }
}
