package com.indusy55.mimottsapp.service

import android.content.Context
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.google.gson.JsonParser
import com.indusy55.mimottsapp.data.api.AudioConfig
import com.indusy55.mimottsapp.data.api.ChatCompletionRequest
import com.indusy55.mimottsapp.data.api.ChatMessage
import com.indusy55.mimottsapp.data.api.MimoApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class MimoTtsService : TextToSpeechService() {
    private val TAG = "MimoTtsService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var synthesisJob: kotlinx.coroutines.Job? = null
    @Volatile private var isSynthesisStopped = false
    @Volatile private var synthesisGeneration = 0L
    @Volatile private var currentResponseBody: okhttp3.ResponseBody? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "TTS service created")
    }

    private val apiService: MimoApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.xiaomimimo.com/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MimoApiService::class.java)
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText.toString()
        Log.i(TAG, "Synthesis requested: ${text.length} characters")
        val prefs = getSharedPreferences("mimo_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""

        if (apiKey.isBlank()) {
            Log.e(TAG, "Synthesis rejected: API key is missing")
            callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET)
            return
        }

        val model = prefs.getString("selected_model", "mimo-v2.5-tts") ?: "mimo-v2.5-tts"
        val selectedVoice = prefs.getString("selected_voice", null)
            ?.takeIf { it.isNotBlank() } ?: "冰糖"
        val userPrompt = prefs.getString("user_prompt", "") ?: ""
        val styleTags = prefs.getString("style_tags", "") ?: ""

        var finalVoice: String? = when (model) {
            "mimo-v2.5-tts" -> selectedVoice
            "mimo-v2.5-tts-voiceclone" -> {
                val data = prefs.getString("active_clone_data", null)
                val mime = prefs.getString("active_clone_mime", "audio/wav")
                if (data != null) "data:$mime;base64,$data" else null
            }
            else -> null
        }

        val finalModel = if (model == "mimo-v2.5-tts-voiceclone" && finalVoice == null) "mimo-v2.5-tts" else model
        val safeVoice = if (finalModel == "mimo-v2.5-tts" && (finalVoice == null || finalVoice.startsWith("data"))) "冰糖" else finalVoice

        isSynthesisStopped = false
        val generation = ++synthesisGeneration
        try {
            callback.start(24000, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
        } catch (e: Exception) {
            Log.w(TAG, "TTS callback was closed before synthesis started", e)
            return
        }
        var deliveredAudioBytes = 0

        runBlocking {
            val job = launch(Dispatchers.IO) {
                try {
                    val messages = mutableListOf<ChatMessage>()
                    if (userPrompt.isNotBlank()) messages.add(ChatMessage(role = "user", content = userPrompt))
                    val contentWithTags = if (styleTags.isNotBlank()) "$styleTags$text" else text
                    messages.add(ChatMessage(role = "assistant", content = contentWithTags))
                    val apiRequest = ChatCompletionRequest(
                        model = finalModel,
                        messages = messages,
                        audio = AudioConfig(
                            format = if (finalModel == "mimo-v2.5-tts-voiceclone") "wav" else "pcm16",
                            voice = safeVoice,
                            optimizeTextPreview = if (finalModel == "mimo-v2.5-tts-voicedesign") true else null
                        ),
                        stream = true
                    )
                    currentResponseBody = apiService.streamChatCompletion("Bearer $apiKey", apiRequest)
                    val responseBody = currentResponseBody ?: return@launch
                    responseBody.use { body ->
                        body.source().use { source ->
                            while (!source.exhausted() && !isSynthesisStopped && generation == synthesisGeneration) {
                                val line = source.readUtf8Line() ?: break
                                if (!line.startsWith("data: ")) continue
                                val data = line.substring(6).trim()
                                if (data == "[DONE]") break
                                try {
                                    val root = JsonParser.parseString(data).asJsonObject
                                    val choices = root.getAsJsonArray("choices")
                                    val delta = choices?.firstOrNull()?.takeIf { it.isJsonObject }
                                        ?.asJsonObject?.get("delta")?.takeIf { it.isJsonObject }
                                        ?.asJsonObject
                                    val audio = delta?.get("audio")?.takeIf { it.isJsonObject }?.asJsonObject
                                    val audioData = audio?.get("data")?.takeIf { it.isJsonPrimitive }?.asString
                                    if (!audioData.isNullOrBlank()) {
                                        val bytes = android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
                                        deliveredAudioBytes += bytes.size
                                        var offset = 0
                                        while (offset < bytes.size && !isSynthesisStopped && generation == synthesisGeneration) {
                                            val length = minOf(4096, bytes.size - offset)
                                            if (callback.audioAvailable(bytes, offset, length) == TextToSpeech.STOPPED) {
                                                isSynthesisStopped = true
                                                break
                                            }
                                            offset += length
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Invalid TTS stream event: ${data.take(300)}", e)
                                }
                            }
                        }
                    }
                    if (generation == synthesisGeneration && !isSynthesisStopped) {
                        if (deliveredAudioBytes == 0) callback.error()
                        else callback.done()
                    }
                } catch (e: CancellationException) {
                    Log.i(TAG, "TTS synthesis cancelled")
                } catch (e: Exception) {
                    if (generation == synthesisGeneration && !isSynthesisStopped) {
                        Log.e(TAG, "Streaming failed", e)
                        runCatching { callback.error() }
                    }
                } finally {
                    currentResponseBody = null
                }
            }
            synthesisJob = job
            try {
                job.join()
            } finally {
                if (synthesisJob === job) synthesisJob = null
            }
        }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val language = lang?.lowercase(Locale.ROOT) ?: return TextToSpeech.LANG_NOT_SUPPORTED
        val supported = language == "zh" || language == "zho" || language == "chi" ||
            language == "en" || language == "eng"
        Log.i(TAG, "Language requested: lang=$lang country=$country variant=$variant supported=$supported")
        return if (supported) TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("zho", "CHN", "")
    }

    override fun onGetDefaultVoiceNameFor(lang: String, country: String, variant: String): String? {
        return when (lang.lowercase(Locale.ROOT)) {
            "zh", "zho", "chi" -> "zh-CN"
            "en", "eng" -> "en-US"
            else -> null
        }
    }

    override fun onGetVoices(): List<Voice> = listOf(
        Voice("zh-CN", Locale.SIMPLIFIED_CHINESE, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, true, emptySet()),
        Voice("en-US", Locale.US, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, true, emptySet())
    )

    // Accept ALL voice names — this engine synthesizes on-demand via API, no local voice data needed.
    // Rejecting a voice name causes the system to disconnect all TTS clients.
    override fun onIsValidVoiceName(voiceName: String?): Int = TextToSpeech.SUCCESS

    override fun onLoadVoice(voiceName: String?): Int {
        Log.i(TAG, "Loading voice: name=$voiceName")
        return TextToSpeech.SUCCESS
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        Log.i(TAG, "Loading language: lang=$lang country=$country variant=$variant")
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onStop() {
        isSynthesisStopped = true
        synthesisGeneration++
        synthesisJob?.cancel(CancellationException("TTS synthesis stopped"))
        synthesisJob = null
        currentResponseBody?.close()
        currentResponseBody = null
    }

    override fun onDestroy() {
        onStop()
        serviceScope.cancel()
        super.onDestroy()
    }
}
