package com.example.util.agent

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AiProviderManager {
    private val _activeProviderName = MutableStateFlow("Nexora AI Core (Gemini)")
    val activeProviderName: StateFlow<String> = _activeProviderName.asStateFlow()

    // API Keys stored securely
    var openAiApiKey: String = ""
    var geminiApiKey: String = ""
    var claudeApiKey: String = ""
    var deepseekApiKey: String = ""
    var qwenApiKey: String = ""

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("ai_provider_keys", Context.MODE_PRIVATE)
        openAiApiKey = prefs.getString("openai_key", "") ?: ""
        geminiApiKey = prefs.getString("gemini_key", "") ?: ""
        claudeApiKey = prefs.getString("claude_key", "") ?: ""
        deepseekApiKey = prefs.getString("deepseek_key", "") ?: ""
        qwenApiKey = prefs.getString("qwen_key", "") ?: ""
        
        // Load default or built-in Gemini Key
        if (geminiApiKey.isBlank()) {
            val key = if (com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
                com.example.BuildConfig.GEMINI_API_KEY
            } else {
                AiContextManager.getBuiltInGeminiKey()
            }
            geminiApiKey = key
        }
    }

    fun saveKeys(
        context: Context,
        openai: String,
        gemini: String,
        claude: String,
        deepseek: String,
        qwen: String
    ) {
        openAiApiKey = openai
        geminiApiKey = gemini
        claudeApiKey = claude
        deepseekApiKey = deepseek
        qwenApiKey = qwen
        
        val prefs = context.getSharedPreferences("ai_provider_keys", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("openai_key", openai)
            .putString("gemini_key", gemini)
            .putString("claude_key", claude)
            .putString("deepseek_key", deepseek)
            .putString("qwen_key", qwen)
            .apply()
    }

    // Automatically routes the prompt to the best available active provider in the background
    fun getBestProvider(query: String): AiProvider {
        // Fallback hierarchy:
        // 1. OpenAI (if key is configured)
        // 2. Claude (if key is configured)
        // 3. DeepSeek (if key is configured)
        // 4. Qwen (if key is configured)
        // 5. Gemini (always available with built-in key)
        // 6. Local Fallback

        if (openAiApiKey.isNotBlank() && openAiApiKey != "YOUR_OPENAI_KEY_HERE" && openAiApiKey.length > 10) {
            _activeProviderName.value = "Nexora AI Core (OpenAI)"
            return OpenAiProvider()
        }
        if (claudeApiKey.isNotBlank() && claudeApiKey != "YOUR_CLAUDE_KEY_HERE" && claudeApiKey.length > 10) {
            _activeProviderName.value = "Nexora AI Core (Claude)"
            return ClaudeProvider()
        }
        if (deepseekApiKey.isNotBlank() && deepseekApiKey != "YOUR_DEEPSEEK_KEY_HERE" && deepseekApiKey.length > 10) {
            _activeProviderName.value = "Nexora AI Core (DeepSeek)"
            return DeepSeekProvider()
        }
        if (qwenApiKey.isNotBlank() && qwenApiKey != "YOUR_QWEN_KEY_HERE" && qwenApiKey.length > 10) {
            _activeProviderName.value = "Nexora AI Core (Qwen)"
            return QwenProvider()
        }

        // Default to Google Gemini (always has built-in key if nothing else is active)
        _activeProviderName.value = "Nexora AI Core (Gemini)"
        return GeminiProvider()
    }
}
