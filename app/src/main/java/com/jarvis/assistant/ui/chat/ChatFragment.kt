package com.jarvis.assistant.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.automation.ai.AiGenerationEngine
import com.jarvis.assistant.background.JarvisConversationService
import com.jarvis.assistant.databinding.FragmentChatBinding
import com.jarvis.assistant.ui.home.MainActivity
import com.jarvis.assistant.ui.home.MainViewModel
import com.jarvis.assistant.ui.preview.LivePreviewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel get() = (requireActivity() as MainActivity).viewModel

    private var attachedImageUri: Uri? = null
    private var attachedImagePath: String? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            attachedImageUri = uri
            val path = saveUriToCache(uri)
            attachedImagePath = path
            if (path != null) {
                binding.ivAttachmentThumb.setImageBitmap(BitmapFactory.decodeFile(path))
                binding.layoutAttachmentPreview.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Image attached successfully for AI analysis & code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val chatAdapter = JarvisChatAdapter(
        onEditUserMessage = { text ->
            binding.etMessageInput.setText(text)
            binding.etMessageInput.setSelection(text.length)
            binding.etMessageInput.requestFocus()
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)

        // Setup Chat List
        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.adapter = chatAdapter

        // Send Message
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

        // Attach Image
        binding.btnAttach.setOnClickListener {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnRemoveAttachment.setOnClickListener {
            attachedImageUri = null
            attachedImagePath = null
            binding.layoutAttachmentPreview.visibility = View.GONE
        }

        // Mode Chips
        binding.chipCodeMode.setOnClickListener {
            val current = binding.etMessageInput.text?.toString() ?: ""
            binding.etMessageInput.setText("Write production-grade Kotlin & Jetpack Compose code for: $current")
            binding.etMessageInput.setSelection(binding.etMessageInput.text.length)
            Toast.makeText(requireContext(), "Mode: Code Generation & Preview Active", Toast.LENGTH_SHORT).show()
        }

        binding.chipImageGen.setOnClickListener {
            val prompt = binding.etMessageInput.text?.toString()?.trim() ?: "Futuristic cyberpunk Android UI concept"
            generateImageInChat(prompt)
        }

        binding.chipLiveSearch.setOnClickListener {
            val query = binding.etMessageInput.text?.toString()?.trim() ?: "Latest tech news & real-time live facts"
            performLiveSearch(query)
        }

        // New Chat / Clear
        binding.btnChatNew.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("New Conversation")
                .setMessage("Start a new conversation and clear current history?")
                .setPositiveButton("New Chat") { _, _ ->
                    JarvisApp.instance.chatRepository.clearHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Back / Menu
        binding.btnChatMenu.setOnClickListener {
            (activity as? MainActivity)?.selectTab(MainActivity.TAB_HOME)
        }

        observeChatTurns()
    }

    private fun saveUriToCache(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun sendMessage() {
        val text = binding.etMessageInput.text?.toString()?.trim() ?: ""
        if (text.isBlank() && attachedImagePath == null) return

        val userQuery = text.ifEmpty { "Analyze this attached image" }
        val imgPath = attachedImagePath

        // Clear input and attachment preview
        binding.etMessageInput.text?.clear()
        attachedImageUri = null
        attachedImagePath = null
        binding.layoutAttachmentPreview.visibility = View.GONE

        binding.layoutTypingIndicator.visibility = View.VISIBLE

        // Add user turn immediately to repository
        JarvisApp.instance.chatRepository.addTurn(userQuery, "")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val responseText = callGeminiApiWithContext(userQuery, imgPath)
                withContext(Dispatchers.Main) {
                    binding.layoutTypingIndicator.visibility = View.GONE
                    JarvisApp.instance.chatRepository.addTurn(userQuery, responseText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.layoutTypingIndicator.visibility = View.GONE
                    JarvisApp.instance.chatRepository.addTurn(userQuery, "Error communicating with Gemini API: ${e.message}")
                }
            }
        }
    }

    private suspend fun callGeminiApiWithContext(prompt: String, imagePath: String?): String = withContext(Dispatchers.IO) {
        val apiKey = JarvisApp.instance.preferences.apiKey.ifBlank { com.jarvis.assistant.BuildConfig.GEMINI_API_KEY }
        if (apiKey.isBlank()) {
            return@withContext "API Key is missing. Please set your Gemini API key in Settings or Secrets."
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", "You are ANGEL AI, an expert software engineer and witty assistant. Provide detailed answers, code snippets in markdown code blocks when requested, and brilliant solutions.\n\nUser Prompt: $prompt"))

        if (!imagePath.isNullOrBlank()) {
            val file = File(imagePath)
            if (file.exists()) {
                val bytes = file.readBytes()
                val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val inlineObj = JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Str)
                    })
                }
                partsArray.put(inlineObj)
            }
        }

        val contentObj = JSONObject().put("parts", partsArray)
        val contentsList = JSONArray().put(contentObj)
        val reqJson = JSONObject().put("contents", contentsList)

        val body = reqJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful && response.body != null) {
                val respString = response.body!!.string()
                val root = JSONObject(respString)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")
                return@withContext text ?: "No response generated."
            } else {
                return@withContext "API Error: ${response.code} - ${response.message}"
            }
        }
    }

    private fun generateImageInChat(prompt: String) {
        binding.layoutTypingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AiGenerationEngine.generateImage(requireContext(), prompt)
            withContext(Dispatchers.Main) {
                binding.layoutTypingIndicator.visibility = View.GONE
                if (result.success && result.localFilePath != null) {
                    val turn = com.jarvis.assistant.data.model.ChatTurn(
                        userTranscript = "Generate image: $prompt",
                        jarvisResponse = "Here is the generated image created by ${result.provider}:",
                        jarvisImagePath = result.localFilePath
                    )
                    // Add via repository
                    JarvisApp.instance.chatRepository.addTurn(turn.userTranscript, turn.jarvisResponse)
                } else {
                    Toast.makeText(requireContext(), "Image generation failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performLiveSearch(query: String) {
        val searchPrompt = "Perform a real-time live web search and provide up-to-date facts, news, and data for: $query"
        binding.etMessageInput.setText(searchPrompt)
        sendMessage()
    }

    private fun observeChatTurns() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chatTurns.collect { turns ->
                    binding.layoutTypingIndicator.visibility = View.GONE
                    chatAdapter.submitList(turns) {
                        if (turns.isNotEmpty()) {
                            binding.rvChatMessages.smoothScrollToPosition(turns.size - 1)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
