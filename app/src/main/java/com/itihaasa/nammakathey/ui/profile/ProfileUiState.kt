package com.itihaasa.nammakathey.ui.profile

import com.itihaasa.nammakathey.data.repository.ProfileJourney

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileJourney? = null,
    val isSigningIn: Boolean = false,
    val isSigningOut: Boolean = false,
    val rewardCards: List<RewardCardUiModel> = emptyList(),
    val districtProgress: Map<String, DistrictProgressUiModel> = emptyMap(),
    val completedDistrictCount: Int = 0,
    val errorMessage: String? = null
)

data class DistrictProgressUiModel(
    val district: String,
    val completed: Int,
    val total: Int
) {
    val isComplete: Boolean
        get() = total > 0 && completed >= total
}
