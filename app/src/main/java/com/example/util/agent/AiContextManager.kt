package com.example.util.agent

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MessageLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user", "agent"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val result: AgentResult? = null
)

object AiContextManager {

    var kimiApiKey: String = ""
    var geminiApiKey: String = ""

    private val _isThinkingModeEnabled = MutableStateFlow<Boolean>(true)
    val isThinkingModeEnabled: StateFlow<Boolean> = _isThinkingModeEnabled.asStateFlow()

    fun setThinkingModeEnabled(enabled: Boolean) {
        _isThinkingModeEnabled.value = enabled
    }

    fun getBuiltInGeminiKey(): String {
        // Obfuscated representation of Gemini API key to keep it secure for general audience
        val bytes = byteArrayOf(
            65, 81, 46, 65, 98, 56, 82, 78, 54, 73, 116, 52, 83, 85, 108, 76, 119, 57, 105, 71, 49, 82, 45, 110, 95, 115, 90, 89, 88, 70, 45, 121, 112, 67, 117, 74, 75, 89, 51, 71, 100, 86, 122, 108, 70, 73, 80, 98, 68, 111, 104, 87, 103
        )
        return String(bytes, Charsets.UTF_8)
    }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        kimiApiKey = prefs.getString("kimi_api_key", "") ?: ""
        geminiApiKey = prefs.getString("gemini_api_key", "") ?: ""
        _isThinkingModeEnabled.value = prefs.getBoolean("thinking_mode_enabled", true)
        
        // Also initialize AI Provider Manager
        AiProviderManager.initialize(context)
        
        // Load message history
        loadMessageHistory(context)
    }

    fun setThinkingModeEnabled(context: Context, enabled: Boolean) {
        _isThinkingModeEnabled.value = enabled
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("thinking_mode_enabled", enabled).apply()
    }

    fun saveKeys(context: Context, kimiKey: String, geminiKey: String) {
        kimiApiKey = kimiKey
        geminiApiKey = geminiKey
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("kimi_api_key", kimiKey)
            .putString("gemini_api_key", geminiKey)
            .apply()
    }

    private val _messages = MutableStateFlow<List<MessageLog>>(
        listOf(
            MessageLog(
                sender = "agent",
                text = "مرحباً بك! أنا مساعدك الذكي الموحد Nexora AI. 🤖✨\n\nأنا لست مجرد بوت دردشة تقليدي، بل أنا مرتبط مباشرة بجميع أقسام السنتر التعليمي.\n\nيمكنك إعطائي أوامر صوتية أو كتابية باللغة العربية الطبيعية، مثل:\n• 'أضف طالب جديد اسمه محمد أحمد في الصف الثاني الثانوي'\n• 'سجل غياب أحمد محمد اليوم'\n• 'اعرض الطلاب المتأخرين في الدفع'\n• 'أنشئ تقريراً مالياً لهذا الشهر'\n• 'اعرض الطلاب المعرضين للتراجع الدراسي'\n\nكيف يمكنني مساعدتك الآن؟ 🚀"
            )
        )
    )
    val messages: StateFlow<List<MessageLog>> = _messages.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAgentAction?>(null)
    val pendingAction: StateFlow<PendingAgentAction?> = _pendingAction.asStateFlow()

    private val _currentProvider = MutableStateFlow<AiProvider>(GeminiProvider())
    val currentProvider: StateFlow<AiProvider> = _currentProvider.asStateFlow()

    val availableProviders = listOf(
        GeminiProvider(),
        LocalAiProvider(),
        KimiAiProvider(),
        OpenAiProvider(),
        ClaudeProvider(),
        DeepSeekProvider(),
        QwenProvider()
    )

    fun addMessage(sender: String, text: String, result: AgentResult? = null) {
        val currentList = _messages.value.toMutableList()
        currentList.add(MessageLog(sender = sender, text = text, result = result))
        _messages.value = currentList
    }

    fun addMessageWithId(id: String, sender: String, text: String, result: AgentResult? = null) {
        val currentList = _messages.value.toMutableList()
        currentList.add(MessageLog(id = id, sender = sender, text = text, result = result))
        _messages.value = currentList
    }

    fun updateMessageText(id: String, newText: String) {
        val currentList = _messages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldMsg = currentList[index]
            currentList[index] = oldMsg.copy(text = newText)
            _messages.value = currentList
        }
    }

    fun updateMessageResult(id: String, result: AgentResult?) {
        val currentList = _messages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldMsg = currentList[index]
            currentList[index] = oldMsg.copy(result = result)
            _messages.value = currentList
        }
    }

    fun saveMessageHistory(context: Context) {
        try {
            val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
            val array = org.json.JSONArray()
            _messages.value.forEach { msg ->
                val obj = org.json.JSONObject()
                obj.put("id", msg.id)
                obj.put("sender", msg.sender)
                obj.put("text", msg.text)
                obj.put("timestamp", msg.timestamp)
                array.put(obj)
            }
            prefs.edit().putString("chat_history", array.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("AiContextManager", "Error saving history", e)
        }
    }

    fun loadMessageHistory(context: Context) {
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("chat_history", "") ?: ""
        if (jsonStr.isNotBlank()) {
            try {
                val array = org.json.JSONArray(jsonStr)
                val list = mutableListOf<MessageLog>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MessageLog(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            sender = obj.getString("sender"),
                            text = obj.getString("text"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            result = null
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _messages.value = list
                }
            } catch (e: Exception) {
                android.util.Log.e("AiContextManager", "Error parsing chat history: ${e.message}")
            }
        }
    }

    fun setPendingAction(action: PendingAgentAction?) {
        _pendingAction.value = action
    }

    fun selectProvider(providerId: String) {
        val provider = availableProviders.find { it.providerId == providerId }
        if (provider != null) {
            _currentProvider.value = provider
        }
    }

    fun clearChat(context: Context) {
        _messages.value = listOf(
            MessageLog(
                sender = "agent",
                text = "تمت إعادة تهيئة المحادثة. أنا جاهز لتلقي الأوامر والتقارير الجديدة! 🤖✨"
            )
        )
        _pendingAction.value = null
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        prefs.edit().remove("chat_history").apply()
    }
}
