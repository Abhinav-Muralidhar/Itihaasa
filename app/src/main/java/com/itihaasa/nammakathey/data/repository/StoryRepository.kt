package com.itihaasa.nammakathey.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.itihaasa.nammakathey.BuildConfig
import com.itihaasa.nammakathey.data.remote.GeminiApiService
import com.itihaasa.nammakathey.data.remote.GeminiContent
import com.itihaasa.nammakathey.data.remote.GeminiGenerateContentRequest
import com.itihaasa.nammakathey.data.remote.GeminiGenerateContentResponse
import com.itihaasa.nammakathey.data.remote.GeminiPart
import com.itihaasa.nammakathey.data.remote.WikipediaApiService
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.QuizQuestion
import com.itihaasa.nammakathey.model.Story
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class StoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val geminiApiService: GeminiApiService,
    private val wikipediaApiService: WikipediaApiService,
    private val gson: Gson
) {
    suspend fun getStory(place: Place, lang: String = "en"): Story = withContext(Dispatchers.IO) {
        val docKey = storyDocKey(place, lang)
        val storyDocument = firestore.collection(STORIES_COLLECTION).document(docKey)
        val cachedStory = runCatching {
            storyDocument
                .get()
                .await()
                .toObject(Story::class.java)
        }.getOrNull()

        if (cachedStory != null) {
            return@withContext cachedStory
        }

        val imageUrl = fetchWikipediaImageUrl(place.name)
        val generatedStory = generateStory(place, lang, imageUrl)

        runCatching {
            val storyPayload = gson.fromJson(
                gson.toJson(generatedStory),
                MutableMap::class.java
            ).toMutableMap()
            storyPayload["authorId"] = ensureSignedInUserId()

            storyDocument
                .set(storyPayload)
                .await()
        }

        generatedStory
    }

    private suspend fun fetchWikipediaImageUrl(placeName: String): String? {
        return runCatching {
            wikipediaApiService.getPageSummary(placeName).thumbnail?.source
        }.getOrNull()
    }

    private suspend fun generateStory(
        place: Place,
        lang: String,
        imageUrl: String?
    ): Story {
        val apiKey = BuildConfig.GEMINI_API_KEY
        require(apiKey.isNotBlank()) { "Gemini API key is missing. Add GEMINI_API_KEY to local.properties." }

        val response = generateStoryWithRetry(
            apiKey = apiKey,
            request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(
                                "You are a historian specialising in Karnataka heritage. Generate heritage content for Place: ${place.name}, Type: ${place.type}, District: ${place.district}, Era hints: ${place.seedKeywords}, Language: $lang. Respond only with valid JSON and no markdown. Use this exact shape: { heroName, era, story, significance, quiz: [{q, options[4], answer}x3] }"
                            )
                        )
                    )
                )
            )
        )

        val json = response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            .orEmpty()
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsedStory = gson.fromJson(json, GeneratedStoryResponse::class.java)

        return Story(
            placeId = place.id,
            lang = lang,
            heroName = parsedStory.heroName,
            era = parsedStory.era.ifBlank { place.era },
            storyText = parsedStory.story,
            significance = parsedStory.significance,
            quiz = parsedStory.quiz.map {
                QuizQuestion(
                    question = it.q,
                    options = it.options,
                    answer = it.answer
                )
            },
            imageUrl = imageUrl,
            generatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun generateStoryWithRetry(
        apiKey: String,
        request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse {
        var lastError: Throwable? = null

        repeat(GEMINI_MAX_ATTEMPTS) { attempt ->
            try {
                return geminiApiService.generateContent(
                    apiKey = apiKey,
                    request = request
                )
            } catch (throwable: HttpException) {
                if (throwable.code() != HTTP_UNAVAILABLE || attempt == GEMINI_MAX_ATTEMPTS - 1) {
                    throw throwable
                }
                lastError = throwable
            }

            delay(GEMINI_RETRY_DELAYS_MS.getOrElse(attempt) { GEMINI_RETRY_DELAYS_MS.last() })
        }

        throw lastError ?: error("Gemini story generation failed.")
    }

    private fun storyDocKey(place: Place, lang: String): String {
        return "${place.stateId}_${place.id}_$lang"
    }

    private suspend fun ensureSignedInUserId(): String {
        firebaseAuth.currentUser?.let { return it.uid }
        return firebaseAuth.signInAnonymously().await().user?.uid
            ?: error("Anonymous sign-in failed.")
    }

    private data class GeneratedStoryResponse(
        val heroName: String = "",
        val era: String = "",
        val story: String = "",
        val significance: String = "",
        val quiz: List<GeneratedQuizQuestion> = emptyList()
    )

    private data class GeneratedQuizQuestion(
        val q: String = "",
        val options: List<String> = emptyList(),
        @SerializedName("answer")
        val answer: String = ""
    )

    private companion object {
        const val STORIES_COLLECTION = "stories"
        const val GEMINI_MAX_ATTEMPTS = 3
        const val HTTP_UNAVAILABLE = 503
        val GEMINI_RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L)
    }
}
