package com.itihaasa.nammakathey.ui.story

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.local.StoryCatalogDataSource
import com.itihaasa.nammakathey.data.local.DistrictDataSource
import com.itihaasa.nammakathey.data.repository.StoryProgressRepository
import com.itihaasa.nammakathey.data.repository.StoryRepository
import com.itihaasa.nammakathey.model.District
import com.itihaasa.nammakathey.model.QuizQuestion
import com.itihaasa.nammakathey.model.Story
import com.itihaasa.nammakathey.model.StoryCatalogEntry
import com.itihaasa.nammakathey.model.StorySection
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.MutedClay
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

@HiltViewModel
class StoryScreenViewModel @Inject constructor(
    private val storyCatalogDataSource: StoryCatalogDataSource,
    private val districtDataSource: DistrictDataSource,
    private val storyRepository: StoryRepository,
    private val storyProgressRepository: StoryProgressRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryScreenUiState())
    val uiState: StateFlow<StoryScreenUiState> = _uiState.asStateFlow()

    fun loadStory(placeId: String) {
        val hero = storyCatalogDataSource.getHeroByPlaceId(placeId) ?: run {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Could not find this hero.")
            }
            return
        }
        _uiState.update {
            it.copy(
                hero = hero,
                isLoading = true,
                errorMessage = null,
                badgeEarned = false,
                quizAnswers = emptyMap(),
                sectionChoices = emptyMap(),
                districts = districtDataSource.districts
            )
        }
        refreshProgressState()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyRepository.getStory(placeId, _uiState.value.lang)
            }.onSuccess { story ->
                _uiState.update {
                    it.copy(
                        story = story,
                        isLoading = false,
                        isSignedIn = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        story = null,
                        isLoading = false,
                        isSignedIn = false,
                        errorMessage = throwable.message ?: "Could not load story. Please try again."
                    )
                }
            }
        }
    }

    private fun refreshProgressState() {
        viewModelScope.launch(Dispatchers.IO) {
            val homeDistrict = sharedPreferences.getString(KEY_HOME_DISTRICT, null)
            val progressState = storyProgressRepository.getProgressState(homeDistrict)
            _uiState.update {
                it.copy(
                    districts = districtDataSource.districts,
                    activeDistrict = progressState.activeDistrict,
                    unlockedDistricts = progressState.unlockedDistricts,
                    completedHeroIds = progressState.completedHeroIds
                )
            }
        }
    }

    fun onOptionSelected(sectionIndex: Int, isOptionA: Boolean) {
        _uiState.update {
            it.copy(sectionChoices = it.sectionChoices + (sectionIndex to isOptionA))
        }
    }

    fun onToggleLang() {
        val newLang = if (_uiState.value.lang == "en") "kn" else "en"
        _uiState.update {
            it.copy(
                lang = newLang,
                story = null,
                isLoading = true,
                sectionChoices = emptyMap(),
                quizAnswers = emptyMap(),
                badgeEarned = false,
                errorMessage = null
            )
        }
        _uiState.value.hero?.let { hero ->
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    storyRepository.getStory(hero.placeId, newLang)
                }.onSuccess { story ->
                    _uiState.update {
                        it.copy(story = story, isLoading = false, errorMessage = null)
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            story = null,
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load story. Please try again."
                        )
                    }
                }
            }
        }
    }

    fun onAnswerSelected(questionIndex: Int, answer: String) {
        val story = _uiState.value.story ?: return
        if (questionIndex !in story.quiz.indices) return
        val newAnswers = _uiState.value.quizAnswers + (questionIndex to answer)
        _uiState.update { it.copy(quizAnswers = newAnswers) }
    }

    fun onQuizComplete() {
        val story = _uiState.value.story ?: return
        val answers = _uiState.value.quizAnswers
        val correct = story.quiz.indices.count { i ->
            answers[i] == story.quiz[i].answer
        }
        val passed = story.quiz.isNotEmpty() &&
            answers.size == story.quiz.size &&
            correct == story.quiz.size
        _uiState.update {
            it.copy(badgeEarned = passed)
        }
        if (passed) {
            _uiState.value.hero?.let { hero ->
                val updatedCompleted = storyProgressRepository.markHeroCompletedLocally(hero)
                val districtIsNowComplete = storyCatalogDataSource
                    .getStoriesByDistrict(hero.district)
                    .all { it.placeId in updatedCompleted }
                _uiState.update { state ->
                    state.copy(completedHeroIds = updatedCompleted)
                }
                viewModelScope.launch(Dispatchers.IO) {
                    storyProgressRepository.markHeroCompleted(hero)
                    if (districtIsNowComplete) {
                        storyProgressRepository.markDistrictCompleted(hero.district)
                    }
                }
            }
        }
    }

    fun remainingHeroesInDistrict(hero: StoryCatalogEntry): Int {
        val districtHeroes = storyCatalogDataSource.getStoriesByDistrict(hero.district)
        val completedCount = districtHeroes.count { it.placeId in _uiState.value.completedHeroIds }
        return (districtHeroes.size - completedCount).coerceAtLeast(0)
    }

    fun nextHeroInDistrict(hero: StoryCatalogEntry): StoryCatalogEntry? {
        val districtHeroes = storyCatalogDataSource.getStoriesByDistrict(hero.district)
        val currentIndex = districtHeroes.indexOfFirst { it.placeId == hero.placeId }
        if (currentIndex < 0) return null
        return districtHeroes.getOrNull(currentIndex + 1)
    }

    fun nextDistrictAfter(hero: StoryCatalogEntry): String? {
        val districtNames = districtDataSource.districts.map { it.name }
        val currentIndex = districtNames.indexOf(hero.district)
        if (currentIndex < 0) return null
        return districtNames.getOrNull(currentIndex + 1)
    }

    fun unlockNextDistrict(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val hero = _uiState.value.hero ?: return@launch
            val nextDistrict = nextDistrictAfter(hero)

            _uiState.update { it.copy(isUnlocking = true) }
            runCatching {
                if (nextDistrict != null) {
                    storyProgressRepository.unlockDistrict(nextDistrict)
                    storyProgressRepository.setActiveDistrict(nextDistrict)
                }
            }
            _uiState.update { state ->
                state.copy(
                    isUnlocking = false,
                    unlockedDistricts = if (nextDistrict != null) {
                        state.unlockedDistricts + nextDistrict
                    } else {
                        state.unlockedDistricts
                    },
                    activeDistrict = nextDistrict ?: state.activeDistrict
                )
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun onRetryQuiz() {
        _uiState.update {
            it.copy(
                quizAnswers = emptyMap(),
                badgeEarned = false
            )
        }
    }
}

data class StoryScreenUiState(
    val hero: StoryCatalogEntry? = null,
    val story: Story? = null,
    val lang: String = "en",
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = false,
    val sectionChoices: Map<Int, Boolean> = emptyMap(),
    val quizAnswers: Map<Int, String> = emptyMap(),
    val badgeEarned: Boolean = false,
    val districts: List<District> = emptyList(),
    val unlockedDistricts: Set<String> = emptySet(),
    val completedHeroIds: Set<String> = emptySet(),
    val activeDistrict: String? = null,
    val isUnlocking: Boolean = false,
    val errorMessage: String? = null
)

private const val KEY_HOME_DISTRICT = "home_district"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryScreen(
    placeId: String,
    viewModel: StoryScreenViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(placeId) { viewModel.loadStory(placeId) }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val story = uiState.story
    val totalSections = story?.sections?.size ?: 0
    val totalPages = if (story != null) totalSections + 3 else 1
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val currentPage = pagerState.currentPage
    val textToRead = story?.let {
        when {
            currentPage < totalSections -> it.sections[currentPage].text
            currentPage == totalSections -> it.didYouKnow.joinToString(". ")
            currentPage == totalSections + 1 -> it.quiz.getOrNull(uiState.quizAnswers.size)?.question.orEmpty()
            else -> uiState.hero?.title.orEmpty()
        }
    }.orEmpty()

    LaunchedEffect(placeId, uiState.lang) {
        pagerState.scrollToPage(0)
    }

    fun goToPage(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page.coerceIn(0, totalPages - 1))
        }
    }

    Scaffold(
        topBar = {
            StoryTopBar(
                currentPage = currentPage,
                totalPages = totalPages,
                lang = uiState.lang,
                textToRead = textToRead,
                onBack = onNavigateBack,
                onToggleLang = { viewModel.onToggleLang() }
            )
        },
        containerColor = Parchment
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = HeritageOchre
                )
            } else if (story == null) {
                Text(
                    text = uiState.errorMessage ?: "Could not load story. Please try again.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Charcoal
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val currentHero = uiState.hero ?: return@HorizontalPager
                    when {
                        page < totalSections ->
                            SectionPage(
                                section = story.sections[page],
                                choiceMade = uiState.sectionChoices[page],
                                isLastSection = page == totalSections - 1,
                                onOptionSelected = { isA ->
                                    viewModel.onOptionSelected(page, isA)
                                },
                                onContinue = { goToPage(page + 1) }
                            )
                        page == totalSections ->
                            DidYouKnowPage(
                                facts = story.didYouKnow,
                                onContinue = { goToPage(page + 1) }
                            )
                        page == totalSections + 1 ->
                            QuizPage(
                                quiz = story.quiz,
                                quizAnswers = uiState.quizAnswers,
                                onAnswerSelected = { q, a ->
                                    viewModel.onAnswerSelected(q, a)
                                },
                                onComplete = {
                                    viewModel.onQuizComplete()
                                    goToPage(page + 1)
                                }
                            )
                        page == totalSections + 2 ->
                            BadgePage(
                                hero = currentHero,
                                badgeEarned = uiState.badgeEarned,
                                remainingCount = viewModel.remainingHeroesInDistrict(currentHero),
                                nextHero = viewModel.nextHeroInDistrict(currentHero),
                                nextDistrict = viewModel.nextDistrictAfter(currentHero),
                                isUnlocking = uiState.isUnlocking,
                                onUnlockNextDistrict = {
                                    viewModel.unlockNextDistrict {
                                        onNavigateBack()
                                    }
                                },
                                onContinue = onNavigateBack,
                                onRetryQuiz = {
                                    viewModel.onRetryQuiz()
                                    goToPage(totalSections + 1)
                                }
                            )
                        else ->
                            DidYouKnowPage(
                                facts = story.didYouKnow,
                                onContinue = { goToPage(totalSections + 1) }
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryTopBar(
    currentPage: Int,
    totalPages: Int,
    lang: String,
    textToRead: String,
    onBack: () -> Unit,
    onToggleLang: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoyalIndigo)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null, tint = ParchmentLight)
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .width(if (index == currentPage) 20.dp else 6.dp)
                        .height(6.dp)
                        .background(
                            if (index == currentPage) {
                                HeritageOchre
                            } else if (index < currentPage) {
                                Color(0xFF27AE60)
                            } else {
                                ParchmentLight.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
        StoryTtsButton(text = textToRead, lang = lang)
        TextButton(onClick = onToggleLang) {
            Text(
                text = if (lang == "en") "ಕನ್ನಡ" else "EN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ParchmentLight
            )
        }
    }
}

@Composable
private fun StoryTtsButton(
    text: String,
    lang: String
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { isSpeaking = true }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }

            @Deprecated("Deprecated in Android TTS API")
            override fun onError(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }
        })
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    LaunchedEffect(lang, isReady) {
        if (isReady) {
            tts?.language = if (lang == "kn") {
                Locale.forLanguageTag("kn-IN")
            } else {
                Locale.forLanguageTag("en-IN")
            }
        }
    }

    LaunchedEffect(text) {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
        }
    }

    Surface(
        enabled = isReady && text.isNotBlank(),
        onClick = {
            val engine = tts
            if (engine != null) {
                if (isSpeaking) {
                    engine.stop()
                    isSpeaking = false
                } else {
                    engine.language = if (lang == "kn") {
                        Locale.forLanguageTag("kn-IN")
                    } else {
                        Locale.forLanguageTag("en-IN")
                    }
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "story-page")
                    isSpeaking = true
                }
            }
        },
        color = if (isSpeaking) HeritageOchre else ParchmentLight.copy(alpha = 0.12f),
        contentColor = if (isSpeaking) RoyalIndigo else ParchmentLight,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (isSpeaking) ParchmentLight else HeritageOchre.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.VolumeUp,
                contentDescription = "Read aloud",
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isSpeaking) "Playing" else "Listen",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SectionPage(
    section: StorySection,
    choiceMade: Boolean?,
    isLastSection: Boolean,
    onOptionSelected: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalIndigo.copy(alpha = 0.06f))
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = ParchmentLight,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.28f)),
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "THE STORY",
                            fontSize = 10.sp,
                            color = HeritageOchre,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = section.text,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Serif,
                            color = Charcoal,
                            lineHeight = 29.sp
                        )
                    }
                }
            }

            if (section.hasQuestion && !isLastSection) {
                item {
                    Surface(
                        color = RoyalIndigo,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.48f))
                    ) {
                        Text(
                            text = section.question.orEmpty(),
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Serif,
                            color = ParchmentLight,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        )
                    }
                }

                if (choiceMade == null) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OptionButton(
                                text = section.optionA?.text ?: "",
                                onClick = { onOptionSelected(true) },
                                modifier = Modifier.weight(1f)
                            )
                            OptionButton(
                                text = section.optionB?.text ?: "",
                                onClick = { onOptionSelected(false) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    item {
                        val feedback = if (choiceMade) {
                            section.optionA?.feedback
                        } else {
                            section.optionB?.feedback
                        }

                        Surface(
                            color = ParchmentLight,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "✦",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HeritageOchre
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = feedback ?: "",
                                    fontSize = 14.sp,
                                    color = Charcoal,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
            color = ParchmentLight
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val canContinue = !section.hasQuestion ||
                    isLastSection ||
                    choiceMade != null

                Button(
                    onClick = onContinue,
                    enabled = canContinue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeritageOchre,
                        disabledContainerColor = HeritageOchre.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isLastSection) "Next" else "Continue",
                        fontSize = 14.sp,
                        color = Parchment
                    )
                }
            }
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(1.dp, HeritageOchre),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = RoyalIndigo
        )
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun QuizPage(
    quiz: List<QuizQuestion>,
    quizAnswers: Map<Int, String>,
    onAnswerSelected: (Int, String) -> Unit,
    onComplete: () -> Unit
) {
    if (quiz.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Parchment),
            contentAlignment = Alignment.Center
        ) {
            Text("Quiz is not available yet.", color = Charcoal)
        }
        return
    }

    val currentIndex = quizAnswers.size.coerceAtMost(quiz.size - 1)
    val isComplete = quizAnswers.size == quiz.size

    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(1200)
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalIndigo.copy(alpha = 0.06f))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Surface(
            color = RoyalIndigo,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.52f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DISCOVERY QUIZ",
                    fontSize = 10.sp,
                    color = HeritageOchre,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Answer every question correctly to earn this badge.",
                    fontSize = 14.sp,
                    color = ParchmentLight.copy(alpha = 0.88f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quiz.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(
                            when {
                                i < quizAnswers.size && quizAnswers[i] == quiz[i].answer ->
                                    Color(0xFF27AE60)
                                i < quizAnswers.size ->
                                    MutedClay
                                i == currentIndex -> HeritageOchre
                                else -> RoyalIndigo.copy(alpha = 0.2f)
                            },
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val question = quiz[currentIndex]
        val selectedAnswer = quizAnswers[currentIndex]
        val isAnswered = selectedAnswer != null

        Surface(
            color = ParchmentLight,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.28f)),
            shadowElevation = 2.dp
        ) {
            Text(
                text = question.question,
                fontSize = 21.sp,
                fontFamily = FontFamily.Serif,
                color = RoyalIndigo,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        question.options.forEach { option ->
            val isCorrect = option == question.answer
            val isSelected = option == selectedAnswer

            val bgColor = when {
                !isAnswered -> ParchmentLight
                isCorrect -> Color(0xFF27AE60)
                isSelected && !isCorrect -> MutedClay
                else -> ParchmentLight
            }
            val textColor = when {
                (isCorrect || isSelected) && isAnswered -> Color.White
                else -> Charcoal
            }

            Surface(
                onClick = { if (!isAnswered) onAnswerSelected(currentIndex, option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = bgColor,
                border = BorderStroke(0.5.dp, RoyalIndigo.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (isAnswered && isCorrect) {
                        Text("✓", color = textColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isComplete) {
            Text(
                text = "Checking your answers...",
                modifier = Modifier.fillMaxWidth(),
                color = HeritageOchre,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BadgePage(
    hero: StoryCatalogEntry,
    badgeEarned: Boolean,
    remainingCount: Int,
    nextHero: StoryCatalogEntry?,
    nextDistrict: String?,
    isUnlocking: Boolean,
    onUnlockNextDistrict: () -> Unit,
    onContinue: () -> Unit,
    onRetryQuiz: () -> Unit
) {
    if (badgeEarned) {
        var burstVisible by remember { mutableStateOf(false) }
        LaunchedEffect(hero.placeId) {
            burstVisible = false
            delay(120)
            burstVisible = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalIndigo.copy(alpha = 0.98f))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CelebrationConfetti(
                    visible = burstVisible,
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = burstVisible,
                    enter = scaleIn(
                        animationSpec = spring(dampingRatio = 0.42f, stiffness = 260f)
                    ) + fadeIn()
                ) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .background(HeritageOchre.copy(alpha = 0.16f), CircleShape)
                            .border(2.dp, HeritageOchre, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hero.district.firstOrNull()?.uppercaseChar()?.toString() ?: "",
                            fontSize = 54.sp,
                            fontFamily = FontFamily.Serif,
                            color = Parchment
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hero badge earned",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                color = Parchment,
                textAlign = TextAlign.Center
            )
            Text(
                text = hero.district,
                fontSize = 16.sp,
                color = HeritageOchre,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You completed ${hero.title} and earned a hero badge.",
                fontSize = 14.sp,
                color = ParchmentLight.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ParchmentLight.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.48f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Achievement",
                        fontSize = 11.sp,
                        color = HeritageOchre,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${hero.title} badge",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        color = Parchment,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This badge is earned for a single hero story.",
                        fontSize = 13.sp,
                        color = ParchmentLight.copy(alpha = 0.84f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ParchmentLight,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.36f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val remainingText = when (remainingCount) {
                        0 -> "You have completed all stories in this district!"
                        1 -> "One more hero story is still waiting in this district."
                        else -> "$remainingCount more hero stories are waiting in this district."
                    }

                    Text(
                        text = remainingText,
                        fontSize = 14.sp,
                        color = Charcoal,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (remainingCount == 0) {
                        Text(
                            text = "District crest unlocked. " +
                                (nextDistrict?.let { "Next district: $it" } ?: "No next district available."),
                            fontSize = 12.sp,
                            color = Charcoal,
                            lineHeight = 17.sp
                        )
                    } else {
                        Text(
                            text = nextHero?.title?.let { "Next hero story: $it" }
                                ?: "No next hero story found.",
                            fontSize = 12.sp,
                            color = Charcoal,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (remainingCount == 0) {
                        if (nextDistrict != null) {
                            onUnlockNextDistrict()
                        } else {
                            onContinue()
                        }
                    } else {
                        onContinue()
                    }
                },
                enabled = !isUnlocking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (remainingCount == 0) HeritageOchre else RoyalIndigo,
                    contentColor = Parchment
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isUnlocking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Parchment,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when (remainingCount) {
                            0 -> if (nextDistrict != null) "Unlock next district ->" else "Back to map"
                            else -> "Continue to next story ->"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Parchment)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "So close!",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                color = RoyalIndigo,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You need every answer correct to earn the badge. Read the story once more and try again.",
                fontSize = 15.sp,
                color = Charcoal,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetryQuiz,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Try the quiz again", color = Parchment)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CelebrationConfetti(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "confettiProgress"
    )
    val pieces = remember {
        List(14) {
            ConfettiPiece(
                x = Random.nextInt(-180, 181).toFloat(),
                y = Random.nextInt(-170, 171).toFloat(),
                drift = Random.nextInt(24, 54).toFloat(),
                size = Random.nextInt(8, 14).dp,
                color = listOf(
                    HeritageOchre,
                    ParchmentLight,
                    Color(0xFF27AE60),
                    Color(0xFFF4C25A)
                ).random()
            )
        }
    }

    Box(modifier = modifier) {
        pieces.forEachIndexed { index, piece ->
            val sway = sin((progress * Math.PI * 2) + index).toFloat()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(piece.size)
                    .graphicsLayer {
                        translationX = piece.x * progress + sway * piece.drift
                        translationY = piece.y * progress - (progress * 80f)
                        rotationZ = progress * 280f + (index * 11f)
                        alpha = when {
                            progress <= 0f -> 0f
                            progress < 0.2f -> progress / 0.2f
                            else -> 1f - (progress - 0.2f) * 0.45f
                        }.coerceIn(0f, 1f)
                        scaleX = 0.6f + (progress * 0.6f)
                        scaleY = 0.6f + (progress * 0.6f)
                    }
                    .clip(CircleShape)
                    .background(piece.color)
            )
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val drift: Float,
    val size: androidx.compose.ui.unit.Dp,
    val color: Color
)

@Composable
fun DidYouKnowPage(
    facts: List<String>,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "DID YOU KNOW?",
            fontSize = 11.sp,
            color = HeritageOchre,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Surprising facts about this place",
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            color = RoyalIndigo
        )

        Spacer(modifier = Modifier.height(20.dp))

        facts.forEachIndexed { i, fact ->
            Surface(
                color = ParchmentLight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, HeritageOchre.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 18.sp,
                        color = HeritageOchre,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = fact,
                        fontSize = 14.sp,
                        color = Charcoal,
                        lineHeight = 21.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (facts.isEmpty()) {
            Surface(
                color = ParchmentLight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, HeritageOchre.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "More heritage facts will appear here after this story regenerates.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 14.sp,
                    color = Charcoal,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = HeritageOchre),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Start quiz",
                fontSize = 14.sp,
                color = Parchment
            )
        }
    }
}
