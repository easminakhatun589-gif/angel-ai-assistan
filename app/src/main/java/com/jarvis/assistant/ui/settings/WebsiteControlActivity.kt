package com.jarvis.assistant.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.databinding.ActivityWebsiteControlBinding
import java.util.Locale

class WebsiteControlActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"

        fun start(context: Context, initialUrl: String = "https://lovable.dev") {
            val intent = Intent(context, WebsiteControlActivity::class.java).apply {
                putExtra(EXTRA_URL, initialUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityWebsiteControlBinding

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                binding.etVoiceCommand.setText(spokenText)
                executeVoiceCommand(spokenText)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebsiteControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarWebsiteControl)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarWebsiteControl.setNavigationOnClickListener { finish() }

        // Add help/guide action on toolbar click
        binding.toolbarWebsiteControl.setOnClickListener {
            showVoiceCommandGuide()
        }

        val initialUrl = intent.getStringExtra(EXTRA_URL) ?: "https://lovable.dev"
        binding.etUrlInput.setText(initialUrl)

        setupWebView()
        setupListeners()

        binding.webViewBrowser.loadUrl(initialUrl)
        
        // Show welcome toast about Lovable & Multilingual Voice Control
        Toast.makeText(this, "JARVIS AI Voice & Lovable Agent Ready (EN / বাংলা / हिन्दी)", Toast.LENGTH_LONG).show()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewBrowser.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        binding.webViewBrowser.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                binding.progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }
        }

        binding.webViewBrowser.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.etUrlInput.setText(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.tvAgentStatus.text = "Loaded: ${url ?: "Unknown"} — Voice & Code Editing Ready."
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (binding.webViewBrowser.canGoBack()) binding.webViewBrowser.goBack()
        }

        binding.btnForward.setOnClickListener {
            if (binding.webViewBrowser.canGoForward()) binding.webViewBrowser.goForward()
        }

        binding.btnGo.setOnClickListener {
            loadUrlFromInput()
        }

        binding.etUrlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromInput()
                true
            } else false
        }

        // Quick Chips
        binding.chipLovable.setOnClickListener { navigateTo("https://lovable.dev") }
        binding.chipGoogle.setOnClickListener { navigateTo("https://www.google.com") }
        binding.chipYouTube.setOnClickListener { navigateTo("https://www.youtube.com") }
        binding.chipGitHub.setOnClickListener { navigateTo("https://github.com") }
        binding.chipWikipedia.setOnClickListener { navigateTo("https://www.wikipedia.org") }
        binding.chipChatGPT.setOnClickListener { navigateTo("https://chatgpt.com") }
        binding.chipReddit.setOnClickListener { navigateTo("https://www.reddit.com") }

        // Voice Command Input
        binding.btnExecuteCommand.setOnClickListener {
            val cmd = binding.etVoiceCommand.text.toString().trim()
            if (cmd.isNotBlank()) {
                executeVoiceCommand(cmd)
            }
        }

        binding.etVoiceCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val cmd = binding.etVoiceCommand.text.toString().trim()
                if (cmd.isNotBlank()) {
                    executeVoiceCommand(cmd)
                }
                true
            } else false
        }

        binding.btnVoiceMic.setOnClickListener {
            startVoiceRecognition()
        }
    }

    private fun loadUrlFromInput() {
        var url = binding.etUrlInput.text.toString().trim()
        if (url.isNotEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (url.contains(".") && !url.contains(" ")) {
                    url = "https://$url"
                } else {
                    url = "https://www.google.com/search?q=" + android.net.Uri.encode(url)
                }
            }
            navigateTo(url)
        }
    }

    private fun navigateTo(url: String) {
        binding.etUrlInput.setText(url)
        binding.webViewBrowser.loadUrl(url)
        binding.tvAgentStatus.text = "Navigating to $url..."
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD,hi-IN,en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say command in English, Bangla or Hindi (e.g. 'Lovable kholo', 'code generate', 'click login')...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available on device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVoiceCommandGuide() {
        MaterialAlertDialogBuilder(this)
            .setTitle("JARVIS Multilingual Voice & Lovable Control Guide")
            .setMessage("""
                Control any website, build on Lovable.dev, and write/edit code step by step using voice in English, Bangla, and Hindi!
                
                🌐 1. Lovable & Website Navigation:
                • English: "Open Lovable", "Go to Lovable.dev"
                • Bangla: "লোভেবল খোলো", "লোভেবল ওয়েবসাইট এ যাও"
                • Hindi: "लोवेबल खोलो", "लोवेबल वेबसाइट पर जाओ"
                
                💻 2. Code Generation & Step-by-Step Editing:
                • English: "Generate code for login", "Step by step update", "Edit code here"
                • Bangla: "কোড তৈরি করো", "ধাপে ধাপে আপডেট করো", "কোড এডিট করো"
                • Hindi: "कोड जनरेट करो", "स्टेप बाय स्टेप अपडेट करो", "कोड एडिट करो"
                
                👆 3. Browser Action Control:
                • English: "Click login", "Scroll down", "Type [text]"
                • Bangla: "লগইন ক্লিক করো", "নিচে স্ক্রল করো"
                • Hindi: "लॉगिन क्लिक करो", "नीचे स्क्रॉल करो"
            """.trimIndent())
            .setPositiveButton("Got It") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun executeVoiceCommand(command: String) {
        val lower = command.toLowerCase(Locale.ROOT)
        binding.tvAgentStatus.text = "Executing: \"$command\"..."

        when {
            // Lovable Navigation
            lower.contains("lovable") || lower.contains("lovesable") || lower.contains("লোভেবল") || lower.contains("लोवेबल") -> {
                navigateTo("https://lovable.dev")
                Toast.makeText(this, "Opening Lovable.dev for AI App Building...", Toast.LENGTH_LONG).show()
            }
            lower.contains("go to ") || lower.contains("open ") -> {
                val target = lower.replace("go to ", "").replace("open ", "").trim()
                navigateTo(target)
            }
            // Bangla / Hindi opening commands
            lower.contains(" kholo") || lower.contains("kholo ") || lower.contains("খোলো") || lower.contains("खोलो") -> {
                val targetSite = lower.replace("kholo", "").replace("খোলো", "").replace("खोलो", "").trim()
                if (targetSite.isNotEmpty()) {
                    navigateTo("https://www.$targetSite.com")
                } else {
                    Toast.makeText(this, "Please specify website to open.", Toast.LENGTH_SHORT).show()
                }
            }
            // Code Generation & Step-by-Step Editing Commands
            lower.contains("code") || lower.contains("কোড") || lower.contains("कोड") || lower.contains("generate") || lower.contains("banao") || lower.contains("tiri") || lower.contains("তৈরি") -> {
                val codePrompt = command
                val js = """
                    (function() {
                        var inputs = document.querySelectorAll('textarea, input[type="text"], div[contenteditable="true"], [role="textbox"]');
                        if (inputs.length > 0) {
                            inputs[0].focus();
                            inputs[0].value = '$codePrompt';
                            inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                            inputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                            return 'Injected code prompt into editor: $codePrompt';
                        }
                        return 'No code editor or input box found on page';
                    })();
                """.trimIndent()
                binding.webViewBrowser.evaluateJavascript(js) { res ->
                    binding.tvAgentStatus.text = "Code Agent: $res"
                    Toast.makeText(this, "Code Command Executed: Step-by-step update initiated", Toast.LENGTH_SHORT).show()
                }
            }
            lower.contains("scroll down") || lower.contains("নিচে") || lower.contains("नीचे") -> {
                binding.webViewBrowser.evaluateJavascript("window.scrollBy(0, window.innerHeight * 0.75);", null)
                Toast.makeText(this, "Scrolled down", Toast.LENGTH_SHORT).show()
            }
            lower.contains("scroll up") || lower.contains("উপরে") || lower.contains("ऊपर") -> {
                binding.webViewBrowser.evaluateJavascript("window.scrollBy(0, -window.innerHeight * 0.75);", null)
                Toast.makeText(this, "Scrolled up", Toast.LENGTH_SHORT).show()
            }
            lower.contains("refresh") || lower.contains("reload") -> {
                binding.webViewBrowser.reload()
            }
            lower.contains("click ") -> {
                val targetText = command.substringAfter("click").trim().replace("'", "").replace("\"", "")
                val js = """
                    (function() {
                        var elements = document.querySelectorAll('a, button, input[type="submit"], input[type="button"], [role="button"]');
                        for (var i = 0; i < elements.length; i++) {
                            if (elements[i].innerText && elements[i].innerText.toLowerCase().includes('$targetText'.toLowerCase())) {
                                elements[i].click();
                                return 'Clicked: ' + elements[i].innerText;
                            }
                        }
                        return 'Element not found: $targetText';
                    })();
                """.trimIndent()
                binding.webViewBrowser.evaluateJavascript(js) { res ->
                    binding.tvAgentStatus.text = "Result: $res"
                }
            }
            lower.contains("type ") -> {
                val parts = lower.split(" into ")
                val textToType = parts[0].substringAfter("type").trim().replace("'", "").replace("\"", "")
                val js = """
                    (function() {
                        var inputs = document.querySelectorAll('input[type="text"], input[type="search'], textarea, input:not([type])');
                        if (inputs.length > 0) {
                            inputs[0].value = '$textToType';
                            inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                            inputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                            return 'Typed into input field successfully';
                        }
                        return 'No input field found';
                    })();
                """.trimIndent()
                binding.webViewBrowser.evaluateJavascript(js) { res ->
                    binding.tvAgentStatus.text = "Result: $res"
                }
            }
            lower.contains("search ") -> {
                val query = command.substringAfter("search").trim()
                val searchUrl = "https://www.google.com/search?q=" + android.net.Uri.encode(query)
                navigateTo(searchUrl)
            }
            else -> {
                val js = """
                    (function() {
                        return document.title;
                    })();
                """.trimIndent()
                binding.webViewBrowser.evaluateJavascript(js) { title ->
                    binding.tvAgentStatus.text = "Page Title: $title. Voice command processed."
                }
            }
        }
    }
}
