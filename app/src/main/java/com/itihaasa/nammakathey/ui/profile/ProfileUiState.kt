package com.itihaasa.nammakathey.ui.profile

import com.itihaasa.nammakathey.data.repository.ProfileJourney

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileJourney? = null,
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null
)
