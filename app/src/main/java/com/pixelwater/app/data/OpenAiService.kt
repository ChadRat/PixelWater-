package com.pixelwater.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenaiRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenaiMessage>,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "max_tokens") val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenaiMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenaiResponse(
    @Json(name = "choices") val choices: List<OpenaiChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenaiChoice(
    @Json(name = "message") val message: OpenaiMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

interface OpenaiApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenaiRequest
    ): OpenaiResponse
}

object OpenaiRetrofitClient {
    private const val BASE_URL = "https://api.openai.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: OpenaiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(OpenaiApiService::class.java)
    }
}
