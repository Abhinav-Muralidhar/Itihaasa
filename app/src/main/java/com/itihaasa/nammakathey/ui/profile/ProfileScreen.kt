package com.itihaasa.nammakathey.ui.profile

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.itihaasa.nammakathey.data.repository.ProfileJourney
import com.itihaasa.nammakathey.model.Badge
import com.itihaasa.nammakathey.model.ExplorerRank
import com.itihaasa.nammakathey.model.ExploredPlace
import com.itihaasa.nammakathey.model.toExplorerRank
import com.itihaasa.nammakathey.ui.components.HeritageBadge
import com.itihaasa.nammakathey.ui.components.HeritageBadgeStyle
import com.itihaasa.nammakathey.ui.components.HeritageBadgeVisual
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.ParchmentVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAuthClick: () -> Unit = {},
    onSetupClick: () -> Unit = {},
    onStoryClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        )
    }
    val signOut: () -> Unit = {
        googleSignInClient.signOut().addOnCompleteListener {
            viewModel.signOut()
        }
    }
    val profileState = when {
        uiState.isLoading || uiState.isSigningOut -> ProfileContentState.Loading
        uiState.profile == null -> ProfileContentState.SignedOut
        uiState.profile?.profileComplete != true -> ProfileContentState.SetupRequired
        else -> ProfileContentState.Ready
    }

    Crossfade(
        targetState = profileState,
        label = "profile-content",
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (it) {
                ProfileContentState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = HeritageOchre
                )

                ProfileContentState.SignedOut -> SignedOutProfile(
                    isSigningIn = uiState.isSigningIn,
                    errorMessage = uiState.errorMessage,
                    onBackClick = onBackClick,
                    onSettingsClick = onSettingsClick,
                    onSignInClick = onAuthClick
                )

                ProfileContentState.SetupRequired -> ProfileSetupRequired(
                    profile = uiState.profile,
                    onBackClick = onBackClick,
                    onSettingsClick = onSettingsClick,
                    onSetupClick = onSetupClick,
                    onSignOutClick = signOut
                )

                ProfileContentState.Ready -> ProfileContent(
                    profile = uiState.profile,
                    rewardCards = uiState.rewardCards,
                    districtProgress = uiState.districtProgress,
                    completedDistrictCount = uiState.completedDistrictCount,
                    onBackClick = onBackClick,
                    onSettingsClick = onSettingsClick,
                    onSignOutClick = signOut,
                    onStoryClick = onStoryClick
                )
            }
        }
    }
}

private enum class ProfileContentState {
    Loading,
    SignedOut,
    SetupRequired,
    Ready
}

@Composable
private fun ProfileContent(
    profile: ProfileJourney?,
    rewardCards: List<RewardCardUiModel>,
    districtProgress: Map<String, DistrictProgressUiModel>,
    completedDistrictCount: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: (() -> Unit)?,
    onStoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (profile == null) return
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }
    var selectedDistrictProgress by remember { mutableStateOf<DistrictProgressUiModel?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileTopBar(onBackClick = onBackClick, onSettingsClick = onSettingsClick)
        ProfileHeader(profile, completedDistrictCount)
        JourneySummarySection(profile)
        RewardRewardsSection(cards = rewardCards)
        DistrictProgressSection(
            districtProgress = districtProgress,
            onDistrictClick = { selectedDistrictProgress = it }
        )
        BadgeCollectionPreviewSection(
            badges = profile.badgesEarned,
            onBadgeClick = { selectedBadge = it }
        )
        BadgeCollectionSection(
            badges = profile.badgesEarned,
            onBadgeClick = { selectedBadge = it }
        )
        if (onSignOutClick != null) {
            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    selectedBadge?.let { badge ->
        BadgeDetailSheet(
            badge = badge,
            onDismiss = { selectedBadge = null },
            onOpenStory = {
                selectedBadge = null
                onStoryClick(badge.placeId)
            }
        )
    }

    selectedDistrictProgress?.let { progress ->
        DistrictCrestDetailSheet(
            progress = progress,
            onDismiss = { selectedDistrictProgress = null }
        )
    }
}

@Composable
private fun RewardRewardsSection(cards: List<RewardCardUiModel>) {
    SectionCard(title = "Rewards") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cards.forEach { card ->
                RewardTemplateCard(
                    typeLabel = when (card.kind) {
                        RewardKind.QuizBadge -> "Quiz Badge"
                        RewardKind.DistrictBadge -> "District Badge"
                        RewardKind.RankPlaque -> "Rank Plaque"
                    },
                    title = card.title,
                    subtitle = card.subtitle,
                    accent = card.accent,
                    icon = card.icon,
                    statusText = card.statusText,
                    active = card.active
                )
            }
        }
    }
}

