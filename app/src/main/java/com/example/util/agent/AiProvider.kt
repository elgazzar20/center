package com.example.util.agent

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAiProvider : AiProvider {
    override val name: String = "Nexora Local Engine 🤖 (افتراضي آمن)"
    override val providerId: String = "local_nexora"
    
    override fun isAvailable(): Boolean = true

    override suspend fun generateResponse(prompt: String, contextText: String): String? {
        return null // The engine will handle parsing locally using rules and offline regex patterns.
    }
}

class GeminiProvider : AiProvider {
    override val name: String = "Google Gemini API ✨"
    override val providerId: String = "gemini_api"
    
    override fun isAvailable(): Boolean = true

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(prompt: String, contextText: String): String {
        val apiKey = if (AiProviderManager.geminiApiKey.isNotBlank()) {
            AiProviderManager.geminiApiKey
        } else if (com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
            com.example.BuildConfig.GEMINI_API_KEY
        } else {
            AiContextManager.getBuiltInGeminiKey()
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return """
                {
                  "thought": "No API Key configured",
                  "conversational_response": "يرجى تهيئة مفتاح API الخاص بـ Gemini لتفعيل الذكاء الاصطناعي المتقدم. ⚠️",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
            """.trimIndent()
        }

        val isThinkingEnabled = AiContextManager.isThinkingModeEnabled.value
        val fullPrompt = "$prompt\n\n[DATABASE_CONTEXT]\n$contextText"
        val promptEscaped = escapeJsonString(fullPrompt)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        return withContext(Dispatchers.IO) {
            val modelsToTry = if (isThinkingEnabled) {
                listOf("gemini-3.1-pro-preview", "gemini-3.5-flash")
            } else {
                listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")
            }
            var lastError = "لم يتم الحصول على استجابة"
            var lastCode = 0
            
            for (model in modelsToTry) {
                val isModelThinkingSupported = model.contains("pro") || model.contains("thinking")
                val isThinkingModeForModel = isThinkingEnabled && isModelThinkingSupported
                
                val generationConfigStr = if (isThinkingModeForModel) {
                    """
                    "responseMimeType": "application/json",
                    "thinkingConfig": {
                      "thinkingLevel": "HIGH"
                    }
                    """.trimIndent()
                } else {
                    """
                    "responseMimeType": "application/json"
                    """.trimIndent()
                }

                val jsonPayload = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": "$promptEscaped" }
                      ]
                    }
                  ],
                  "generationConfig": {
                    $generationConfigStr
                  }
                }
                """.trimIndent()

                val requestBody = jsonPayload.toRequestBody(mediaType)
                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                    .header("X-goog-api-key", apiKey)
                    .post(requestBody)
                    .build()
                
                try {
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val responseJson = org.json.JSONObject(body)
                        val candidates = responseJson.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val firstPart = parts?.optJSONObject(0)
                        val text = firstPart?.optString("text") ?: ""
                        if (text.isNotBlank()) {
                            return@withContext text
                        }
                    } else {
                        val errorBody = body ?: ""
                        val errorDetail = try {
                            val errJson = org.json.JSONObject(errorBody)
                            val errObj = errJson.optJSONObject("error")
                            errObj?.optString("message") ?: errorBody
                        } catch (e: Exception) {
                            errorBody.ifBlank { response.message }
                        }
                        lastError = errorDetail
                        lastCode = response.code
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GeminiProvider", "Error calling Gemini with model $model", e)
                    lastError = e.localizedMessage ?: "Network error"
                }
            }
            
            """
            {
              "thought": "API call failed after trying all models",
              "conversational_response": "عذراً، حدث خطأ أثناء الاتصال بـ Gemini API: $lastError (كود الخطأ: $lastCode)",
              "tool_call": null,
              "requires_confirmation": false,
              "confirmation_message": ""
            }
            """.trimIndent()
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

class OpenAiProvider : AiProvider {
    override val name: String = "OpenAI GPT-4 🧠"
    override val providerId: String = "openai_gpt"
    
    override fun isAvailable(): Boolean = AiProviderManager.openAiApiKey.isNotBlank()

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(prompt: String, contextText: String): String {
        val apiKey = AiProviderManager.openAiApiKey
        if (apiKey.isBlank()) {
            return """
                {
                  "thought": "No OpenAI API Key configured",
                  "conversational_response": "يرجى تهيئة مفتاح API الخاص بـ OpenAI في الإعدادات المتقدمة لتفعيله. ⚠️",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
            """.trimIndent()
        }

        val promptEscaped = escapeJsonString(prompt)
        val contextEscaped = escapeJsonString(contextText)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonPayload = """
        {
          "model": "gpt-4o-mini",
          "messages": [
            { "role": "system", "content": "$promptEscaped" },
            { "role": "user", "content": "$contextEscaped" }
          ],
          "response_format": { "type": "json_object" }
        }
        """.trimIndent()

        val requestBody = jsonPayload.toRequestBody(mediaType)
        val request = okhttp3.Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val responseJson = org.json.JSONObject(body)
                    val choices = responseJson.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val content = message?.optString("content") ?: ""
                    if (content.isNotBlank()) {
                        return@withContext content
                    }
                }
                val errorMsg = body ?: "Unknown OpenAI Error"
                """
                {
                  "thought": "OpenAI API Call Failed",
                  "conversational_response": "عذراً، حدث خطأ أثناء الاتصال بـ OpenAI: $errorMsg (كود الخطأ: ${response.code})",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            } catch (e: Exception) {
                android.util.Log.e("OpenAiProvider", "Error calling OpenAI API", e)
                """
                {
                  "thought": "Network exception",
                  "conversational_response": "تعذر الاتصال بخوادم OpenAI: ${e.localizedMessage}",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            }
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

class ClaudeProvider : AiProvider {
    override val name: String = "Anthropic Claude 3 🌸"
    override val providerId: String = "claude"
    
    override fun isAvailable(): Boolean = AiProviderManager.claudeApiKey.isNotBlank()

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(prompt: String, contextText: String): String {
        val apiKey = AiProviderManager.claudeApiKey
        if (apiKey.isBlank()) {
            return """
                {
                  "thought": "No Claude API Key configured",
                  "conversational_response": "يرجى تهيئة مفتاح API الخاص بـ Claude في الإعدادات المتقدمة لتفعيله. ⚠️",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
            """.trimIndent()
        }

        val promptEscaped = escapeJsonString(prompt)
        val contextEscaped = escapeJsonString(contextText)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Anthropic Claude 3 messages schema
        val jsonPayload = """
        {
          "model": "claude-3-5-sonnet-20241022",
          "max_tokens": 4000,
          "system": "$promptEscaped",
          "messages": [
            { "role": "user", "content": "$contextEscaped" }
          ]
        }
        """.trimIndent()

        val requestBody = jsonPayload.toRequestBody(mediaType)
        val request = okhttp3.Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val responseJson = org.json.JSONObject(body)
                    val contentArray = responseJson.optJSONArray("content")
                    val firstContent = contentArray?.optJSONObject(0)
                    val text = firstContent?.optString("text") ?: ""
                    if (text.isNotBlank()) {
                        return@withContext text
                    }
                }
                val errorMsg = body ?: "Unknown Claude Error"
                """
                {
                  "thought": "Claude API Call Failed",
                  "conversational_response": "عذراً، حدث خطأ أثناء الاتصال بـ Claude: $errorMsg (كود الخطأ: ${response.code})",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            } catch (e: Exception) {
                android.util.Log.e("ClaudeProvider", "Error calling Claude API", e)
                """
                {
                  "thought": "Network exception",
                  "conversational_response": "تعذر الاتصال بخوادم Claude: ${e.localizedMessage}",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            }
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

class DeepSeekProvider : AiProvider {
    override val name: String = "DeepSeek R1 ⚡"
    override val providerId: String = "deepseek"
    
    override fun isAvailable(): Boolean = AiProviderManager.deepseekApiKey.isNotBlank()

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(prompt: String, contextText: String): String {
        val apiKey = AiProviderManager.deepseekApiKey
        if (apiKey.isBlank()) {
            return """
                {
                  "thought": "No DeepSeek API Key configured",
                  "conversational_response": "يرجى تهيئة مفتاح API الخاص بـ DeepSeek في الإعدادات المتقدمة لتفعيله. ⚠️",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
            """.trimIndent()
        }

        val promptEscaped = escapeJsonString(prompt)
        val contextEscaped = escapeJsonString(contextText)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonPayload = """
        {
          "model": "deepseek-chat",
          "messages": [
            { "role": "system", "content": "$promptEscaped" },
            { "role": "user", "content": "$contextEscaped" }
          ],
          "response_format": { "type": "json_object" }
        }
        """.trimIndent()

        val requestBody = jsonPayload.toRequestBody(mediaType)
        val request = okhttp3.Request.Builder()
            .url("https://api.deepseek.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val responseJson = org.json.JSONObject(body)
                    val choices = responseJson.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val content = message?.optString("content") ?: ""
                    if (content.isNotBlank()) {
                        return@withContext content
                    }
                }
                val errorMsg = body ?: "Unknown DeepSeek Error"
                """
                {
                  "thought": "DeepSeek API Call Failed",
                  "conversational_response": "عذراً، حدث خطأ أثناء الاتصال بـ DeepSeek: $errorMsg (كود الخطأ: ${response.code})",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            } catch (e: Exception) {
                android.util.Log.e("DeepSeekProvider", "Error calling DeepSeek API", e)
                """
                {
                  "thought": "Network exception",
                  "conversational_response": "تعذر الاتصال بخوادم DeepSeek: ${e.localizedMessage}",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            }
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

class QwenProvider : AiProvider {
    override val name: String = "Alibaba Qwen 2.5 🌌"
    override val providerId: String = "qwen"
    
    override fun isAvailable(): Boolean = AiProviderManager.qwenApiKey.isNotBlank()

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(prompt: String, contextText: String): String {
        val apiKey = AiProviderManager.qwenApiKey
        if (apiKey.isBlank()) {
            return """
                {
                  "thought": "No Qwen API Key configured",
                  "conversational_response": "يرجى تهيئة مفتاح API الخاص بـ Qwen في الإعدادات المتقدمة لتفعيله. ⚠️",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
            """.trimIndent()
        }

        val promptEscaped = escapeJsonString(prompt)
        val contextEscaped = escapeJsonString(contextText)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonPayload = """
        {
          "model": "qwen-turbo",
          "messages": [
            { "role": "system", "content": "$promptEscaped" },
            { "role": "user", "content": "$contextEscaped" }
          ],
          "response_format": { "type": "json_object" }
        }
        """.trimIndent()

        val requestBody = jsonPayload.toRequestBody(mediaType)
        val request = okhttp3.Request.Builder()
            .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val responseJson = org.json.JSONObject(body)
                    val choices = responseJson.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val content = message?.optString("content") ?: ""
                    if (content.isNotBlank()) {
                        return@withContext content
                    }
                }
                val errorMsg = body ?: "Unknown Qwen Error"
                """
                {
                  "thought": "Qwen API Call Failed",
                  "conversational_response": "عذراً، حدث خطأ أثناء الاتصال بـ Qwen: $errorMsg (كود الخطأ: ${response.code})",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            } catch (e: Exception) {
                android.util.Log.e("QwenProvider", "Error calling Qwen API", e)
                """
                {
                  "thought": "Network exception",
                  "conversational_response": "تعذر الاتصال بخوادم Qwen: ${e.localizedMessage}",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            }
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

class KimiAiProvider : AiProvider {
    override val name: String = "Kimi AI ✨ (Moonshot)"
    override val providerId: String = "kimi_api"
    
    override fun isAvailable(): Boolean = true

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(prompt: String, contextText: String): String {
        val apiKey = AiContextManager.kimiApiKey
        if (apiKey.isBlank() || apiKey == "YOUR_KIMI_API_KEY_HERE") {
            return """
                {
                  "thought": "No API Key configured",
                  "conversational_response": "يرجى تهيئة مفتاح API الخاص بـ Kimi AI في إعدادات التطبيق لتفعيله. ⚠️",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
            """.trimIndent()
        }

        val fullPrompt = "$prompt\n\n[DATABASE_CONTEXT]\n$contextText"
        val promptEscaped = escapeJsonString(fullPrompt)
        
        val jsonPayload = """
        {
          "model": "moonshot-v1-8k",
          "messages": [
            {
              "role": "system",
              "content": "You are Nexora AI Assistant. You must always return your response as a valid JSON object matching the requested schema. Do not output any markdown formatting (like ```json ... ```) outside the JSON."
            },
            {
              "role": "user",
              "content": "$promptEscaped"
            }
          ],
          "temperature": 0.3,
          "response_format": {
            "type": "json_object"
          }
        }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)
        
        val request = okhttp3.Request.Builder()
            .url("https://api.moonshot.cn/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val responseJson = org.json.JSONObject(body)
                    val choices = responseJson.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val text = message?.optString("content") ?: ""
                    
                    var cleanText = text.trim()
                    if (cleanText.startsWith("```json")) {
                        cleanText = cleanText.removePrefix("```json")
                        if (cleanText.endsWith("```")) {
                            cleanText = cleanText.removeSuffix("```")
                        }
                    } else if (cleanText.startsWith("```")) {
                        cleanText = cleanText.removePrefix("```")
                        if (cleanText.endsWith("```")) {
                            cleanText = cleanText.removeSuffix("```")
                        }
                    }
                    cleanText.trim()
                } else {
                    val errorBody = body ?: ""
                    val errorDetail = try {
                        val errJson = org.json.JSONObject(errorBody)
                        val errObj = errJson.optJSONObject("error")
                        errObj?.optString("message") ?: errorBody
                    } catch (e: Exception) {
                        errorBody.ifBlank { response.message }
                    }
                    """
                    {
                      "thought": "Kimi API call failed",
                      "conversational_response": "عذراً، حدث خطأ أثناء الاتصال بـ Kimi AI: $errorDetail (كود الخطأ: ${response.code})",
                      "tool_call": null,
                      "requires_confirmation": false,
                      "confirmation_message": ""
                    }
                    """.trimIndent()
                }
            } catch (e: Exception) {
                android.util.Log.e("KimiProvider", "Error calling Kimi API", e)
                """
                {
                  "thought": "Network exception",
                  "conversational_response": "عذراً، تعذر الاتصال بـ Kimi API. يرجى التحقق من جودة اتصال الإنترنت: ${e.localizedMessage}",
                  "tool_call": null,
                  "requires_confirmation": false,
                  "confirmation_message": ""
                }
                """.trimIndent()
            }
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
