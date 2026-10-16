package com.jarvis.assistant.automation.command

import android.util.Log
import com.jarvis.assistant.automation.phone.PhoneControlManager
import com.jarvis.assistant.automation.whatsapp.WhatsAppAssistant
import com.jarvis.assistant.automation.youtube.YouTubeAutomationManager
import com.jarvis.assistant.data.memory.MemoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommandExecutor(
    private val phoneManager: PhoneControlManager,
    private val youtubeManager: YouTubeAutomationManager,
    private val whatsappAssistant: WhatsAppAssistant,
    private val memoryManager: MemoryManager
) {
    companion object {
        private const val TAG = "CommandExecutor"
    }

    suspend fun execute(command: JarvisCommand): String = withContext(Dispatchers.Main) {
        Log.i(TAG, "Executing command: $command")
        when (command) {
            // YouTube
            is JarvisCommand.PlayYouTube -> {
                val resolvedSong = memoryManager.resolveSongQuery(command.query)
                memoryManager.trackSongPlay(resolvedSong)
                youtubeManager.playVideo(resolvedSong)
            }
            is JarvisCommand.SearchYouTube -> youtubeManager.searchYouTube(command.query)
            is JarvisCommand.PauseMedia -> youtubeManager.pausePlayback()
            is JarvisCommand.ResumeMedia -> youtubeManager.resumePlayback()
            is JarvisCommand.NextMedia -> youtubeManager.nextMedia()
            is JarvisCommand.PreviousMedia -> youtubeManager.previousMedia()
            is JarvisCommand.SubscribeChannel -> youtubeManager.subscribeToChannel()
            is JarvisCommand.ToggleFullscreen -> youtubeManager.toggleFullscreen()

            // Phone Controls
            is JarvisCommand.Flash -> phoneManager.setFlashlight(command.enabled)
            is JarvisCommand.Brightness -> {
                if (command.isRelative) {
                    phoneManager.adjustBrightness(command.delta > 0)
                } else {
                    phoneManager.setBrightnessPercent(command.level)
                }
            }
            is JarvisCommand.Volume -> {
                if (command.isRelative) {
                    phoneManager.adjustVolume(command.delta > 0)
                } else {
                    phoneManager.setVolumePercent(command.level)
                }
            }
            is JarvisCommand.OpenApp -> phoneManager.openApp(command.packageName)
            is JarvisCommand.OpenWebUrls -> phoneManager.openMultipleUrls(command.urls)
            is JarvisCommand.PhoneCall -> phoneManager.makePhoneCall(command.contactOrNumber)
            is JarvisCommand.SearchWeb -> phoneManager.searchWeb(command.query)

            // WhatsApp
            is JarvisCommand.WhatsAppMessage -> whatsappAssistant.sendMessageDirectly(command.contact, command.message)
            is JarvisCommand.WhatsAppCall -> whatsappAssistant.makeCall(command.contact, command.video)
            is JarvisCommand.WhatsAppDeleteMessage -> whatsappAssistant.deleteLastMessage(command.contact, command.forEveryone)
            is JarvisCommand.ConfirmAction -> {
                if (command.confirmed) {
                    whatsappAssistant.confirmAndSend()
                } else {
                    whatsappAssistant.cancelDraft()
                }
            }
            is JarvisCommand.DisambiguationChoice -> whatsappAssistant.handleDisambiguationChoice(command.choice)

            // Memory
            is JarvisCommand.Remember -> memoryManager.rememberFact(command.text)
            is JarvisCommand.Forget -> memoryManager.forgetFact(command.query)
            is JarvisCommand.WhatDoYouRemember -> memoryManager.getWhatDoYouRemember()
            is JarvisCommand.RecallBirthday -> memoryManager.getBirthdayResponse()
            is JarvisCommand.RecallFavoriteSong -> memoryManager.getFavoriteSongResponse()
            is JarvisCommand.RecallRoutine -> memoryManager.getRoutineResponse()
            is JarvisCommand.GoToBackground -> "Going to background."

            // AI Multi-Provider Generation (DeepSeek, Hive, Grok, Meta, Gemini)
            is JarvisCommand.GenerateImage -> {
                val context = com.jarvis.assistant.JarvisApp.instance
                val res = com.jarvis.assistant.automation.ai.AiGenerationEngine.generateImage(context, command.prompt, command.provider)
                if (res.success && res.localFilePath != null) {
                    com.jarvis.assistant.ui.preview.LivePreviewActivity.startPhoto(
                        context,
                        "AI Photo • ${res.provider}",
                        res.provider,
                        res.prompt,
                        res.localFilePath
                    )
                    "Photo generated on screen using ${res.provider}, Sir."
                } else {
                    "I was unable to generate the photo. Please check your API key."
                }
            }
            is JarvisCommand.GenerateVideo -> {
                val context = com.jarvis.assistant.JarvisApp.instance
                val res = com.jarvis.assistant.automation.ai.AiGenerationEngine.generateVideo(context, command.prompt, command.durationSeconds, command.provider)
                com.jarvis.assistant.ui.preview.LivePreviewActivity.startVideo(
                    context,
                    "15s Video • ${res.provider}",
                    res.provider,
                    res.prompt,
                    res.htmlContent
                )
                "Playing 15-second video on screen using ${res.provider}, Sir."
            }
            is JarvisCommand.GenerateCode -> {
                val context = com.jarvis.assistant.JarvisApp.instance
                val res = com.jarvis.assistant.automation.ai.AiGenerationEngine.generateCode(context, command.prompt, command.projectType, command.provider)
                com.jarvis.assistant.ui.preview.LivePreviewActivity.startCode(
                    context,
                    res.title,
                    res.provider,
                    res.prompt,
                    res.htmlCode
                )
                "I have generated the ${res.projectType} and launched the live preview on your screen, Sir."
            }
        }
    }
}
