package com.itihaasa.nammakathey.ui.story

import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.itihaasa.nammakathey.R
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.QuizQuestion
import com.itihaasa.nammakathey.model.Story
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentVariant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryBottomSheet(
    uiState: StoryUiState,
    onQuestionSubmitted: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onGoogleSignInToken: (String) -> Unit,
    onSaveBadge: () -> Unit,
    onDismiss: () -> Unit
) {
    val place = uiState.place ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    var contentVisible by remember(place.id) { mutableStateOf(false) }
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
                .idToken
        }.getOrNull()?.let(onGoogleSignInToken)
    }

    LaunchedEffect(place.id) {
        contentVisible = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.85f),
        containerColor = Parchment
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 4 }
            ) + fadeIn(animationSpec = tween(durationMillis = 260))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Parchment)
                    .parchmentTexture()
            ) {
                when {
                    uiState.isLoading -> StorySkeleton(place)
                    uiState.story != null -> StoryContent(
                        place = place,
                        story = uiState.story,
                        chatMessages = uiState.chatMessages,
                        isChatLoading = uiState.isChatLoading,
                        currentLang = uiState.currentLang,
                        isGoogleSignedIn = uiState.isGoogleSignedIn,
                        isSigningIn = uiState.isSigningIn,
                        authErrorMessage = uiState.authErrorMessage,
                        isBadgeSaving = uiState.isBadgeSaving,
                        badgeSaved = uiState.badgeSaved,
                        onQuestionSubmitted = onQuestionSubmitted,
                        onLanguageSelected = onLanguageSelected,
                        onStartGoogleSignIn = {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        onSaveBadge = onSaveBadge
                    )
                    uiState.errorMessage != null -> StoryError(place = place, message = uiState.errorMessage)
                    else -> StorySkeleton(place)
                }
            }
        }
    }
}

