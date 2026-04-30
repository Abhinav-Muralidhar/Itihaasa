package com.itihaasa.nammakathey.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val preferredLang: String = "en",
    val badgesEarned: List<String> = emptyList(),
    val placesExplored: List<ExploredPlace> = emptyList(),
    val quizStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val joinedAt: Long = System.currentTimeMillis()
)

data class ExploredPlace(
    val placeId: String = "",
    val name: String = "",
    val timestamp: Long = 0L,
    val badgeEarned: Boolean = false
)