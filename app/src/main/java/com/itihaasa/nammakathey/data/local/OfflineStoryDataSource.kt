package com.itihaasa.nammakathey.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itihaasa.nammakathey.model.Story
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineStoryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val stories: List<Story> by lazy {
        val json = context.assets.open("stories.json")
            .bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Story>>() {}.type
        Gson().fromJson(json, type)
    }

    fun getStory(placeId: String, lang: String): Story? =
        stories.firstOrNull { it.placeId == placeId && it.lang == lang }
            ?: stories.firstOrNull { it.placeId == placeId && it.lang == DEFAULT_LANG }

    fun getStoryPlaceIds(lang: String = DEFAULT_LANG): Set<String> =
        stories.filter { it.lang == lang }.map { it.placeId }.toSet()

    private companion object {
        const val DEFAULT_LANG = "en"
    }
}