@Composable
private fun StoryContent(
    place: Place,
    story: Story,
    chatMessages: List<StoryChatMessage>,
    isChatLoading: Boolean,
    currentLang: String,
    isGoogleSignedIn: Boolean,
    isSigningIn: Boolean,
    authErrorMessage: String?,
    isBadgeSaving: Boolean,
    badgeSaved: Boolean,
    onQuestionSubmitted: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onStartGoogleSignIn: () -> Unit,
    onSaveBadge: () -> Unit
) {
    var chatInput by rememberSaveable(place.id) { mutableStateOf("") }
    var quizMode by rememberSaveable(place.id, story.lang) { mutableStateOf(QuizMode.Reading) }
    var quizCanSaveBadge by rememberSaveable(place.id, story.lang) { mutableStateOf(false) }
    var selectedAnswers by rememberSaveable(place.id, story.lang) {
        mutableStateOf(List(story.quiz.take(MAX_QUIZ_QUESTIONS).size) { -1 })
    }
    var signInAction by rememberSaveable { mutableStateOf(PendingSignInAction.None) }
    var showStartSignInDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveSignInDialog by rememberSaveable { mutableStateOf(false) }
    val quizQuestions = story.quiz.take(MAX_QUIZ_QUESTIONS)
    val chapters = remember(story.storyText, story.significance, story.lang) {
        story.toChapterSections()
    }
    var selectedChapterIndex by rememberSaveable(place.id, story.lang) { mutableStateOf(0) }
    val selectedChapter = chapters.getOrElse(selectedChapterIndex) { chapters.first() }

    LaunchedEffect(story.placeId, story.lang, quizQuestions.size) {
        selectedAnswers = List(quizQuestions.size) { -1 }
        quizMode = QuizMode.Reading
        quizCanSaveBadge = false
        selectedChapterIndex = 0
    }

    LaunchedEffect(isGoogleSignedIn, signInAction) {
        if (!isGoogleSignedIn) return@LaunchedEffect
        when (signInAction) {
            PendingSignInAction.StartQuiz -> {
                quizCanSaveBadge = true
                quizMode = QuizMode.Quiz
            }
            PendingSignInAction.SaveBadge -> onSaveBadge()
            PendingSignInAction.None -> Unit
        }
        signInAction = PendingSignInAction.None
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        StoryImage(place = place, story = story)
        StoryMetadata(place = place, story = story)
        LanguageAndSpeechControls(
            story = story,
            textToRead = selectedChapter.body,
            currentLang = currentLang,
            onLanguageSelected = onLanguageSelected
        )
        ChapterTimeline(
            chapters = chapters,
            selectedIndex = selectedChapterIndex,
            onChapterSelected = { selectedChapterIndex = it },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = selectedChapter.title,
            modifier = Modifier.padding(horizontal = 20.dp),
            fontFamily = LoraSerif,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = HeritageOchre
        )
        Text(
            text = selectedChapter.body,
            modifier = Modifier.padding(horizontal = 20.dp),
            fontFamily = LoraSerif,
            fontSize = 15.sp,
            lineHeight = 27.sp,
            color = Charcoal
        )
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = ParchmentVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = story.significance,
                modifier = Modifier.padding(12.dp),
                fontFamily = LoraSerif,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Charcoal
            )
        }
        QuizSection(
            place = place,
            questions = quizQuestions,
            mode = quizMode,
            selectedAnswers = selectedAnswers,
            canSaveBadge = quizCanSaveBadge,
            isGoogleSignedIn = isGoogleSignedIn,
            isSigningIn = isSigningIn,
            isBadgeSaving = isBadgeSaving,
            badgeSaved = badgeSaved,
            onStartQuizClick = {
                if (isGoogleSignedIn) {
                    quizCanSaveBadge = true
                    quizMode = QuizMode.Quiz
                } else {
                    showStartSignInDialog = true
                }
            },
            onAnswerSelected = { questionIndex, optionIndex ->
                if (selectedAnswers.getOrElse(questionIndex) { -1 } == -1) {
                    selectedAnswers = selectedAnswers.toMutableList().also {
                        it[questionIndex] = optionIndex
                    }
                }
            },
            onRetry = {
                selectedAnswers = List(quizQuestions.size) { -1 }
                quizMode = QuizMode.Quiz
            },
            onAwardVisible = {
                quizMode = QuizMode.Award
                if (quizCanSaveBadge && isGoogleSignedIn && !badgeSaved) {
                    onSaveBadge()
                }
            },
            onSaveBadgeClick = {
                if (isGoogleSignedIn) {
                    onSaveBadge()
                } else {
                    showSaveSignInDialog = true
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        ChatSection(
            messages = chatMessages,
            isLoading = isChatLoading,
            input = chatInput,
            onInputChange = { chatInput = it },
            onSubmit = {
                val question = chatInput.trim()
                if (question.isNotBlank()) {
                    onQuestionSubmitted(question)
                    chatInput = ""
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }

    if (showStartSignInDialog) {
        SignInDialog(
            title = "Sign in to earn your badge and save your journey",
            isSigningIn = isSigningIn,
            errorMessage = authErrorMessage,
            onSignIn = {
                showStartSignInDialog = false
                signInAction = PendingSignInAction.StartQuiz
                onStartGoogleSignIn()
            },
            onMaybeLater = {
                showStartSignInDialog = false
                quizCanSaveBadge = false
                quizMode = QuizMode.Quiz
            },
            onDismiss = { showStartSignInDialog = false }
        )
    }

    if (showSaveSignInDialog) {
        SignInDialog(
            title = "Sign in with Google to save your badge",
            isSigningIn = isSigningIn,
            errorMessage = authErrorMessage,
            onSignIn = {
                showSaveSignInDialog = false
                signInAction = PendingSignInAction.SaveBadge
                onStartGoogleSignIn()
            },
            onMaybeLater = { showSaveSignInDialog = false },
            onDismiss = { showSaveSignInDialog = false }
        )
    }
}

@Composable
private fun StoryMetadata(
    place: Place,
    story: Story
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = story.era.ifBlank { place.era },
            modifier = Modifier.weight(1f),
            fontFamily = LoraSerif,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HeritageOchre
        )
        AssistChip(
            onClick = {},
            label = { Text(place.type.name.replace('_', ' ')) }
        )
    }
}

@Composable
private fun LanguageAndSpeechControls(
    story: Story,
    textToRead: String,
    currentLang: String,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReading by rememberSaveable(story.placeId, story.lang) { mutableStateOf(false) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = story.lang.toSpeechLocale()
            }
        }
        textToSpeech = tts
        onDispose {
            tts.stop()
            tts.shutdown()
            textToSpeech = null
        }
    }

    LaunchedEffect(story.lang) {
        textToSpeech?.language = story.lang.toSpeechLocale()
        if (isReading) {
            textToSpeech?.stop()
            isReading = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentLang == "en",
            onClick = { onLanguageSelected("en") },
            label = { Text("EN") }
        )
        FilterChip(
            selected = currentLang == "kn",
            onClick = { onLanguageSelected("kn") },
            label = { Text("KN") }
        )
        IconButton(
            onClick = {
                textToSpeech?.language = story.lang.toSpeechLocale()
                textToSpeech?.speak(
                    textToRead,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "story-${story.placeId}-${story.lang}"
                )
                isReading = true
            }
        ) {
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = "Read story aloud",
                tint = HeritageOchre
            )
        }
        if (isReading) {
            TextButton(
                onClick = {
                    textToSpeech?.stop()
                    isReading = false
                }
            ) {
                Text("Pause")
            }
            IconButton(
                onClick = {
                    textToSpeech?.stop()
                    isReading = false
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop reading",
                    tint = HeritageOchre
                )
            }
        }
    }
}

