package com.itihaasa.nammakathey.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itihaasa.nammakathey.model.StoryCatalogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryCatalogDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val stories: List<StoryCatalogEntry> by lazy {
        val json = context.assets.open("heroes.json")
            .bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<StoryCatalogEntry>>() {}.type
        Gson().fromJson(json, type)
    }

    fun getStoriesByDistrict(district: String): List<StoryCatalogEntry> =
        stories
            .filter { it.district.equals(district, ignoreCase = true) }
            .sortedBy { it.chronologicalOrder }

    fun getHeroByPlaceId(placeId: String): StoryCatalogEntry? =
        stories.firstOrNull { it.placeId == placeId }
}
