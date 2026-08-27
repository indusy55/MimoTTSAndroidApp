package com.indusy55.mimottsapp.data.api

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val audio: AudioConfig? = null,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class AudioConfig(
    val format: String = "wav",
    val voice: String? = null,
    @SerializedName("optimize_text_preview")
    val optimizeTextPreview: Boolean? = null
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessageWithAudio
)

data class ChatMessageWithAudio(
    val role: String,
    val content: String?,
    val audio: AudioResponse?
)

data class AudioResponse(
    val id: String,
    val data: String, // Base64 encoded audio
    @SerializedName("expires_at")
    val expiresAt: Long,
    val transcript: String?
)
