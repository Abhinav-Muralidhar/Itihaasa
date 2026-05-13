package com.itihaasa.nammakathey.model

enum class ExplorerRank(
    val title: String,
    val kannadaTitle: String,
    val badgesRequired: Int,
    val description: String
) {
    NAVARANGA("Navaranga", "Navaranga", 0, "A newcomer beginning their journey"),
    NAYAKA("Nayaka", "Nayaka", 3, "A local leader who is building steady story knowledge"),
    SENAPATI("Senapati", "Senapati", 8, "A commander who has completed stories across districts"),
    VEERA("Veera", "Veera", 15, "A warrior of proven courage and curiosity"),
    RAJADUTA("Rajaduta", "Rajaduta", 25, "An ambassador carrying Karnataka's heritage forward"),
    MAHAMANDALA("Mahamandala", "Mahamandala", 40, "A keeper of many district memories"),
    ITIHASA_RAKSHAKA("Itihasa Rakshaka", "Itihasa Rakshaka", 55, "A guardian whose journey spans most of Karnataka"),
    KARNATAKA_RATNA("Karnataka Ratna", "Karnataka Ratna", 66, "The jewel of Karnataka history")
}

fun Int.toExplorerRank(): ExplorerRank =
    ExplorerRank.entries
        .sortedByDescending { it.badgesRequired }
        .first { this >= it.badgesRequired }
