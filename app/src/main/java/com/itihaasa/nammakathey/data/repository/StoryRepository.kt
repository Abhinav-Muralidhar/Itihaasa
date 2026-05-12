package com.itihaasa.nammakathey.data.repository

import com.itihaasa.nammakathey.data.local.OfflineStoryDataSource
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
}
