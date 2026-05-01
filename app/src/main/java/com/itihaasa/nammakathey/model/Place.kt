package com.itihaasa.nammakathey.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class Place(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val type: PlaceType = PlaceType.HERO_SITE,
    val district: String = "",
    val stateId: String = "KA",
    val era: String = "",
    val seedKeywords: List<String> = emptyList(),
    val historicalDate: String? = null
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(lat, lng)
    override fun getTitle(): String = name
    override fun getSnippet(): String = "$district - $era"
    override fun getZIndex(): Float = 0f
}

enum class PlaceType {
    FORT,
    TEMPLE,
    HERO_SITE,
    BATTLEFIELD,
    REFORM_SITE
}
