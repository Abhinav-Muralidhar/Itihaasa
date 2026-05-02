package com.itihaasa.nammakathey.ui.story

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.repository.StoryRepository
import com.itihaasa.nammakathey.model.Place
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences(STORY_PREFERENCES, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                currentLang = preferences.getString(KEY_STORY_LANG, "en") ?: "en",
                isGoogleSignedIn = storyRepository.isGoogleSignedIn()
            )
        }
    }

    fun loadStory(place: Place, lang: String = _uiState.value.currentLang) {
        if (
            _uiState.value.place?.id == place.id &&
            _uiState.value.currentLang == lang &&
            _uiState.value.story != null
        ) {
            return
        }

        _uiState.update {
            it.copy(
                place = place,
                story = null,
                currentLang = lang,
                isLoading = true,
                chatMessages = emptyList(),
                isChatLoading = false,
                badgeSaved = false,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyRepository.getStory(place, lang)
            }.onSuccess { story ->
                _uiState.update {
                    it.copy(story = story, isLoading = false, errorMessage = null)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.toStoryErrorMessage()
                    )
                }
            }
        }
    }

    fun switchLanguage(lang: String) {
        if (lang != "en" && lang != "kn") return
        preferences.edit().putString(KEY_STORY_LANG, lang).apply()
        val place = _uiState.value.place ?: return
        loadStory(place, lang)
    }

    fun clearStory() {
        _uiState.value = StoryUiState(
            currentLang = preferences.getString(KEY_STORY_LANG, "en") ?: "en",
            isGoogleSignedIn = storyRepository.isGoogleSignedIn()
        )
    }

    fun sendChatQuestion(question: String) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank() || _uiState.value.isChatLoading) return

        val place = _uiState.value.place ?: return
        val story = _uiState.value.story ?: return

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + StoryChatMessage(
                    text = trimmedQuestion,
                    isUser = true
                ),
                isChatLoading = true
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyRepository.askHeritageGuide(
                    place = place,
                    cachedStoryText = story.storyText,
                    userQuestion = trimmedQuestion
                )
            }.onSuccess { answer ->
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + StoryChatMessage(
                            text = answer,
                            isUser = false
                        ),
                        isChatLoading = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + StoryChatMessage(
                            text = throwable.toStoryErrorMessage(),
                            isUser = false
                        ),
                        isChatLoading = false
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.update {
            it.copy(isSigningIn = true, authErrorMessage = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyRepository.signInWithGoogle(idToken)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isGoogleSignedIn = true,
                        isSigningIn = false,
                        authErrorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        authErrorMessage = throwable.message ?: "Google sign-in failed."
                    )
                }
            }
        }
    }

    fun saveBadge() {
        val place = _uiState.value.place ?: return
        if (!_uiState.value.isGoogleSignedIn || _uiState.value.isBadgeSaving) return

        _uiState.update { it.copy(isBadgeSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyRepository.saveBadge(place)
            }.onSuccess {
                _uiState.update {
                    it.copy(isBadgeSaving = false, badgeSaved = true)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isBadgeSaving = false,
                        authErrorMessage = throwable.message ?: "Could not save badge."
                    )
                }
            }
        }
    }

    private fun Throwable.toStoryErrorMessage(): String {
        return when (this) {
            is SocketTimeoutException,
            is TimeoutCancellationException -> "Story generation is taking too long. Please try again."
            else -> message ?: "Could not load this story."
        }
    }

    private companion object {
        const val STORY_PREFERENCES = "story_preferences"
        const val KEY_STORY_LANG = "story_lang"
    }
}
