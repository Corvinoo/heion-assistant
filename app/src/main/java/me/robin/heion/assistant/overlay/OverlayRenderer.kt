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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import me.robin.heion.R
import me.robin.heion.databinding.OverlayMainBinding
import me.robin.heion.ui.chat.MessageAdapter
import androidx.core.view.isVisible

class OverlayRenderer(
    private val context: Context,
    private val binding: OverlayMainBinding
) {
    private val markwon = Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .build()

    val adapter = MessageAdapter(markwon)

    init {
        binding.messageList.adapter = adapter
        binding.messageList.itemAnimator = null
    }

    fun getRootView(): View = binding.rootContainer

    fun isExpanded(): Boolean = binding.messageList.isVisible

    fun showKeyboard(view: View) {
        view.postDelayed({
            context.getSystemService<InputMethodManager>()
                ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    fun hideKeyboard(view: View) {
        context.getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun expand() {
        binding.conversationContainer.visibility = View.VISIBLE
        binding.messageList.visibility = View.VISIBLE
        binding.btnSend.visibility = View.VISIBLE
        binding.scrimView.visibility = View.VISIBLE

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                binding.scrimView.alpha = animator.animatedValue as Float
            }
            start()
        }
    }

    fun collapse() {
        binding.scrimView.alpha = 0f
        binding.scrimView.visibility = View.GONE
        binding.conversationContainer.visibility = View.GONE
    }

    fun updateVoiceRecordingUI(active: Boolean) {
        with(binding) {
            if (active) {
                inputBar.setBackgroundResource(R.drawable.bg_floating_bar_voice)
                mainInput.hint = "Listening..."
                btnMultimodal.visibility = View.GONE
            } else {
                inputBar.setBackgroundResource(R.drawable.bg_floating_bar)
                mainInput.hint = "Ask anything…"
                btnMultimodal.visibility = View.VISIBLE
            }
        }
    }

    fun updateSendButtonIcon(isGenerating: Boolean, isEmpty: Boolean) {
        if (isGenerating && isEmpty) {
            binding.btnSend.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            binding.btnSend.setImageResource(android.R.drawable.ic_menu_send)
        }
    }

    fun updateMultimodalButtonsUI(isScreenshotPending: Boolean, hasAttachedScreenshot: Boolean) {
        binding.btnMultimodal.setSelectedState(isScreenshotPending || hasAttachedScreenshot)
    }

    fun getTouchableRegion(screenWidth: Int, screenHeight: Int): Rect {
        return if (isExpanded()) {
            Rect(0, 0, screenWidth, screenHeight)
        } else {
            val rect = Rect()
            binding.conversationContainer.getGlobalVisibleRect(rect)
            rect
        }
    }

    fun setupWindowInsets(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val bottomInset = if (imeHeight > 0) imeHeight else systemBarsHeight

            val params = binding.conversationContainer.layoutParams as ViewGroup.MarginLayoutParams
            val margin16dp = (16 * context.resources.displayMetrics.density).toInt()
            params.bottomMargin = bottomInset + margin16dp
            binding.conversationContainer.layoutParams = params
            insets
        }
    }

    fun scrollToBottom() {
        if (adapter.itemCount > 0) {
            binding.messageList.scrollToPosition(adapter.itemCount - 1)
        }
    }
}
