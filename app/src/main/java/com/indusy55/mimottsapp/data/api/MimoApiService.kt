package com.indusy55.mimottsapp.data.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface MimoApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") auth: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @Streaming
    @POST("chat/completions")
    suspend fun streamChatCompletion(
        @Header("Authorization") auth: String,
        @Body request: ChatCompletionRequest
    ): ResponseBody

    @retrofit2.http.GET("models")
    suspend fun listModels(
        @Header("Authorization") auth: String
    ): ResponseBody
}
