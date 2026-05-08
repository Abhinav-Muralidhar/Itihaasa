package com.itihaasa.nammakathey.ui.story

import com.itihaasa.nammakathey.model.ExplorerRank
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.SectionOption
import com.itihaasa.nammakathey.model.Story

data class StoryUiState(
    val place: Place? = null,
    val story: Story? = null,
    val lang: String = "en",
    val currentLang: String = "en",
    val isLoading: Boolean = false,
    val chatMessages: List<StoryChatMessage> = emptyList(),
    val isChatLoading: Boolean = false,
    val isGoogleSignedIn: Boolean = false,
    val isSignedIn: Boolean = false,
    val isSigningIn: Boolean = false,
    val authErrorMessage: String? = null,
    val isBadgeSaving: Boolean = false,
    val badgeSaved: Boolean = false,
    val rankUpRank: ExplorerRank? = null,
    val quizAnswers: Map<Int, Boolean> = emptyMap(),
    val badgeEarned: Boolean = false,
    val choiceMade: SectionOption? = null,
    val errorMessage: String? = null
)

data class StoryChatMessage(
    val text: String,
    val isUser: Boolean
)

typealias ChatMessage = StoryChatMessage