@Composable
private fun RewardTemplateCard(
    typeLabel: String,
    title: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    statusText: String,
    active: Boolean
) {
    Surface(
        modifier = Modifier.width(220.dp),
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = if (active) 0.3f else 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeritageBadge(
                    visual = HeritageBadgeVisual(
                        title = title,
                        subtitle = subtitle,
                        typeLabel = typeLabel,
                        districtCode = typeLabel.split(" ").joinToString("") { word ->
                            word.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
                        },
                        primary = accent,
                        secondary = HeritageOchre,
                        emblemText = when (typeLabel) {
                            "District Badge" -> "CREST"
                            "Rank Plaque" -> "RANK"
                            else -> "HERO"
                        },
                        style = when (typeLabel) {
                            "District Badge" -> HeritageBadgeStyle.District
                            "Rank Plaque" -> HeritageBadgeStyle.Rank
                            else -> HeritageBadgeStyle.Hero
                        }
                    ),
                    size = 48.dp,
                    labelVisible = false
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Charcoal.copy(alpha = 0.74f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = accent.copy(alpha = if (active) 0.12f else 0.08f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.24f))
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = RoyalIndigo
            )
        }
        Text(
            text = "Journey",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = RoyalIndigo,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = RoyalIndigo
            )
        }
    }
}

