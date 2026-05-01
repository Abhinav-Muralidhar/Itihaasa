package com.itihaasa.nammakathey.ui.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.repository.StoryRepository
import com.itihaasa.nammakathey.model.Place
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
    private val storyRepository: StoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    fun loadStory(place: Place, lang: String = "en") {
        if (_uiState.value.place?.id == place.id && _uiState.value.story != null) return

        _uiState.update {
            it.copy(
                place = place,
                story = null,
                isLoading = true,
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

    fun clearStory() {
        _uiState.value = StoryUiState()
    }

    private fun Throwable.toStoryErrorMessage(): String {
        return when (this) {
            is SocketTimeoutException,
            is TimeoutCancellationException -> "Story generation is taking too long. Please try again."
            else -> message ?: "Could not load this story."
        }
    }
}
