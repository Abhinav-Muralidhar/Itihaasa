package com.itihaasa.nammakathey.model

data class Story(
    val placeId: String = "",
    val lang: String = "en",
    val heroName: String = "",
    val era: String = "",
    val significance: String = "",
    val sections: List<StorySection> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val didYouKnow: List<String> = emptyList(),
    val imageUrl: String? = null,
    val cacheType: String = "",
    val generatedAt: Long = System.currentTimeMillis()
)

data class StorySection(
    val text: String = "",
    val question: String? = null,
    val optionA: SectionOption? = null,
    val optionB: SectionOption? = null
) {
    val hasQuestion: Boolean get() = question != null
}

data class SectionOption(
    val text: String = "",
    val feedback: String = ""
)

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = ""
)
