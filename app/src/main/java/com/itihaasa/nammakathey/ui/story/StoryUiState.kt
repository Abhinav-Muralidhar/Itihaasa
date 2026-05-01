package com.itihaasa.nammakathey.ui.story

import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.Story

data class StoryUiState(
    val place: Place? = null,
    val story: Story? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
