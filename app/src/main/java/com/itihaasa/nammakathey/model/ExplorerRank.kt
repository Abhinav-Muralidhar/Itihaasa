package com.itihaasa.nammakathey.model

enum class ExplorerRank(
    val title: String,
    val kannadaTitle: String,
    val badgesRequired: Int,
    val description: String
) {
    NAVARANGA("Navaranga", "ನವರಂಗ", 0, "A newcomer beginning their journey"),
    NAYAKA("Nayaka", "ನಾಯಕ", 1, "A local leader who knows their district"),
    SENAPATI("Senapati", "ಸೇನಾಪತಿ", 5, "A commander who has led many battles"),
    VEERA("Veera", "ವೀರ", 10, "A warrior of proven courage"),
    RAJADUTA("Rajaduta", "ರಾಜದೂತ", 20, "An ambassador of Karnataka's heritage"),
    KARNATAKA_RATNA("Karnataka Ratna", "ಕರ್ನಾಟಕ ರತ್ನ", 30, "The jewel of Karnataka history")
}

fun Int.toExplorerRank(): ExplorerRank =
    ExplorerRank.entries
        .sortedByDescending { it.badgesRequired }
        .first { this >= it.badgesRequired }