@Composable
private fun ProfileHeader(profile: ProfileJourney, completedDistrictCount: Int) {
    val badgeCount = profile.badgesEarned.rankBadgeCount()
    val storyCount = profile.completedStoryCount()

    Surface(
        color = RoyalIndigo,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileAvatar(profile = profile, size = 72)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = profile.displayName.ifBlank { "Namma Kathey Explorer" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile.homeDistrict.ifBlank { "Home district pending" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HeritageOchre
                    )
                    Text(
                        text = "Stories in ${profile.preferredLang.uppercase(Locale.getDefault())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ParchmentLight.copy(alpha = 0.82f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderMetric("Stories", storyCount.toString(), Icons.Filled.Map, Modifier.weight(1f))
                HeaderMetric("Badges", badgeCount.toString(), Icons.Filled.EmojiEvents, Modifier.weight(1f))
                HeaderMetric("Districts", completedDistrictCount.toString(), Icons.Filled.Map, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    profile: ProfileJourney,
    size: Int
) {
    if (profile.photoUrl.isBlank()) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(ParchmentLight)
                .border(2.dp, RoyalIndigo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.displayName.firstOrNull()?.uppercase().orEmpty().ifBlank { "N" },
                style = MaterialTheme.typography.headlineSmall,
                color = RoyalIndigo,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        AsyncImage(
            model = profile.photoUrl,
            contentDescription = profile.displayName,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .border(2.dp, RoyalIndigo, CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun HeaderMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ParchmentLight.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HeritageOchre,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ParchmentLight
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentLight.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun JourneySummarySection(profile: ProfileJourney) {
    val badgeCount = profile.badgesEarned.rankBadgeCount()
    val currentRank = badgeCount.toExplorerRank()
    val nextRank = ExplorerRank.entries
        .sortedBy { it.badgesRequired }
        .firstOrNull { it.badgesRequired > currentRank.badgesRequired }
    val progress = if (nextRank == null) {
        1f
    } else {
        val span = (nextRank.badgesRequired - currentRank.badgesRequired).coerceAtLeast(1)
        ((badgeCount - currentRank.badgesRequired).toFloat() / span).coerceIn(0f, 1f)
    }

    Surface(
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HeritageOchre),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RANK",
                        color = Parchment,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentRank.title,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalIndigo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentRank.description,
                        fontSize = 13.sp,
                        color = Charcoal.copy(alpha = 0.78f)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = HeritageOchre,
                trackColor = RoyalIndigo.copy(alpha = 0.16f)
            )
            InfoRow(
                label = "Next milestone",
                value = nextRank?.let { "$badgeCount / ${it.badgesRequired} badges " }
                    ?: "$badgeCount badges earned"
            )
            InfoRow(
                label = "Home district",
                value = profile.homeDistrict.ifBlank { "Not set" }
            )
            if (profile.joinedAt > 0L) {
                InfoRow(label = "Started", value = profile.joinedAt.formatDate())
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExplorerRank.entries.forEach { rank ->
                    val selected = rank == currentRank
                    Surface(
                        color = if (selected) HeritageOchre else Parchment,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = if (selected) 1f else 0.24f))
                    ) {
                        Text(
                            text = rank.title,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Parchment else RoyalIndigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(profile: ProfileJourney) {
    val exploredDistricts = profile.badgesEarned
        .map { it.district.normalizedDistrict() }
        .filter { it.isNotBlank() }
        .distinct()
        .size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Stories", profile.completedStoryCount().toString(), Modifier.weight(1f))
        StatCard("Badges", profile.badgesEarned.distinctBy { it.placeId }.size.toString(), Modifier.weight(1f))
        StatCard("Streak", profile.quizStreak.toString(), Modifier.weight(1f))
        StatCard("Districts", exploredDistricts.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HeritageOchre
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Charcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BadgeCollectionPreviewSection(
    badges: List<Badge>,
    onBadgeClick: (Badge) -> Unit
) {
    val earned = badges.distinctBy { it.placeId }.sortedByDescending { it.earnedAt }.take(3)

    SectionCard(title = "Recent Badges") {
        if (earned.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RoyalIndigo.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = HeritageOchre,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Complete a story quiz to add your first badge to the collection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal.copy(alpha = 0.74f)
                )
            }
        } else {
            earned.forEach { badge ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBadgeClick(badge) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeritageBadge(
                        visual = badge.toBadgeVisual(),
                        size = 48.dp,
                        labelVisible = false
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.placeName.ifBlank { "Heritage story" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Charcoal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOf(badge.district, badge.earnedAt.formatDate())
                                .filter { it.isNotBlank() }
                                .joinToString(" - "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Charcoal.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { onBadgeClick(badge) }) {
                        Text("View")
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictProgressSection(
    districtProgress: Map<String, DistrictProgressUiModel>,
    onDistrictClick: (DistrictProgressUiModel) -> Unit
) {
    val districtItems = districtProgress.values
        .sortedBy { it.district }
    val completedDistricts = districtItems.count { it.isComplete }

    SectionCard(title = "District Crests") {
        Text(
            text = "$completedDistricts / ${districtItems.size} district crests earned",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Charcoal
        )
        Text(
            text = "Each crest unlocks after every story in that district is complete. Locked crests stay muted until then.",
            style = MaterialTheme.typography.bodySmall,
            color = Charcoal.copy(alpha = 0.72f)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            districtItems.forEach { progress ->
                DistrictCrestProgressItem(
                    progress = progress,
                    onClick = { onDistrictClick(progress) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(color = Color(0xFF205D68), text = "Earned")
            LegendItem(color = HeritageOchre, text = "In progress")
            LegendItem(color = ParchmentVariant, text = "Locked")
        }
    }
}

@Composable
private fun DistrictCrestProgressItem(
    progress: DistrictProgressUiModel,
    onClick: () -> Unit
) {
    val earned = progress.isComplete

    Column(
        modifier = Modifier.width(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HeritageBadge(
            visual = progress.toCrestVisual(),
            size = 82.dp,
            labelVisible = false,
            onClick = onClick
        )
        Text(
            text = progress.district,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (earned) FontWeight.Bold else FontWeight.SemiBold,
            color = if (earned) Charcoal else Charcoal.copy(alpha = 0.66f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (earned) "Complete" else "${progress.completed}/${progress.total}",
            style = MaterialTheme.typography.labelSmall,
            color = if (earned) HeritageOchre else Charcoal.copy(alpha = 0.58f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Charcoal.copy(alpha = 0.74f)
        )
    }
}

@Composable
private fun BadgeCollectionSection(
    badges: List<Badge>,
    onBadgeClick: (Badge) -> Unit
) {
    val heroBadges = badges
        .filterNot { it.badgeType == "district" }
        .sortedByDescending { it.earnedAt }
        .distinctBy { it.placeId }

    SectionCard(title = "Hero Badges") {
        if (heroBadges.isEmpty()) {
            EmptyText("Hero badges will appear here after each completed story quiz.")
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                heroBadges.chunked(2).forEach { badgeColumn ->
                    Column(
                        modifier = Modifier.width(104.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        badgeColumn.forEach { badge ->
                            BadgeGridItem(
                                badge = badge,
                                onBadgeClick = onBadgeClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeGridItem(
    badge: Badge,
    onBadgeClick: (Badge) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HeritageBadge(
            visual = badge.toBadgeVisual(),
            size = 84.dp,
            labelVisible = false,
            onClick = { onBadgeClick(badge) }
        )
        Text(
            text = badge.placeName.ifBlank { "Heritage" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Charcoal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = badge.district,
            style = MaterialTheme.typography.labelSmall,
            color = Charcoal.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = badge.earnedAt.formatDate(),
            style = MaterialTheme.typography.labelSmall,
            color = HeritageOchre,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun Badge.badgeAccent(): Color {
    if (badgeType == "district") return Color(0xFF205D68)
    if (badgeType == "rank") return HeritageOchre
    val source = "${placeName.lowercase(Locale.getDefault())} ${district.lowercase(Locale.getDefault())}"
    return when {
        listOf("temple", "mandir", "devasthana", "gudi").any(source::contains) -> Color(0xFF8C5A2B)
        listOf("fort", "kote", "durga").any(source::contains) -> Color(0xFF375D42)
        listOf("palace", "mahal").any(source::contains) -> RoyalIndigo
        listOf("cave", "gavi").any(source::contains) -> Color(0xFF596071)
        listOf("river", "falls", "kere", "lake").any(source::contains) -> Color(0xFF1F6D79)
        else -> Color(0xFFB05A35)
    }
}

private fun ProfileJourney.completedStoryCount(): Int =
    completedHeroIds.size.takeIf { it > 0 } ?: badgesEarned.rankBadgeCount()

private fun List<Badge>.rankBadgeCount(): Int =
    filterNot { it.badgeType == "district" }
        .distinctBy { it.placeId }
        .size

private fun Badge.toBadgeVisual(): HeritageBadgeVisual =
    HeritageBadgeVisual(
        title = placeName.ifBlank {
            if (badgeType == "district") "District Crest" else "Heritage Story"
        },
        subtitle = district,
        typeLabel = when (badgeType) {
            "district" -> "District"
            "rank" -> "Rank"
            else -> "Hero"
        },
        districtCode = district.ifBlank {
            when (badgeType) {
                "district" -> "District"
                "rank" -> "Rank"
                else -> "Hero"
            }
        },
        primary = badgeAccent(),
        secondary = if (badgeType == "rank") RoyalIndigo else HeritageOchre,
        emblemText = when (badgeType) {
            "district" -> "CREST"
            "rank" -> "RANK"
            else -> "HERO"
        },
        style = when (badgeType) {
            "district" -> HeritageBadgeStyle.District
            "rank" -> HeritageBadgeStyle.Rank
            else -> HeritageBadgeStyle.Hero
        }
    )

private fun DistrictProgressUiModel.toCrestVisual(): HeritageBadgeVisual {
    val earned = isComplete
    val active = completed > 0
    return HeritageBadgeVisual(
        title = "$district Crest",
        subtitle = "$completed/$total stories",
        typeLabel = "District",
        districtCode = district,
        primary = when {
            earned -> Color(0xFF205D68)
            active -> Color(0xFF7A6A4A)
            else -> Color(0xFFB9B3A8)
        },
        secondary = when {
            earned -> HeritageOchre
            active -> HeritageOchre.copy(alpha = 0.82f)
            else -> Color(0xFF8E877B)
        },
        emblemText = if (earned) "CREST" else "$completed/$total",
        style = HeritageBadgeStyle.District
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeDetailSheet(
    badge: Badge,
    onDismiss: () -> Unit,
    onOpenStory: () -> Unit
) {
    val isHeroBadge = badge.badgeType != "district" && badge.badgeType != "rank"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ParchmentLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeritageBadge(
                visual = badge.toBadgeVisual(),
                size = 132.dp,
                labelVisible = false
            )
            Text(
                text = badge.placeName.ifBlank { "Heritage Badge" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo,
                textAlign = TextAlign.Center
            )
            Text(
                text = when (badge.badgeType) {
                    "district" -> "District crest"
                    "rank" -> "Rank badge"
                    else -> "Hero story badge"
                },
                style = MaterialTheme.typography.labelLarge,
                color = HeritageOchre,
                fontWeight = FontWeight.SemiBold
            )
            InfoRow(label = "District", value = badge.district.ifBlank { "Not set" })
            InfoRow(label = "Earned", value = badge.earnedAt.formatDate().ifBlank { "Recently" })
            Text(
                text = when (badge.badgeType) {
                    "district" -> "Unlocked by completing every story in this district."
                    "rank" -> "Unlocked by reaching a story milestone."
                    else -> "Unlocked by completing this hero story quiz."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal.copy(alpha = 0.76f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (isHeroBadge) {
                Button(
                    onClick = onOpenStory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Read Story Again")
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DistrictCrestDetailSheet(
    progress: DistrictProgressUiModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ParchmentLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeritageBadge(
                visual = progress.toCrestVisual(),
                size = 132.dp,
                labelVisible = false
            )
            Text(
                text = "${progress.district} Crest",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (progress.isComplete) {
                    "District crest earned"
                } else {
                    "District crest locked"
                },
                style = MaterialTheme.typography.labelLarge,
                color = HeritageOchre,
                fontWeight = FontWeight.SemiBold
            )
            InfoRow(label = "Stories", value = "${progress.completed} / ${progress.total}")
            InfoRow(
                label = "Status",
                value = if (progress.isComplete) "Complete" else "In progress"
            )
            Text(
                text = if (progress.isComplete) {
                    "Unlocked by completing every available hero story in this district."
                } else {
                    "Complete the remaining stories in this district to unlock this crest."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal.copy(alpha = 0.76f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}


@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
            content()
        }
    }
}

@Composable
private fun ProfileSetupRequired(
    profile: ProfileJourney?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSetupClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileTopBar(onBackClick = onBackClick, onSettingsClick = onSettingsClick)
        Surface(
            color = RoyalIndigo,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, RoyalIndigo)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(ParchmentLight)
                        .border(2.dp, RoyalIndigo, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.displayName?.firstOrNull()?.uppercase().orEmpty().ifBlank { "N" },
                        color = RoyalIndigo,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                }
                Text(
                    text = "Complete your profile",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Choose your home district to unlock your first story path and preserve progress under your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentLight.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSetupClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Set up profile")
                }
            }
        }
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Charcoal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Charcoal
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Charcoal.copy(alpha = 0.72f)
    )
}

@Composable
private fun SignedOutProfile(
    isSigningIn: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ProfileTopBar(onBackClick = onBackClick, onSettingsClick = onSettingsClick)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RoyalIndigo,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, RoyalIndigo)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(ParchmentLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = RoyalIndigo,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = "Sign in to save your journey",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your home district, unlocked stories, earned badges, and quiz streak stay connected to your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentLight.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onSignInClick,
                    enabled = !isSigningIn,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSigningIn) "Opening..." else "Sign in or create account")
                }
            }
        }
        SectionCard(title = "Journey Preview") {
            InfoRow(label = "Start", value = "Choose your home district")
            InfoRow(label = "Unlock", value = "Complete story challenges")
            InfoRow(label = "Progress", value = "Earn badges by district")
        }
    }
}

private fun Long.formatDate(): String {
    if (this <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
}

private fun String.normalizedDistrict(): String {
    return trim().lowercase(Locale.getDefault())
}

private fun previewProfileJourney(): ProfileJourney {
    val now = System.currentTimeMillis()
    return ProfileJourney(
        displayName = "Namma Kathey Explorer",
        preferredLang = "EN",
        quizStreak = 4,
        joinedAt = now,
        badgesEarned = listOf(
            Badge(placeId = "mysuru-palace", placeName = "Mysuru Palace", district = "Mysuru", earnedAt = now),
            Badge(placeId = "bidar-fort", placeName = "Bidar Fort", district = "Bidar", earnedAt = now - 86_400_000L),
            Badge(placeId = "badami-caves", placeName = "Badami Caves", district = "Bagalkot", earnedAt = now - 172_800_000L),
            Badge(placeId = "jog-falls", placeName = "Jog Falls", district = "Shivamogga", earnedAt = now - 259_200_000L)
        ),
        placesExplored = listOf(
            ExploredPlace(placeId = "mysuru-palace", name = "Mysuru Palace", timestamp = now, badgeEarned = true),
            ExploredPlace(placeId = "bidar-fort", name = "Bidar Fort", timestamp = now - 86_400_000L, badgeEarned = true)
        )
    )
}
