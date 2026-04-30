package com.itihaasa.nammakathey.model

data class Story(
    val placeId: String = "",
    val lang: String = "en",
    val heroName: String = "",
    val era: String = "",
    val storyText: String = "",
    val significance: String = "",
    val quiz: List<QuizQuestion> = emptyList(),
    val imageUrl: String? = null,
    val generatedAt: Long = System.currentTimeMillis()
)

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = ""
)