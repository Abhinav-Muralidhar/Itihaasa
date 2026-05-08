package com.itihaasa.nammakathey.data.local

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itihaasa.nammakathey.model.Place
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class LocationsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val places: List<Place> by lazy {
        val json = context.assets
            .open("locations.json")
            .bufferedReader()
            .use { it.readText() }
        val type = object : TypeToken<List<Place>>() {}.type
        Gson().fromJson(json, type)
    }

    fun getAllPlaces(): List<Place> = places

    fun getPlaceById(id: String): Place? = places.find { it.id == id }

    fun getPlacesByDistrict(district: String): List<Place> =
        getAllPlaces()
            .filter { it.district.equals(district, ignoreCase = true) }
            .sortedBy { it.chronologicalOrder }

    fun getDistrictCentroid(district: String): LatLng? {
        val districtPlaces = getPlacesByDistrict(district)
        if (districtPlaces.isEmpty()) return null
        val explicit = districtPlaces.firstOrNull {
            it.districtCentroidLat != null && it.districtCentroidLng != null
        }
        return explicit?.let { LatLng(it.districtCentroidLat ?: it.lat, it.districtCentroidLng ?: it.lng) }
            ?: LatLng(
                districtPlaces.map { it.lat }.average(),
                districtPlaces.map { it.lng }.average()
            )
    }

    fun searchPlaces(query: String): List<Place> {
        val q = query.lowercase().trim()
        return places.filter {
            it.name.lowercase().contains(q) ||
                    it.district.lowercase().contains(q) ||
                    it.seedKeywords.any { kw -> kw.lowercase().contains(q) }
        }
    }

    fun getPlacesNear(lat: Double, lng: Double, radiusKm: Double = 20.0): List<Place> {
        return places
            .map { it to distanceKm(lat, lng, it.lat, it.lng) }
            .filter { it.second <= radiusKm }
            .sortedBy { it.second }
            .map { it.first }
    }

    fun getTodayInHistory(): Place? {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val today = String.format("%02d-%02d", month, day)
        return places.find { it.historicalDate == today }
    }

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return R * 2 * asin(sqrt(a))
    }
}
