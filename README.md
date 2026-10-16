# 🤖 JARVIS AI Voice Assistant for Android

A production-grade, multimodal personal AI assistant for Android powered by Google Gemini Live bidirectional streaming, offline Vosk wake-word detection, Room memory database, screen vision reasoning, and hands-free accessibility automations.

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.3.1-brightgreen.svg)](https://gradle.org)

---

## ⚡ Core Architecture & Features

- **Gemini Live Bidi Streaming**: Real-time bidirectional WebSocket PCM audio communication with Google Gemini Live API.
- **Futuristic Iron Man Mark 85 HUD**: Interactive WebGL / Canvas visual reactor orb with audio reactivity and status states.
- **17 Interactive Floating Overlays**: System-wide companion overlays (Alien, Blob, Cat, Cloud, Mech, Pebble, etc.).
- **Real-Time Screen-Sharing Vision**: MediaProjection screen capture with UI grounding for contextual assistance.
- **Offline Wake-Word Engine**: Vosk speech recognition and speaker identification for hands-free activation.
- **System Automations**: Accessibility service automation for WhatsApp messaging, YouTube video search & playback, phone calls, volume, and brightness.
- **Persistent Memory Core**: SQLite/Room database for long-term memory of user preferences, contacts, notes, and routines.
- **Sound FX Engine**: Synthesized Iron Man system audio cues for activation, standby, error, and confirmations.

---

## 🚀 Getting Started

### 1. Set Your Gemini API Key
Provide your Google Gemini API key using one of the following methods:
- In Google AI Studio Build: Add `GEMINI_API_KEY` in the **Secrets** panel.
- In a `.env` file: Set `GEMINI_API_KEY=your_key_here`.
- Inside the app: Open **Settings > API Connection** and paste your API key.

### 2. Building & Running
- Android SDK 34+
- Built via Gradle Kotlin DSL (`./gradlew assembleDebug` or `compile_applet`)
