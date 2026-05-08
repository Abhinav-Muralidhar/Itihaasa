package com.itihaasa.nammakathey.data.repository

import com.itihaasa.nammakathey.data.local.OfflineStoryDataSource
import com.itihaasa.nammakathey.model.ExplorerRank
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.Story
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StoryRepository @Inject constructor(
    private val offlineStoryDataSource: OfflineStoryDataSource
) {
    suspend fun getStory(placeId: String, lang: String = "en"): Story = withContext(Dispatchers.IO) {
        offlineStoryDataSource.getStory(placeId, lang)
            ?: error("No offline story found for $placeId.")
    }

    suspend fun getStory(place: Place, lang: String = "en"): Story = getStory(place.id, lang)

    fun isGoogleSignedIn(): Boolean = false

    suspend fun signInWithGoogle(idToken: String) {
        // Story reading is fully offline now; sign-in is intentionally not used here.
    }

    suspend fun saveBadge(place: Place): ExplorerRank? = null

    suspend fun askHeritageGuide(
        place: Place,
        cachedStoryText: String,
        userQuestion: String
    ): String = "The story guide is offline. Read the saved story sections and quiz for this hero."
}
