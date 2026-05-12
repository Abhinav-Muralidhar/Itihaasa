package com.itihaasa.nammakathey.ui.profile

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.OchreContainer
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
                    onSignOutClick = signOut
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
    modifier: Modifier = Modifier
) {
    if (profile == null) return

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
        DistrictProgressSection(profile, districtProgress)
        BadgeCollectionPreviewSection(profile.badgesEarned)
        BadgeCollectionSection(profile.badgesEarned)
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
}

@Composable
private fun RewardRewardsSection(cards: List<RewardCardUiModel>) {
    SectionCard(title = "Rewards") {
        Text(
            text = "These cards are driven by live progress. They reflect unlocked quiz badges, district crests, and rank movement.",
            style = MaterialTheme.typography.bodySmall,
            color = Charcoal.copy(alpha = 0.72f)
        )
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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ParchmentLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
    val badgeCount = profile.badgesEarned.distinctBy { it.placeId }.size
    val exploredPlaces = profile.placesExplored.distinctBy { it.placeId }.size

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
                HeaderMetric("Places", exploredPlaces.toString(), Icons.Filled.LocationOn, Modifier.weight(1f))
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
    val badgeCount = profile.badgesEarned.distinctBy { it.placeId }.size
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
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Parchment,
                        modifier = Modifier.size(27.dp)
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
                value = nextRank?.let { "$badgeCount / ${it.badgesRequired} badges to ${it.title}" }
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
        StatCard("Places", profile.placesExplored.distinctBy { it.placeId }.size.toString(), Modifier.weight(1f))
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
private fun BadgeCollectionPreviewSection(badges: List<Badge>) {
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(badge.badgeAccent()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badge.badgeIcon(),
                            contentDescription = null,
                            tint = ParchmentLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = HeritageOchre,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DistrictProgressSection(
    profile: ProfileJourney,
    districtProgress: Map<String, DistrictProgressUiModel>
) {
    val homeDistrict = profile.homeDistrict.normalizedDistrict()
    val completedDistricts = districtProgress.values.count { it.isComplete }

    SectionCard(title = "District Journey") {
        Text(
            text = "$completedDistricts / ${KARNATAKA_DISTRICTS.size} districts complete",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Charcoal
        )
        Text(
            text = profile.homeDistrict
                .takeIf { it.isNotBlank() }
                ?.let { "$it is your home district. It completes as you finish every story there." }
                ?: "Choose a home district to start your story path.",
            style = MaterialTheme.typography.bodySmall,
            color = Charcoal.copy(alpha = 0.72f)
        )
        KARNATAKA_DISTRICTS.chunked(5).forEach { rowDistricts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowDistricts.forEach { district ->
                    val progress = districtProgress[district.normalizedDistrict()]
                    val explored = progress?.isComplete == true
                    val isHome = district.normalizedDistrict() == homeDistrict
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    explored -> RoyalIndigo
                                    isHome -> OchreContainer
                                    else -> Parchment
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    explored -> RoyalIndigo
                                    isHome -> HeritageOchre
                                    else -> HeritageOchre.copy(alpha = 0.42f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                explored -> "Done"
                                (progress?.completed ?: 0) > 0 -> "${progress?.completed}/${progress?.total}"
                                else -> district.districtInitials()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (explored) Parchment else if (isHome) RoyalIndigo else HeritageOchre,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(color = HeritageOchre, text = "Home")
            LegendItem(color = RoyalIndigo, text = "Complete")
            LegendItem(color = ParchmentVariant, text = "Locked")
        }
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
private fun BadgeCollectionSection(badges: List<Badge>) {
    SectionCard(title = "Badge Collection") {
        if (badges.isEmpty()) {
            EmptyText("Your badge collection will grow here after each completed story quiz.")
        } else {
            badges.sortedByDescending { it.earnedAt }
                .distinctBy { it.placeId }
                .chunked(3)
                .forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            BadgeGridItem(badge = badge, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - rowBadges.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
        }
    }
}

@Composable
private fun BadgeGridItem(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(80.dp)
                .border(1.dp, badge.badgeAccent().copy(alpha = 0.36f), CircleShape),
            shape = CircleShape,
            color = badge.badgeAccent()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(ParchmentLight.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badge.badgeIcon(),
                        contentDescription = null,
                        tint = ParchmentLight,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
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

private fun Badge.badgeIcon(): ImageVector {
    if (badgeType == "district") return Icons.Filled.EmojiEvents
    val source = "${placeName.lowercase(Locale.getDefault())} ${district.lowercase(Locale.getDefault())}"
    return when {
        listOf("temple", "mandir", "devasthana", "gudi").any(source::contains) -> Icons.Filled.CheckCircle
        listOf("fort", "kote", "durga").any(source::contains) -> Icons.Filled.LocationOn
        listOf("palace", "mahal").any(source::contains) -> Icons.Filled.EmojiEvents
        listOf("cave", "gavi").any(source::contains) -> Icons.Filled.Map
        listOf("river", "falls", "kere", "lake").any(source::contains) -> Icons.Filled.Star
        else -> Icons.Filled.EmojiEvents
    }
}

private fun Badge.badgeAccent(): Color {
    if (badgeType == "district") return Color(0xFF7B4F9D)
    val source = "${placeName.lowercase(Locale.getDefault())} ${district.lowercase(Locale.getDefault())}"
    return when {
        listOf("temple", "mandir", "devasthana", "gudi").any(source::contains) -> Color(0xFF8C5A2B)
        listOf("fort", "kote", "durga").any(source::contains) -> Color(0xFF2D5A3D)
        listOf("palace", "mahal").any(source::contains) -> Color(0xFF2E2A5F)
        listOf("cave", "gavi").any(source::contains) -> Color(0xFF4D5C7A)
        listOf("river", "falls", "kere", "lake").any(source::contains) -> Color(0xFF206A83)
        else -> Color(0xFF6B4A2E)
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

private fun String.districtInitials(): String {
    return split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
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

private val KARNATAKA_DISTRICTS = listOf(
    "Bagalkot",
    "Ballari",
    "Belagavi",
    "Bengaluru Rural",
    "Bengaluru Urban",
    "Bidar",
    "Chamarajanagar",
    "Chikkaballapur",
    "Chikkamagaluru",
    "Chitradurga",
    "Dakshina Kannada",
    "Davanagere",
    "Dharwad",
    "Gadag",
    "Hassan",
    "Haveri",
    "Kalaburagi",
    "Kodagu",
    "Kolar",
    "Koppal",
    "Mandya",
    "Mysuru",
    "Raichur",
    "Ramanagara",
    "Shivamogga",
    "Tumakuru",
    "Udupi",
    "Uttara Kannada",
    "Vijayapura",
    "Yadgir"
)
