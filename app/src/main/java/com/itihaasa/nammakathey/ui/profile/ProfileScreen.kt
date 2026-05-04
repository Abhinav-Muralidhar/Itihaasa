package com.itihaasa.nammakathey.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.google.android.gms.common.api.ApiException
import com.itihaasa.nammakathey.R
import com.itihaasa.nammakathey.data.repository.ProfileJourney
import com.itihaasa.nammakathey.model.Badge
import com.itihaasa.nammakathey.model.ExploredPlace
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
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
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick,
                onSignInClick = startGoogleSignIn
            )

            else -> ProfileContent(
                profile = uiState.profile,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick,
                onSignOutClick = {
                    googleSignInClient.signOut()
                    viewModel.signOut()
                }
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: ProfileJourney?,
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
        ProfileHeader(profile)
        StatsRow(profile)
        DistrictProgressSection(profile.badgesEarned)
        BadgesSection(profile.badgesEarned)
        ExploredPlacesSection(profile.placesExplored)
        if (onSignOutClick != null) {
            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
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
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = RoyalIndigo
        )
        Row {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RoyalIndigo
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = RoyalIndigo
                )
            }
        }
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
                        .background(RoyalIndigo),
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
        shape = RoundedCornerShape(8.dp)
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
private fun DistrictProgressSection(badges: List<Badge>) {
    val exploredDistricts = badges
        .map { it.district.normalizedDistrict() }
        .filter { it.isNotBlank() }
        .toSet()

    SectionCard(title = "District Progress") {
        Text(
            text = "${exploredDistricts.size} / ${KARNATAKA_DISTRICTS.size} districts explored",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Charcoal
        )
        KARNATAKA_DISTRICTS.chunked(5).forEach { rowDistricts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowDistricts.forEach { district ->
                    val explored = district.normalizedDistrict() in exploredDistricts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (explored) RoyalIndigo else Parchment)
                            .border(
                                width = 1.dp,
                                color = if (explored) RoyalIndigo else HeritageOchre,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = district.districtInitials(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (explored) Parchment else HeritageOchre,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgesSection(badges: List<Badge>) {
    SectionCard(title = "Badges") {
        if (badges.isEmpty()) {
            EmptyText("Pass a discovery quiz to earn your first badge.")
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D5A3D)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.badgeSymbol(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Parchment,
                textAlign = TextAlign.Center
            )
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
                color = RoyalIndigo
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
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ProfileContent(
            profile = previewProfileJourney(),
            onBackClick = onBackClick,
            onSettingsClick = onSettingsClick,
            onSignOutClick = null,
            modifier = Modifier
                .blur(5.dp)
                .alpha(0.34f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray.copy(alpha = 0.18f))
        )
        Surface(
            modifier = Modifier.padding(24.dp),
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
                    color = Charcoal,
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
                    enabled = !isSigningIn
                ) {
                    Text(if (isSigningIn) "Signing in..." else "Sign in with Google")
                }
            }
        }
    }
}

private fun Long.formatDate(): String {
    if (this <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
}

private fun Badge.badgeSymbol(): String {
    val source = "${placeName.lowercase(Locale.getDefault())} ${district.lowercase(Locale.getDefault())}"
    return when {
        listOf("temple", "mandir", "devasthana", "gudi").any(source::contains) -> "T"
        listOf("fort", "kote", "durga").any(source::contains) -> "F"
        listOf("palace", "mahal").any(source::contains) -> "P"
        listOf("cave", "gavi").any(source::contains) -> "C"
        listOf("river", "falls", "kere", "lake").any(source::contains) -> "W"
        else -> placeName.firstOrNull()?.uppercaseChar()?.toString() ?: "H"
    }
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
