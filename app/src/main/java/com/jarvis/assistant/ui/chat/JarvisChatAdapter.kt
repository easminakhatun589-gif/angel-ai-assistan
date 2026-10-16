package com.jarvis.assistant.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.data.model.ChatTurn
import com.jarvis.assistant.databinding.ItemNairaChatMessageBinding
import com.jarvis.assistant.ui.preview.LivePreviewActivity
import java.io.File

class JarvisChatAdapter(
    private val onEditUserMessage: ((String) -> Unit)? = null
) : ListAdapter<ChatTurn, JarvisChatAdapter.MessageViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<ChatTurn>() {
        override fun areItemsTheSame(oldItem: ChatTurn, newItem: ChatTurn): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatTurn, newItem: ChatTurn): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemNairaChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemNairaChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(turn: ChatTurn) {
            val context = binding.root.context

            // --- User Bubble ---
            if (turn.userTranscript.isNotBlank() || !turn.userImagePath.isNullOrBlank()) {
                binding.layoutUserContainer.visibility = View.VISIBLE
                binding.tvUserMessage.text = turn.userTranscript

                if (!turn.userImagePath.isNullOrBlank()) {
                    val file = File(turn.userImagePath)
                    if (file.exists()) {
                        binding.ivUserAttachment.visibility = View.VISIBLE
                        binding.ivUserAttachment.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    } else {
                        binding.ivUserAttachment.visibility = View.GONE
                    }
                } else {
                    binding.ivUserAttachment.visibility = View.GONE
                }

                binding.btnUserCopy.setOnClickListener {
                    copyToClipboard(context, "User message", turn.userTranscript)
                }

                binding.btnUserEdit.setOnClickListener {
                    onEditUserMessage?.invoke(turn.userTranscript)
                }
            } else {
                binding.layoutUserContainer.visibility = View.GONE
            }

            // --- Assistant Bubble ---
            if (turn.jarvisResponse.isNotBlank() || !turn.jarvisImagePath.isNullOrBlank() || !turn.codeSnippet.isNullOrBlank()) {
                binding.layoutAssistantContainer.visibility = View.VISIBLE
                binding.tvAssistantMessage.text = turn.jarvisResponse

                // Assistant Image
                if (!turn.jarvisImagePath.isNullOrBlank()) {
                    val file = File(turn.jarvisImagePath)
                    if (file.exists()) {
                        binding.ivAssistantImage.visibility = View.VISIBLE
                        binding.ivAssistantImage.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                        binding.ivAssistantImage.setOnClickListener {
                            LivePreviewActivity.startPhoto(context, "Generated Artwork", "ANGEL AI", turn.userTranscript, file.absolutePath)
                        }
                    } else {
                        binding.ivAssistantImage.visibility = View.GONE
                    }
                } else {
                    binding.ivAssistantImage.visibility = View.GONE
                }

                // Code Snippet detection & display
                val code = turn.codeSnippet ?: extractCodeBlock(turn.jarvisResponse)
                if (!code.isNullOrBlank()) {
                    binding.layoutCodeSnippetContainer.visibility = View.VISIBLE
                    binding.tvCodeSnippet.text = code

                    binding.btnCopyCodeSnippet.setOnClickListener {
                        copyToClipboard(context, "Code snippet", code)
                    }

                    binding.btnPreviewCode.setOnClickListener {
                        LivePreviewActivity.startCode(context, "Live Code Preview", "ANGEL AI", turn.userTranscript, code)
                    }
                } else {
                    binding.layoutCodeSnippetContainer.visibility = View.GONE
                }

                binding.btnAssistantCopy.setOnClickListener {
                    copyToClipboard(context, "ANGEL AI response", turn.jarvisResponse)
                }

                binding.btnAssistantPlay.setOnClickListener {
                    Toast.makeText(context, "Playing response audio...", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.layoutAssistantContainer.visibility = View.GONE
            }
        }

        private fun extractCodeBlock(response: String): String? {
            val startIdx = response.indexOf("```")
            if (startIdx == -1) return null
            val endIdx = response.indexOf("```", startIdx + 3)
            if (endIdx == -1) return null
            val sub = response.substring(startIdx + 3, endIdx)
            // Remove language specifier if present (e.g. html\n or kotlin\n)
            val newlineIdx = sub.indexOf("\n")
            return if (newlineIdx != -1 && newlineIdx < 15) {
                sub.substring(newlineIdx + 1).trim()
            } else {
                sub.trim()
            }
        }

        private fun copyToClipboard(context: Context, label: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}
