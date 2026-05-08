package com.itihaasa.nammakathey.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itihaasa.nammakathey.model.District
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistrictDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val districts: List<District> by lazy {
        val json = context.assets.open("districts.json")
            .bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<District>>() {}.type
        Gson().fromJson(json, type)
    }
}