@Composable
private fun QuizSection(
    place: Place,
    questions: List<QuizQuestion>,
    mode: QuizMode,
    selectedAnswers: List<Int>,
    canSaveBadge: Boolean,
    isGoogleSignedIn: Boolean,
    isSigningIn: Boolean,
    isBadgeSaving: Boolean,
    badgeSaved: Boolean,
    onStartQuizClick: () -> Unit,
    onAnswerSelected: (Int, Int) -> Unit,
    onRetry: () -> Unit,
    onAwardVisible: () -> Unit,
    onSaveBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) return

    val allAnswered = selectedAnswers.size == questions.size && selectedAnswers.all { it >= 0 }
    val allCorrect = allAnswered && questions.indices.all { index ->
        val selected = selectedAnswers[index]
        questions[index].options.getOrNull(selected) == questions[index].answer
    }

    LaunchedEffect(mode, allCorrect) {
        if (mode == QuizMode.Quiz && allCorrect) {
            onAwardVisible()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (mode) {
            QuizMode.Reading -> {
                Button(
                    onClick = onStartQuizClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Discovery Quiz & Earn Badge")
                }
            }
            QuizMode.Quiz -> {
                questions.forEachIndexed { questionIndex, question ->
                    QuizQuestionCard(
                        questionNumber = questionIndex + 1,
                        question = question,
                        selectedAnswer = selectedAnswers.getOrElse(questionIndex) { -1 },
                        onAnswerSelected = { optionIndex ->
                            onAnswerSelected(questionIndex, optionIndex)
                        }
                    )
                }
                if (allAnswered && !allCorrect) {
                    Text(
                        text = "Some answers need another try.",
                        fontFamily = LoraSerif,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) {
                        Text("Try Again")
                    }
                }
            }
            QuizMode.Award -> {
                Surface(
                    color = ParchmentVariant.copy(alpha = 0.84f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Badge Awarded",
                            fontFamily = LoraSerif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeritageOchre
                        )
                        Text(
                            text = "${place.name}, ${place.district}",
                            fontFamily = LoraSerif,
                            fontSize = 16.sp,
                            color = Charcoal
                        )
                        when {
                            badgeSaved -> Text(
                                text = "Saved to your journey.",
                                fontFamily = LoraSerif,
                                fontSize = 14.sp,
                                color = Charcoal
                            )
                            isBadgeSaving -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = HeritageOchre
                            )
                            !canSaveBadge || !isGoogleSignedIn -> Button(
                                onClick = onSaveBadgeClick,
                                enabled = !isSigningIn,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign in with Google to save your badge")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    questionNumber: Int,
    question: QuizQuestion,
    selectedAnswer: Int,
    onAnswerSelected: (Int) -> Unit
) {
    Surface(
        color = Parchment.copy(alpha = 0.78f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$questionNumber. ${question.question}",
                fontFamily = LoraSerif,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Charcoal
            )
            question.options.take(4).forEachIndexed { optionIndex, option ->
                val answered = selectedAnswer >= 0
                val isSelected = selectedAnswer == optionIndex
                val isCorrect = option == question.answer
                val optionColor = when {
                    answered && isCorrect -> Color(0xFF2E7D32)
                    answered && isSelected && !isCorrect -> Color(0xFFC62828)
                    else -> ParchmentVariant
                }
                Surface(
                    onClick = { if (!answered) onAnswerSelected(optionIndex) },
                    color = optionColor.copy(alpha = if (answered) 0.24f else 0.72f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        fontFamily = LoraSerif,
                        fontSize = 14.sp,
                        color = Charcoal
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInDialog(
    title: String,
    isSigningIn: Boolean,
    errorMessage: String?,
    onSignIn: () -> Unit,
    onMaybeLater: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSignIn,
                enabled = !isSigningIn
            ) {
                Text("Sign in with Google")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onMaybeLater,
                enabled = !isSigningIn
            ) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
private fun ChatSection(
    messages: List<StoryChatMessage>,
    isLoading: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Ask the guide",
            fontFamily = LoraSerif,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal
        )
        messages.forEach { message ->
            ChatBubble(message = message)
        }
        if (isLoading) {
            TypingIndicator()
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                placeholder = {
                    Text(
                        text = "Ask about this place",
                        fontFamily = LoraSerif
                    )
                },
                minLines = 1,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Parchment.copy(alpha = 0.82f),
                    unfocusedContainerColor = Parchment.copy(alpha = 0.82f),
                    focusedBorderColor = HeritageOchre,
                    unfocusedBorderColor = ParchmentVariant
                )
            )
            IconButton(
                onClick = onSubmit,
                enabled = !isLoading && input.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (input.isNotBlank() && !isLoading) HeritageOchre else ParchmentVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send question",
                    tint = if (input.isNotBlank() && !isLoading) Color.White else Color(0xFF777168)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: StoryChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            color = if (message.isUser) HeritageOchre else ParchmentVariant.copy(alpha = 0.84f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontFamily = LoraSerif,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = if (message.isUser) Color.White else Charcoal
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = ParchmentVariant.copy(alpha = 0.84f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = HeritageOchre
                )
                Text(
                    text = "Guide is typing...",
                    fontFamily = LoraSerif,
                    fontSize = 14.sp,
                    color = Charcoal
                )
            }
        }
    }
}

@Composable
private fun StoryImage(
    place: Place,
    story: Story
) {
    val imageUrl = story.imageUrl
    var imageLoadFailed by remember(imageUrl) { mutableStateOf(false) }

    if (imageUrl.isNullOrBlank() || imageLoadFailed) {
        StoryHeroFrame(place = place) {
            PlaceholderImage(place = place)
        }
    } else {
        StoryHeroFrame(place = place) {
            AsyncImage(
                model = imageUrl,
                contentDescription = place.name,
                modifier = Modifier.fillMaxSize(),
                placeholder = ColorPainter(Parchment),
                error = ColorPainter(Parchment),
                contentScale = ContentScale.Crop,
                onError = { imageLoadFailed = true }
            )
        }
    }
}

@Composable
private fun StoryHeroFrame(
    place: Place,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(StoryImageShape)
    ) {
        content()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.45f to Color.Transparent,
                            0.78f to Parchment.copy(alpha = 0.68f),
                            1f to Parchment
                        )
                    )
                )
        )
        Text(
            text = place.name,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            fontFamily = LoraSerif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 29.sp,
            color = Charcoal
        )
    }
}

@Composable
private fun PlaceholderImage() {
    PlaceholderImage(place = null)
}

@Composable
private fun PlaceholderImage(place: Place?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
            .parchmentTexture(),
        contentAlignment = Alignment.Center
    ) {
        if (place != null) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = place.name,
                    fontFamily = LoraSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = place.district,
                    fontFamily = LoraSerif,
                    fontSize = 13.sp,
                    color = Charcoal.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChapterTimeline(
    chapters: List<StoryChapter>,
    selectedIndex: Int,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 28.dp, end = 28.dp, top = 6.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFB8B0A4))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            chapters.forEachIndexed { index, label ->
                ChapterNode(
                    label = label.title,
                    active = index == selectedIndex,
                    onClick = { onChapterSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChapterNode(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeColor = if (active) HeritageOchre else Color(0xFF9A958A)
    val labelColor = if (active) Charcoal else Color(0xFF777168)

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(nodeColor)
        )
        Text(
            text = label,
            fontFamily = LoraSerif,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center
        )
    }
}

private val StoryImageShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp
)

@Composable
private fun StorySkeleton(place: Place) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StoryHeroFrame(place = place) {
            PlaceholderImage()
        }
        SkeletonBar(widthFraction = 0.72f, height = 28.dp, modifier = Modifier.padding(horizontal = 20.dp))
        SkeletonBar(widthFraction = 0.36f, height = 18.dp, modifier = Modifier.padding(horizontal = 20.dp))
        repeat(5) {
            SkeletonBar(
                widthFraction = if (it == 4) 0.64f else 1f,
                height = 16.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun SkeletonBar(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(ParchmentVariant)
    )
}

@Composable
private fun StoryError(
    place: Place,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StoryHeroFrame(place = place) {
            PlaceholderImage()
        }
        Text(
            text = place.name,
            modifier = Modifier.padding(horizontal = 20.dp),
            fontFamily = LoraSerif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal
        )
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 20.dp),
            fontFamily = LoraSerif,
            fontSize = 15.sp,
            lineHeight = 27.sp,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun Modifier.parchmentTexture(): Modifier = drawBehind {
    val warmSpeck = Color(0xFF8C4D3F).copy(alpha = 0.035f)
    val coolSpeck = Color(0xFF253452).copy(alpha = 0.025f)
    val step = 18.dp.toPx()

    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else step / 2f
        while (x < size.width) {
            drawCircle(
                color = if ((row + x.toInt()) % 3 == 0) warmSpeck else coolSpeck,
                radius = 0.9.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            x += step
        }
        row += 1
        y += step
    }
}

private fun String.toSpeechLocale(): Locale {
    return if (this == "kn") Locale("kn", "IN") else Locale("en", "IN")
}

private fun Story.toChapterSections(): List<StoryChapter> {
    val titles = if (lang == "kn") {
        listOf(
            "\u0CB8\u0CCD\u0CA5\u0CB3",
            "\u0CB5\u0CC0\u0CB0",
            "\u0CB8\u0C82\u0C98\u0CB0\u0CCD\u0CB7",
            "\u0CAA\u0CB0\u0C82\u0CAA\u0CB0\u0CC6",
            "\u0CA6\u0C82\u0CA4\u0C95\u0CA5\u0CC6"
        )
    } else {
        listOf("The Place", "The Hero", "The Conflict", "The Legacy", "The Legend")
    }
    val chunks = storyText.toChapterChunks(titles.size)

    return titles.mapIndexed { index, title ->
        val fallback = when (index) {
            titles.lastIndex -> significance.ifBlank { storyText }
            else -> storyText
        }
        StoryChapter(
            title = title,
            body = chunks.getOrNull(index)?.ifBlank { fallback } ?: fallback
        )
    }
}

private fun String.toChapterChunks(count: Int): List<String> {
    val normalized = trim()
    if (normalized.isBlank()) return List(count) { "" }

    val paragraphs = normalized
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (paragraphs.size >= count) {
        return List(count) { index ->
            paragraphs.drop(index).step(count).joinToString("\n\n")
        }
    }

    val sentences = Regex("(?<=[.!?।])\\s+")
        .split(normalized)
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (sentences.size >= count) {
        return sentences.chunkInto(count).map { it.joinToString(" ") }
    }

    val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size < count) return List(count) { index -> if (index == 0) normalized else "" }

    return words.chunkInto(count).map { it.joinToString(" ") }
}

private fun <T> List<T>.chunkInto(count: Int): List<List<T>> {
    if (isEmpty()) return List(count) { emptyList() }

    return List(count) { index ->
        val start = index * size / count
        val end = (index + 1) * size / count
        subList(start, end)
    }
}

private fun <T> List<T>.step(step: Int): List<T> {
    return filterIndexed { index, _ -> index % step == 0 }
}

private data class StoryChapter(
    val title: String,
    val body: String
)

private enum class QuizMode {
    Reading,
    Quiz,
    Award
}

private enum class PendingSignInAction {
    None,
    StartQuiz,
    SaveBadge
}

private const val MAX_QUIZ_QUESTIONS = 5

private val LoraSerif = FontFamily.Serif
