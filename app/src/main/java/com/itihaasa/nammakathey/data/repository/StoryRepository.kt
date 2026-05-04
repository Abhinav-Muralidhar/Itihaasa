package com.itihaasa.nammakathey.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        val storyDocument = firestore.collection(PLACES_COLLECTION).document(docKey)
        Log.d("StoryCache", "Checking: $docKey")
        val cachedStory = runCatching {
            storyDocument
                .get()
                .await()
                .toObject(Story::class.java)
        }.getOrNull()
        Log.d("StoryCache", "Cache hit: ${cachedStory != null}")

        if (cachedStory != null) {
            if (lang == KANNADA_LANG && !cachedStory.storyText.isMostlyKannada()) {
                val regeneratedStory = generateStory(place, lang)
                Log.d("StoryCache", "Saving to Firestore: $docKey")
                saveStoryDocument(storyDocument, regeneratedStory)
                return@withContext regeneratedStory
            }
            if (cachedStory.imageUrl.isNullOrBlank()) {
                val imageUrl = fetchWikipediaImageUrl(cachedStory.wikipediaImageQuery ?: place.name)
                if (!imageUrl.isNullOrBlank()) {
                    runCatching {
                        ensureStoryCacheSession()
                        storyDocument
                            .set(
                                mapOf(
                                    "imageUrl" to imageUrl,
                                    "cacheType" to STORY_CACHE_TYPE
                                ),
                                SetOptions.merge()
                            )
                            .await()
                    }
                    return@withContext cachedStory.copy(imageUrl = imageUrl)
                }
            }
            return@withContext cachedStory
        }

        val generatedStory = generateStory(place, lang)

        Log.d("StoryCache", "Saving to Firestore: $docKey")
        saveStoryDocument(storyDocument, generatedStory)

        generatedStory
    }

    private suspend fun saveStoryDocument(
        storyDocument: com.google.firebase.firestore.DocumentReference,
        story: Story
    ) {
        try {
            ensureStoryCacheSession()
            val storyPayload = gson.fromJson(
                gson.toJson(story),
                MutableMap::class.java
            ).toMutableMap()
            storyPayload["cacheType"] = STORY_CACHE_TYPE

            storyDocument
                .set(storyPayload)
                .await()
            Log.d("StoryCache", "Saved successfully")
        } catch (e: Exception) {
            Log.e("StoryCache", "Save failed: ${e.message}", e)
        }
    }

    private suspend fun fetchWikipediaImageUrl(placeName: String): String? {
        return try {
            fetchWikipediaThumbnail(placeName)
                ?: placeName
                    .trim()
                    .split(Regex("\\s+"))
                    .take(2)
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() && it != placeName.trim() }
                    ?.let { fallbackName -> fetchWikipediaThumbnail(fallbackName) }
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun fetchWikipediaThumbnail(placeName: String): String? {
        val encodedName = placeName.trim().replace(" ", "_")
        if (encodedName.isBlank()) return null

        return wikipediaApiService
            .getPageSummary(encodedName)
            .thumbnail
            ?.source
            ?.let { source ->
                if (source.startsWith("http://")) {
                    source.replaceFirst("http://", "https://")
                } else {
                    source
                }
            }
    }

    fun isGoogleSignedIn(): Boolean {
        val user = firebaseAuth.currentUser
        return user != null && !user.isAnonymous
    }

    suspend fun signInWithGoogle(idToken: String) = withContext(Dispatchers.IO) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = firebaseAuth.signInWithCredential(credential).await().user
            ?: error("Google sign-in failed.")

        val userDocument = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = userDocument.get().await()
        if (!snapshot.exists()) {
            userDocument.set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to user.displayName.orEmpty(),
                    "photoUrl" to (user.photoUrl?.toString().orEmpty()),
                    "preferredLang" to "en",
                    "badgesEarned" to emptyList<Map<String, Any>>(),
                    "placesExplored" to emptyList<Map<String, Any>>(),
                    "quizStreak" to 0,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            userDocument.set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to user.displayName.orEmpty(),
                    "photoUrl" to (user.photoUrl?.toString().orEmpty())
                ),
                SetOptions.merge()
            ).await()
        }
    }

    suspend fun saveBadge(place: Place) = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
        if (user == null || user.isAnonymous) return@withContext

        val badge = mapOf(
            "placeId" to place.id,
            "placeName" to place.name,
            "district" to place.district,
            "earnedAt" to System.currentTimeMillis()
        )
        val exploredPlace = mapOf(
            "placeId" to place.id,
            "name" to place.name,
            "timestamp" to System.currentTimeMillis(),
            "badgeEarned" to true
        )

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "badgesEarned" to FieldValue.arrayUnion(badge),
                    "placesExplored" to FieldValue.arrayUnion(exploredPlace),
                    "quizStreak" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun askHeritageGuide(
        place: Place,
        cachedStoryText: String,
        userQuestion: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        require(apiKey.isNotBlank()) { "Gemini API key is missing. Add GEMINI_API_KEY to local.properties." }

        val prompt = """
            You are a knowledgeable heritage guide at ${place.name}, Karnataka.
            Answer in simple English. Max 100 words. Context: $cachedStoryText
            User: $userQuestion
        """.trimIndent()

        val response = generateStoryWithRetry(
            apiKey = apiKey,
            request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(prompt))
                    )
                )
            )
        )

        return response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            .orEmpty()
            .trim()
            .ifBlank { "I could not find a clear answer for that." }
    }

    private suspend fun generateStory(
        place: Place,
        lang: String
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
                                storyPrompt(place = place, lang = lang)
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
        val wikipediaImageQuery = parsedStory.wikipediaImageQuery.ifBlank { place.name }
        val imageUrl = fetchWikipediaImageUrl(wikipediaImageQuery)

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
            wikipediaImageQuery = wikipediaImageQuery,
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

    private fun storyPrompt(place: Place, lang: String): String {
        val languageInstruction = if (lang == KANNADA_LANG) {
            "STRICT REQUIREMENT: All JSON string values intended for users must be written exclusively in Kannada script. This includes heroName, era, story, significance, every quiz q, every option, and every answer. JSON field names must remain exactly as requested in English. Do not use English, Hindi, Tamil, Telugu, or Roman-script words inside any value. Transliterate proper nouns into Kannada script."
        } else {
            "Write every human-readable value in simple English."
        }

        return "You are a historian specialising in Karnataka heritage. Generate heritage content for Place: ${place.name}, Type: ${place.type}, District: ${place.district}, Era hints: ${place.seedKeywords}, Language code: $lang. $languageInstruction Also choose the best exact English Wikipedia page title to search for an image of this place. Respond only with valid JSON and no markdown. Use this exact shape: { heroName, era, story, significance, quiz: [{q, options[4], answer}x3], wikipediaImageQuery: \"exact Wikipedia page title\" }"
    }

    private fun String.isMostlyKannada(): Boolean {
        val letters = filter { it.isLetter() }
        if (letters.isBlank()) return false

        val kannadaLetters = letters.count { it in '\u0C80'..'\u0CFF' }
        val disallowedScriptLetters = letters.count {
            it in 'A'..'Z' ||
                it in 'a'..'z' ||
                it in '\u0C00'..'\u0C7F' ||
                it in '\u0B80'..'\u0BFF'
        }

        return kannadaLetters >= letters.length * 0.7 && disallowedScriptLetters <= letters.length * 0.08
    }

    private suspend fun ensureStoryCacheSession() {
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().await().user
                ?: error("Anonymous sign-in failed.")
        }
        Log.d("StoryCache", "Auth uid: ${firebaseAuth.currentUser?.uid}")
    }

    private data class GeneratedStoryResponse(
        val heroName: String = "",
        val era: String = "",
        val story: String = "",
        val significance: String = "",
        val quiz: List<GeneratedQuizQuestion> = emptyList(),
        val wikipediaImageQuery: String = ""
    )

    private data class GeneratedQuizQuestion(
        val q: String = "",
        val options: List<String> = emptyList(),
        @SerializedName("answer")
        val answer: String = ""
    )

    private companion object {
        const val PLACES_COLLECTION = "places"
        const val USERS_COLLECTION = "users"
        const val STORY_CACHE_TYPE = "story"
        const val KANNADA_LANG = "kn"
        const val GEMINI_MAX_ATTEMPTS = 3
        const val HTTP_UNAVAILABLE = 503
        val GEMINI_RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L)
    }
}
