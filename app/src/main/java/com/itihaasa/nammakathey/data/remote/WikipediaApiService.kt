package com.itihaasa.nammakathey.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApiService {
    @GET("api/rest_v1/page/summary/{placeName}")
    suspend fun getPageSummary(
        @Path("placeName") placeName: String
    ): WikipediaSummaryResponse
}

data class WikipediaSummaryResponse(
    val thumbnail: WikipediaThumbnail? = null
)

data class WikipediaThumbnail(
    val source: String? = null
)
