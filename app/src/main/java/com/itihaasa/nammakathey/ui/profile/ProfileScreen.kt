package com.itihaasa.nammakathey.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.itihaasa.nammakathey.R
import com.itihaasa.nammakathey.data.repository.ProfileJourney
import com.itihaasa.nammakathey.model.Badge
import com.itihaasa.nammakathey.model.ExploredPlace
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.ForestGreen
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.ParchmentVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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
        }.getOrNull()?.let(viewModel::signInWithGoogle)
    }
    val startGoogleSignIn = {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = HeritageOchre
            )

            uiState.profile == null -> SignedOutProfile(
                isSigningIn = uiState.isSigningIn,
                errorMessage = uiState.errorMessage,
                onSignInClick = startGoogleSignIn
            )
            else -> ProfileContent(profile = uiState.profile)
        }
    }
}

@Composable
private fun ProfileContent(profile: ProfileJourney?) {
    if (profile == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileHeader(profile)
        StatsRow(profile)
        UsefulJourneyInfo(profile)
        BadgesSection(profile.badgesEarned)
        ExploredPlacesSection(profile.placesExplored)
    }
}

@Composable
private fun ProfileHeader(profile: ProfileJourney) {
    Surface(
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (profile.photoUrl.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ForestGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.displayName.firstOrNull()?.uppercase().orEmpty().ifBlank { "N" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Parchment
                    )
                }
            } else {
                AsyncImage(
                    model = profile.photoUrl,
                    contentDescription = profile.displayName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = profile.displayName.ifBlank { "Namma Kathey Explorer" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal
                )
                Text(
                    text = "Preferred language: ${profile.preferredLang.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
                )
                if (profile.joinedAt > 0L) {
                    Text(
                        text = "Joined ${profile.joinedAt.formatDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(profile: ProfileJourney) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Places",
            value = profile.placesExplored.distinctBy { it.placeId }.size.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Badges",
            value = profile.badgesEarned.distinctBy { it.placeId }.size.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Streak",
            value = profile.quizStreak.toString(),
            modifier = Modifier.weight(1f)
        )
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                color = Charcoal
            )
        }
    }
}

@Composable
private fun UsefulJourneyInfo(profile: ProfileJourney) {
    SectionCard(title = "Journey") {
        InfoRow("Next goal", nextGoal(profile.badgesEarned.size))
        InfoRow("Latest badge", profile.badgesEarned.maxByOrNull { it.earnedAt }?.placeName ?: "None yet")
        InfoRow(
            "Last explored",
            profile.placesExplored.maxByOrNull { it.timestamp }?.name ?: "No places explored yet"
        )
    }
}

@Composable
private fun BadgesSection(badges: List<Badge>) {
    SectionCard(title = "Badges") {
        if (badges.isEmpty()) {
            EmptyText("Pass a discovery quiz to earn your first badge.")
        } else {
            badges.sortedByDescending { it.earnedAt }.forEach { badge ->
                Surface(
                        color = ParchmentVariant.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Badge",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = HeritageOchre
                        )
                        Column {
                            Text(
                                text = badge.placeName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal
                            )
                            Text(
                                text = listOf(badge.district, badge.earnedAt.formatDate())
                                    .filter { it.isNotBlank() }
                                    .joinToString(" - "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploredPlacesSection(places: List<ExploredPlace>) {
    SectionCard(title = "Explored Places") {
        if (places.isEmpty()) {
            EmptyText("Open a place story and complete a quiz to build your journey.")
        } else {
            places.sortedByDescending { it.timestamp }.forEach { place ->
                InfoRow(
                    label = place.name.ifBlank { place.placeId },
                    value = if (place.badgeEarned) "Badge earned" else place.timestamp.formatDate()
                )
            }
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
        shape = RoundedCornerShape(8.dp)
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
                color = ForestGreen
            )
            content()
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
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ParchmentLight,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign in to save your journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal
                )
                Text(
                    text = "Your badges, explored places, quiz streak, and profile will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
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
                    enabled = !isSigningIn
                ) {
                    Text(if (isSigningIn) "Signing in..." else "Sign in with Google")
                }
            }
        }
    }
}

private fun nextGoal(badgeCount: Int): String {
    return when {
        badgeCount < 1 -> "Earn your first badge"
        badgeCount < 3 -> "Collect 3 badges"
        badgeCount < 5 -> "Collect 5 badges"
        else -> "Keep exploring Karnataka"
    }
}

private fun Long.formatDate(): String {
    if (this <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
}
