package com.itihaasa.nammakathey.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST

interface GeminiApiService {
    @GET("models")
    suspend fun listModels(
        @Header("x-goog-api-key") apiKey: String
    ): GeminiModelsResponse

    @POST("{model}:generateContent")
    suspend fun generateContent(
        @Path("model", encoded = true) model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)

data class GeminiModelsResponse(
    val models: List<GeminiModel> = emptyList()
)

data class GeminiModel(
    val name: String = "",
    val supportedGenerationMethods: List<String> = emptyList()
)
