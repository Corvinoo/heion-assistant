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

package me.robin.heion.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import me.robin.heion.R
import me.robin.heion.assistant.AssistantStatus
import me.robin.heion.assistant.profiles.ConversationMessage

class MessageAdapter(private val markwon: Markwon) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ConversationMessage>()

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
        private const val TYPE_TOOL = 3
    }

    fun addMessage(message: ConversationMessage) {
        messages.add(message)
        notifyItemInserted(messages.lastIndex)
    }

    fun clear() {
        val count = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, count)
    }

    fun updateMessage(index: Int, message: ConversationMessage) {
        if (index in messages.indices) {
            messages[index] = message
            notifyItemChanged(index)
        }
    }

    fun getMessages(): List<ConversationMessage> = messages.toList()

    fun getLastMessage(): ConversationMessage? = messages.lastOrNull()

    fun updateLastMessageStatus(status: AssistantStatus) {
        if (messages.isEmpty()) return
        val lastIndex = messages.lastIndex
        val last = messages[lastIndex]
        if (last is ConversationMessage.Model) {
            messages[lastIndex] = last.copy(status = status)
            notifyItemChanged(lastIndex)
        }
    }

    fun updateLastMessageThinking(thought: String?, timeMs: Long?) {
        if (messages.isEmpty()) return
        val lastIndex = messages.lastIndex
        val last = messages[lastIndex]
        if (last is ConversationMessage.Model) {
            messages[lastIndex] = last.copy(
                thinkingText = thought,
                thinkingTimeMs = timeMs
            )
            notifyItemChanged(lastIndex)
        }
    }

    fun toggleThoughtExpansion(position: Int) {
        val message = messages[position]
        if (message is ConversationMessage.Model) {
            val updated = message.copy(isThoughtExpanded = !message.isThoughtExpanded)
            messages[position] = updated
            notifyItemChanged(position)
            
            // Also notify the repository to persist this UI state
            onThoughtExpansionToggled?.invoke(position, updated.isThoughtExpanded)
        }
    }

    var onThoughtExpansionToggled: ((Int, Boolean) -> Unit)? = null

    fun updateLastMessage(chunk: String) {
        if (messages.isEmpty()) return

        val lastIndex = messages.lastIndex
        val last = messages[lastIndex]

        if (last is ConversationMessage.Model) {
            val newContent =
                if (chunk.startsWith(last.text) && last.text.isNotEmpty()) {
                    // cumulative stream
                    chunk
                } else {
                    // token stream
                    last.text + chunk
                }

            messages[lastIndex] = last.copy(
                text = newContent
                // Preserving status if it's already set to something meaningful
            )

            notifyItemChanged(lastIndex)
        }
    }

    fun setMessages(newMessages: List<ConversationMessage>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    private class MessageDiffCallback(
        private val oldList: List<ConversationMessage>,
        private val newList: List<ConversationMessage>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.timestamp == new.timestamp && old::class == new::class
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ConversationMessage.User -> TYPE_USER
            is ConversationMessage.Model -> TYPE_AI
            is ConversationMessage.ToolCall,
            is ConversationMessage.ToolResult -> TYPE_TOOL
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_USER -> UserViewHolder(
                inflater.inflate(R.layout.item_message_user, parent, false),
                markwon
            )
            TYPE_AI -> AiViewHolder(
                inflater.inflate(R.layout.item_message_ai, parent, false),
                markwon
            )
            else -> ToolViewHolder(
                inflater.inflate(R.layout.item_message_ai, parent, false),
                markwon
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = messages[position]

        when (holder) {
            is UserViewHolder -> holder.bind(message as ConversationMessage.User)
            is AiViewHolder -> holder.bind(message as ConversationMessage.Model)
            is ToolViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(view: View, private val markwon: Markwon) : RecyclerView.ViewHolder(view) {

        private val text: TextView =
            view.findViewById(R.id.messageText)

        private val micIcon: View =
            view.findViewById(R.id.micIcon)

        private val audioDuration: TextView =
            view.findViewById(R.id.audioDuration)

        fun bind(message: ConversationMessage.User) {
            markwon.setMarkdown(text, message.text)
            
            if (message.audioDurationMs != null) {
                micIcon.visibility = View.VISIBLE
                audioDuration.visibility = View.VISIBLE
                audioDuration.text = formatDuration(message.audioDurationMs)
            } else {
                micIcon.visibility = View.GONE
                audioDuration.visibility = View.GONE
            }
        }

        private fun formatDuration(ms: Long): String {
            val seconds = (ms / 1000) % 60
            val minutes = (ms / (1000 * 60)) % 60
            return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }

        fun bindText(content: String) {
            markwon.setMarkdown(text, content)
        }
    }

    inner class AiViewHolder(view: View, private val markwon: Markwon) : RecyclerView.ViewHolder(view) {

        private val text: TextView =
            view.findViewById(R.id.messageText)

        private val modelNameText: TextView =
            view.findViewById(R.id.modelNameText)

        private val statusContainer: View =
            view.findViewById(R.id.statusContainer)

        private val statusText: TextView =
            view.findViewById(R.id.statusText)

        private val statusProgress: View =
            view.findViewById(R.id.statusProgress)

        private val thoughtContainer: View =
            view.findViewById(R.id.thoughtContainer)

        private val thoughtText: TextView =
            view.findViewById(R.id.thoughtText)

        init {
            statusContainer.setOnClickListener {
                val pos = adapterPosition

                if (pos == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                val item = messages.getOrNull(pos)

                if (item is ConversationMessage.Model) {
                    val hasThinkingContent = !item.thinkingText.isNullOrEmpty()
                    val isThinkingState = item.status is AssistantStatus.Thinking || 
                        (item.status is AssistantStatus.Streaming && item.thinkingTimeMs == null && item.thinkingText != null)
                    val isThoughtDone = item.thinkingTimeMs != null

                    if (hasThinkingContent || isThinkingState || isThoughtDone) {
                        toggleThoughtExpansion(pos)
                    }
                }
            }
        }

        fun bind(message: ConversationMessage.Model) {
            modelNameText.text = message.modelName ?: "Assistant"

            val hasThinkingContent = !message.thinkingText.isNullOrEmpty()
            val isThinkingState = message.status is AssistantStatus.Thinking || 
                (message.status is AssistantStatus.Streaming && message.thinkingTimeMs == null && message.thinkingText != null)
            
            val isThoughtDone = message.thinkingTimeMs != null
            val isBusy = message.status !is AssistantStatus.Idle && 
                message.status !is AssistantStatus.Thinking && 
                message.status !is AssistantStatus.Streaming && 
                message.text.isEmpty()

            val shouldShowStatus = isBusy || isThinkingState || isThoughtDone || hasThinkingContent || message.status is AssistantStatus.Streaming
            statusContainer.visibility = if (shouldShowStatus) View.VISIBLE else View.GONE

            if (shouldShowStatus) {
                val timeMs = message.thinkingTimeMs
                statusText.text = when {
                    timeMs != null -> "Thought for ${String.format(java.util.Locale.US, "%.1f", timeMs / 1000f)}s"
                    isThinkingState -> "Thinking..."
                    else -> message.status.getLabel()
                }
                
                statusProgress.visibility = if (isBusy || isThinkingState || (message.status is AssistantStatus.Streaming && !isThoughtDone)) View.VISIBLE else View.GONE
                
                statusContainer.isClickable = hasThinkingContent || isThinkingState || isThoughtDone
                statusContainer.isEnabled = hasThinkingContent || isThinkingState || isThoughtDone
            }

            if (message.text.isNotEmpty()) {
                text.visibility = View.VISIBLE
                markwon.setMarkdown(text, message.text)
            } else {
                text.visibility = View.GONE
            }

            if (message.isThoughtExpanded && (hasThinkingContent || isThinkingState)) {
                thoughtContainer.visibility = View.VISIBLE
                val displayThought = if (hasThinkingContent) message.thinkingText else "Reasoning..."
                thoughtText.text = displayThought
                thoughtText.alpha = if (hasThinkingContent) 1.0f else 0.5f
            } else {
                thoughtContainer.visibility = View.GONE
            }
        }

        fun bindText(content: String) {
            text.visibility = View.VISIBLE
            markwon.setMarkdown(text, content)
        }
    }

    class ToolViewHolder(view: View, private val markwon: Markwon) : RecyclerView.ViewHolder(view) {

        private val text: TextView =
            view.findViewById(R.id.messageText)

        fun bind(message: ConversationMessage) {
            text.visibility = View.VISIBLE
            text.alpha = 0.7f
            markwon.setMarkdown(text, "_${message.content}_")
        }

        fun bindText(content: String) {
            text.visibility = View.VISIBLE
            markwon.setMarkdown(text, "_${content}_")
        }
    }
}
