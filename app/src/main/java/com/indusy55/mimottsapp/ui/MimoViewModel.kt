package com.indusy55.mimottsapp.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.indusy55.mimottsapp.data.api.AudioConfig
import com.indusy55.mimottsapp.data.api.ChatCompletionRequest
import com.indusy55.mimottsapp.data.api.ChatMessage
import com.indusy55.mimottsapp.data.api.MimoApiService
import com.indusy55.mimottsapp.data.models.AssetType
import com.indusy55.mimottsapp.data.models.VoiceAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MimoViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("mimo_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var apiKey by mutableStateOf(prefs.getString("api_key", "") ?: "")
        private set

    fun updateApiKey(newKey: String) {
        apiKey = newKey
        prefs.edit().putString("api_key", newKey).apply()
    }

    var selectedModel by mutableStateOf(prefs.getString("selected_model", "mimo-v2.5-tts") ?: "mimo-v2.5-tts")
        private set

    fun updateModel(model: String) {
        selectedModel = model
        prefs.edit().putString("selected_model", model).apply()
        selectedVoice = ""
    }

    var selectedVoice by mutableStateOf(prefs.getString("selected_voice", "") ?: "")
        private set

    fun updateVoice(voice: String) {
        selectedVoice = voice
        prefs.edit().putString("selected_voice", voice).apply()
    }

    var styleTags by mutableStateOf(prefs.getString("style_tags", "") ?: "")
        private set

    fun updateStyleTags(tags: String) {
        styleTags = tags
        prefs.edit().putString("style_tags", tags).apply()
    }

    var userPrompt by mutableStateOf(prefs.getString("user_prompt", "") ?: "")
        private set

    fun updateUserPrompt(prompt: String) {
        userPrompt = prompt
        prefs.edit().putString("user_prompt", prompt).apply()
    }

    var assistantContent by mutableStateOf("")

    var savedAssets = mutableStateListOf<VoiceAsset>()
        private set

    var voiceDescription by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var isApiOnline by mutableStateOf<Boolean?>(null)
    var error by mutableStateOf<String?>(null)

    fun checkApiConnection() {
        if (apiKey.isBlank()) {
            isApiOnline = false
            return
        }
        viewModelScope.launch {
            try {
                apiService.listModels("Bearer $apiKey").close()
                isApiOnline = true
            } catch (e: Exception) {
                isApiOnline = false
            }
        }
    }

    var lastGeneratedAudioBase64 by mutableStateOf<String?>(null)

    @Volatile private var synthesisGeneration = 0L
    @Volatile private var currentResponseBody: okhttp3.ResponseBody? = null
    private var currentJob: kotlinx.coroutines.Job? = null

    fun stopSynthesis() {
        synthesisGeneration++
        // Null out reference first so the IO coroutine can't read stale data
        val body = currentResponseBody
        currentResponseBody = null
        currentJob?.cancel()
        currentJob = null
        isLoading = false
        // Close response body off main thread to avoid NetworkOnMainThreadException
        viewModelScope.launch(Dispatchers.IO) {
            try { body?.close() } catch (_: Exception) {}
        }
    }

    init {
        loadAssets()
    }

    private fun loadAssets() {
        val json = prefs.getString("saved_assets", null)
        if (json != null) {
            val type = object : TypeToken<List<VoiceAsset>>() {}.type
            val list: List<VoiceAsset> = gson.fromJson(json, type)
            savedAssets.clear()
            savedAssets.addAll(list)
        }
    }

    private fun saveAssets() {
        val json = gson.toJson(savedAssets.toList())
        prefs.edit().putString("saved_assets", json).apply()
    }

    fun addAsset(name: String, type: AssetType, data: String, mimeType: String? = null) {
        val asset = VoiceAsset(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            type = type,
            data = data,
            mimeType = mimeType
        )
        savedAssets.add(asset)
        saveAssets()
    }

    fun updateAsset(updatedAsset: VoiceAsset) {
        val index = savedAssets.indexOfFirst { it.id == updatedAsset.id }
        if (index != -1) {
            savedAssets[index] = updatedAsset
            saveAssets()
        }
    }

    fun deleteAsset(asset: VoiceAsset) {
        if (selectedVoice == asset.name) updateVoice("")
        savedAssets.remove(asset)
        saveAssets()
    }

    fun selectAsset(asset: VoiceAsset) {
        if (asset.type == AssetType.DESIGN) {
            updateModel("mimo-v2.5-tts-voicedesign")
            updateVoice(asset.name)
            updateUserPrompt(asset.data)
        } else {
            updateModel("mimo-v2.5-tts-voiceclone")
            updateVoice(asset.name)
            prefs.edit()
                .putString("active_clone_data", asset.data)
                .putString("active_clone_name", asset.name)
                .putString("active_clone_mime", asset.mimeType)
                .apply()
        }
    }

    private val apiService: MimoApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl("https://api.xiaomimimo.com/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MimoApiService::class.java)
    }

    fun generateTTSStream(onAudioChunk: (ByteArray) -> Unit, onDone: () -> Unit) {
        if (isLoading) return
        if (assistantContent.isBlank()) {
            error = getApplication<Application>().getString(com.indusy55.mimottsapp.R.string.empty_content_error)
            return
        }
        if (apiKey.isBlank()) { error = "Please set API Key in settings"; return }

        val generation = ++synthesisGeneration
        isLoading = true
        error = null
        currentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = mutableListOf<ChatMessage>()
                var finalVoice: String? = null
                var finalUserPrompt = userPrompt

                when (selectedModel) {
                    "mimo-v2.5-tts" -> {
                        finalVoice = if (selectedVoice.isBlank()) "冰糖" else selectedVoice
                    }
                    "mimo-v2.5-tts-voicedesign" -> {
                        val asset = savedAssets.find { it.type == AssetType.DESIGN && it.name == selectedVoice }
                        finalUserPrompt = asset?.data ?: userPrompt
                        if (finalUserPrompt.isBlank()) finalUserPrompt = "Give me a young male tone."
                    }
                    "mimo-v2.5-tts-voiceclone" -> {
                        val asset = savedAssets.find { it.type == AssetType.CLONE && it.name == selectedVoice }
                        if (asset != null) {
                            val mime = asset.mimeType ?: "audio/wav"
                            finalVoice = "data:$mime;base64,${asset.data}"
                        }
                    }
                }

                if (finalUserPrompt.isNotBlank()) messages.add(ChatMessage(role = "user", content = finalUserPrompt))
                val finalAssistantContent = if (styleTags.isNotBlank()) "$styleTags$assistantContent" else assistantContent
                messages.add(ChatMessage(role = "assistant", content = finalAssistantContent))

                val request = ChatCompletionRequest(
                    model = selectedModel,
                    messages = messages,
                    audio = AudioConfig(
                        format = if (selectedModel == "mimo-v2.5-tts-voiceclone") "wav" else "pcm16",
                        voice = finalVoice,
                        optimizeTextPreview = if (selectedModel == "mimo-v2.5-tts-voicedesign") true else null
                    ),
                    stream = true
                )
                currentResponseBody = apiService.streamChatCompletion("Bearer $apiKey", request)
                val responseBody = currentResponseBody ?: return@launch
                responseBody.use { body ->
                    val reader = body.byteStream().bufferedReader()
                    reader.forEachLine { line ->
                        if (generation != synthesisGeneration) return@forEachLine
                        if (!line.startsWith("data: ")) return@forEachLine
                        val data = line.substring(6).trim()
                        if (data == "[DONE]") return@forEachLine
                        try {
                            val root = com.google.gson.JsonParser.parseString(data).asJsonObject
                            val choices = root.getAsJsonArray("choices")
                            val delta = choices?.firstOrNull()?.takeIf { it.isJsonObject }
                                ?.asJsonObject?.get("delta")?.takeIf { it.isJsonObject }
                                ?.asJsonObject
                            val audio = delta?.get("audio")?.takeIf { it.isJsonObject }
                                ?.asJsonObject
                            val audioData = audio?.get("data")?.takeIf { it.isJsonPrimitive }
                                ?.asString
                            if (!audioData.isNullOrBlank()) {
                                val bytes = android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
                                if (bytes.isNotEmpty() && generation == synthesisGeneration) onAudioChunk(bytes)
                            }
                        } catch (e: Exception) {
                            Log.w("MimoViewModel", "Invalid TTS stream event: ${data.take(300)}", e)
                        }
                    }
                }
                if (generation == synthesisGeneration) onDone()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Stopped by user — normal path
            } catch (e: Exception) {
                if (generation == synthesisGeneration) error = e.message ?: "Unknown error"
            } finally {
                withContext(Dispatchers.Main) {
                    if (generation == synthesisGeneration) {
                        currentResponseBody = null
                        currentJob = null
                        isLoading = false
                    }
                }
            }
        }
    }
}
