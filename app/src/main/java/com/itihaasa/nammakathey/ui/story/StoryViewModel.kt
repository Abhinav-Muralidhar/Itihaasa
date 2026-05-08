package com.itihaasa.nammakathey.ui.story

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.local.LocationsDataSource
import com.itihaasa.nammakathey.data.repository.StoryRepository
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.SectionOption
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
    private val locationsDataSource: LocationsDataSource,
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences(STORY_PREFERENCES, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()
    private val _selectedPage = MutableStateFlow(0)
    val selectedPage: StateFlow<Int> = _selectedPage.asStateFlow()
    private val _userChoice = MutableStateFlow<SectionOption?>(null)
    val userChoice: StateFlow<SectionOption?> = _userChoice.asStateFlow()

    init {
        _uiState.update {
            val savedLang = preferences.getString(KEY_STORY_LANG, "en") ?: "en"
            it.copy(
                lang = savedLang,
                currentLang = savedLang,
                isGoogleSignedIn = storyRepository.isGoogleSignedIn(),
                isSignedIn = storyRepository.isGoogleSignedIn()
            )
        }
    }

    fun loadStory(placeId: String, lang: String = _uiState.value.currentLang) {
        val place = locationsDataSource.getPlaceById(placeId) ?: run {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Could not find this place."
                )
            }
            return
        }
        loadStory(place, lang)
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
                lang = lang,
                currentLang = lang,
                isLoading = true,
                chatMessages = emptyList(),
                isChatLoading = false,
                badgeSaved = false,
                quizAnswers = emptyMap(),
                badgeEarned = false,
                rankUpRank = null,
                choiceMade = null,
                errorMessage = null
            )
        }
        _selectedPage.value = 0
        _userChoice.value = null

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

    fun onToggleLang() {
        val nextLang = if (_uiState.value.lang == "en") "kn" else "en"
        switchLanguage(nextLang)
    }

    fun onPageSelected(page: Int) {
        _selectedPage.value = page.coerceIn(0, STORY_PAGE_COUNT - 1)
    }

    fun onPageChanged(page: Int) {
        onPageSelected(page)
    }

    fun makeChoice(choice: SectionOption) {
        _userChoice.value = choice
        _uiState.update { it.copy(choiceMade = choice) }
    }

    fun onChoiceSelected(choice: SectionOption) {
        makeChoice(choice)
    }

    fun answerQuiz(questionIndex: Int, isCorrect: Boolean) {
        val story = _uiState.value.story ?: return
        val questionCount = story.quiz.size
        if (questionIndex !in 0 until questionCount) return

        val nextAnswers = _uiState.value.quizAnswers + (questionIndex to isCorrect)
        val earned = nextAnswers.size == questionCount && nextAnswers.values.all { it }
        _uiState.update {
            it.copy(
                quizAnswers = nextAnswers,
                badgeEarned = earned,
                badgeSaved = if (earned) it.badgeSaved else false
            )
        }
        if (earned) {
            saveBadge()
        }
    }

    fun onAnswerSelected(questionIndex: Int, answer: String) {
        val quiz = _uiState.value.story?.quiz ?: return
        val question = quiz.getOrNull(questionIndex) ?: return
        val isCorrect = answer == question.answer
        val newAnswers = _uiState.value.quizAnswers + (questionIndex to isCorrect)
        _uiState.update { it.copy(quizAnswers = newAnswers) }
    }

    fun onQuizComplete(passed: Boolean) {
        _uiState.update { it.copy(badgeEarned = passed) }
        if (passed && _uiState.value.isGoogleSignedIn) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value.place?.let { place ->
                    runCatching {
                        storyRepository.saveBadge(place)
                    }.onSuccess { rank ->
                        _uiState.update { state ->
                            state.copy(
                                badgeSaved = true,
                                rankUpRank = rank
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update { state ->
                            state.copy(authErrorMessage = throwable.message ?: "Could not save badge.")
                        }
                    }
                }
            }
        }
    }

    fun clearStory() {
        val signedIn = storyRepository.isGoogleSignedIn()
        _uiState.value = StoryUiState(
            lang = preferences.getString(KEY_STORY_LANG, "en") ?: "en",
            currentLang = preferences.getString(KEY_STORY_LANG, "en") ?: "en",
            isGoogleSignedIn = signedIn,
            isSignedIn = signedIn
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
                    cachedStoryText = story.narrativeText(),
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

    fun sendChatMessage(question: String) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank() || _uiState.value.isChatLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            val place = _uiState.value.place ?: return@launch
            val storyText = _uiState.value.story?.narrativeText().orEmpty()
            val userMsg = ChatMessage(text = trimmedQuestion, isUser = true)

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + userMsg,
                    isChatLoading = true
                )
            }

            runCatching {
                storyRepository.askHeritageGuide(
                    place = place,
                    cachedStoryText = storyText,
                    userQuestion = trimmedQuestion
                )
            }.onSuccess { response ->
                val aiMsg = ChatMessage(text = response, isUser = false)
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + aiMsg,
                        isChatLoading = false
                    )
                }
            }.onFailure { throwable ->
                val aiMsg = ChatMessage(
                    text = throwable.toStoryErrorMessage(),
                    isUser = false
                )
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + aiMsg,
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
                        isSignedIn = true,
                        isSigningIn = false,
                        authErrorMessage = null
                    )
                }
                if (_uiState.value.badgeEarned) {
                    saveBadge()
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

    fun onRetryQuiz() {
        _uiState.update {
            it.copy(
                quizAnswers = emptyMap(),
                badgeEarned = false,
                rankUpRank = null
            )
        }
    }

    fun onShareClick(context: Context, place: Place) {
        val shareText = "I just discovered ${place.name} in ${place.district}! " +
            "Explore Karnataka's heritage on Itihaasa"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share your achievement"))
    }

    fun saveBadge() {
        val place = _uiState.value.place ?: return
        if (!_uiState.value.isGoogleSignedIn || _uiState.value.isBadgeSaving) return

        _uiState.update { it.copy(isBadgeSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyRepository.saveBadge(place)
            }.onSuccess { rank ->
                _uiState.update {
                    it.copy(
                        isBadgeSaving = false,
                        badgeSaved = true,
                        rankUpRank = rank
                    )
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
            is TimeoutCancellationException -> "Story loading is taking too long. Please try again."
            else -> message ?: "Could not load this story."
        }
    }

    private fun com.itihaasa.nammakathey.model.Story.narrativeText(): String =
        sections.joinToString("\n\n") { it.text }

    private companion object {
        const val STORY_PAGE_COUNT = 6
        const val STORY_PREFERENCES = "story_preferences"
        const val KEY_STORY_LANG = "story_lang"
    }
}
