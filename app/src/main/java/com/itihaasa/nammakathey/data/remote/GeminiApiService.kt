package com.itihaasa.nammakathey.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiApiService {
    @POST(GEMINI_GENERATE_CONTENT_PATH)
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

private const val GEMINI_GENERATE_CONTENT_PATH =
    "models/gemma-3-4b-it:generateContent"

data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>
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
