package com.itihaasa.nammakathey.model

data class Place(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val type: String = "",
    val district: String = "",
    val stateId: String = "KA",
    val era: String = "",
    val seedKeywords: List<String> = emptyList(),
    val historicalDate: String? = null
)