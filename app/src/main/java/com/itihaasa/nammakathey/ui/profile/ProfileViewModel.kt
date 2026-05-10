package com.itihaasa.nammakathey.ui.profile

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import com.itihaasa.nammakathey.data.local.StoryCatalogDataSource
import com.itihaasa.nammakathey.data.repository.ProfileRepository
import com.itihaasa.nammakathey.data.repository.ProfileJourney
import com.itihaasa.nammakathey.model.Badge
import com.itihaasa.nammakathey.model.ExplorerRank
import com.itihaasa.nammakathey.model.ExploredPlace
import com.itihaasa.nammakathey.model.toExplorerRank
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val storyCatalogDataSource: StoryCatalogDataSource,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var profileJob: Job? = null
    private var remoteProfile: ProfileJourney? = null
    private val progressListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_COMPLETED_HERO_IDS || key?.startsWith(KEY_COMPLETED_HERO_EARNED_AT_PREFIX) == true) {
            publishProfile(remoteProfile)
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(progressListener)
        observeProfile()
    }

    private fun observeProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            profileRepository.observeProfile()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load profile."
                        )
                    }
                }
                .collect { profile ->
                    remoteProfile = profile
                    publishProfile(profile)
                }
        }
    }

    private fun publishProfile(profile: ProfileJourney?) {
        val mergedProfile = profile?.withLocalProgressOverlay()
        val districtProgress = mergedProfile?.let(::buildDistrictProgress).orEmpty()
        _uiState.update {
            it.copy(
                isLoading = false,
                profile = mergedProfile,
                rewardCards = mergedProfile?.let(::buildRewardCards).orEmpty(),
                districtProgress = districtProgress,
                completedDistrictCount = districtProgress.values.count { progress -> progress.isComplete },
                errorMessage = null
            )
        }
    }

    private fun ProfileJourney.withLocalProgressOverlay(): ProfileJourney {
        val localBadges = localCompletedBadges()
        if (localBadges.isEmpty()) return copy(explorerRank = badgesEarned.distinctBy { it.placeId }.size.toExplorerRank())

        val existingBadgeIds = badgesEarned.map { it.placeId }.toSet()
        val newLocalBadgeCount = localBadges.count { it.placeId !in existingBadgeIds }
        val mergedBadges = (badgesEarned + localBadges)
            .distinctBy { it.placeId }
        val mergedCompletedHeroIds = completedHeroIds + localBadges.map { it.placeId }
        val localPlaces = localBadges.map { badge ->
            ExploredPlace(
                placeId = badge.placeId,
                name = badge.placeName,
                timestamp = badge.earnedAt,
                badgeEarned = true
            )
        }
        return copy(
            badgesEarned = mergedBadges,
            placesExplored = (placesExplored + localPlaces).distinctBy { it.placeId },
            completedHeroIds = mergedCompletedHeroIds,
            quizStreak = quizStreak + newLocalBadgeCount,
            explorerRank = mergedBadges.distinctBy { it.placeId }.size.toExplorerRank()
        )
    }

    private fun localCompletedBadges(): List<Badge> {
        val completedHeroIds = sharedPreferences
            .getStringSet(KEY_COMPLETED_HERO_IDS, emptySet())
            .orEmpty()
        if (completedHeroIds.isEmpty()) return emptyList()

        return completedHeroIds.mapNotNull { placeId ->
            val hero = storyCatalogDataSource.getHeroByPlaceId(placeId) ?: return@mapNotNull null
            Badge(
                placeId = hero.placeId,
                placeName = hero.title,
                district = hero.district,
                earnedAt = sharedPreferences.getLong(
                    KEY_COMPLETED_HERO_EARNED_AT_PREFIX + hero.placeId,
                    System.currentTimeMillis()
                ),
                badgeType = "hero"
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.update {
            it.copy(isSigningIn = true, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                profileRepository.signInWithGoogle(idToken)
            }.onSuccess {
                _uiState.update {
                    it.copy(isSigningIn = false, errorMessage = null)
                }
                observeProfile()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        errorMessage = throwable.message ?: "Google sign-in failed."
                    )
                }
            }
        }
    }

    fun signOut() {
        _uiState.update { it.copy(isSigningOut = true, errorMessage = null) }
        profileRepository.signOut()
        _uiState.update {
            it.copy(
                profile = null,
                rewardCards = emptyList(),
                isSigningIn = false,
                isSigningOut = false,
                errorMessage = null
            )
        }
        observeProfile()
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(progressListener)
        super.onCleared()
    }

    private fun buildRewardCards(profile: ProfileJourney): List<RewardCardUiModel> {
        return listOf(
            quizReward(profile),
            districtReward(profile),
            rankReward(profile)
        )
    }

    private fun buildDistrictProgress(profile: ProfileJourney): Map<String, DistrictProgressUiModel> {
        val completedHeroIds = profile.completedHeroIds.ifEmpty {
            profile.badgesEarned.map { it.placeId }.toSet()
        }

        return storyCatalogDataSource.stories
            .groupBy { it.district.trim() }
            .mapKeys { (district, _) -> district.trim().lowercase(Locale.getDefault()) }
            .mapValues { (district, stories) ->
                val completed = stories.count { it.placeId in completedHeroIds }
                DistrictProgressUiModel(
                    district = district,
                    completed = completed,
                    total = stories.size
                )
            }
    }

    private fun quizReward(profile: ProfileJourney): RewardCardUiModel {
        val latestBadge = profile.badgesEarned.maxByOrNull { it.earnedAt }
        val badgeCount = profile.badgesEarned.distinctBy { it.placeId }.size
        return RewardCardUiModel(
            kind = RewardKind.QuizBadge,
            title = latestBadge?.placeName?.takeIf { it.isNotBlank() } ?: "Quiz Badge",
            subtitle = latestBadge?.district?.takeIf { it.isNotBlank() }
                ?.let { "Latest badge from $it. $badgeCount total earned." }
                ?: "Earn your first badge by finishing a story quiz.",
            accent = Color(0xFFC47D28),
            icon = Icons.Filled.CheckCircle,
            statusText = latestBadge?.earnedAt?.let { "Earned ${it.formatDate()}" } ?: "Locked",
            active = latestBadge != null
        )
    }

    private fun districtReward(profile: ProfileJourney): RewardCardUiModel {
        val completedHeroIds = profile.completedHeroIds.ifEmpty {
            profile.badgesEarned.map { it.placeId }.toSet()
        }
        val districtProgress = storyCatalogDataSource.stories
            .groupBy { it.district.trim() }
            .mapValues { (_, stories) ->
                val total = stories.size
                val completed = stories.count { it.placeId in completedHeroIds }
                DistrictRewardProgress(completed = completed, total = total)
            }

        val completedDistricts = districtProgress.entries
            .filter { (_, progress) -> progress.total > 0 && progress.completed == progress.total }
            .map { it.key }
        val highlightedDistrict = completedDistricts.firstOrNull()
            ?: districtProgress.entries.maxByOrNull { (_, progress) ->
                progress.completionRatio
            }?.key
        val highlightedProgress = highlightedDistrict?.let { districtProgress[it] }
        val districtName = when {
            completedDistricts.isNotEmpty() -> highlightedDistrict
            highlightedDistrict != null -> highlightedDistrict
            else -> profile.homeDistrict.ifBlank { "District" }
        }.orEmpty().ifBlank { "District" }
        val statusText = when {
            completedDistricts.isNotEmpty() -> "Unlocked"
            highlightedProgress != null -> "${highlightedProgress.completed} / ${highlightedProgress.total} stories"
            else -> "Locked"
        }

        return RewardCardUiModel(
            kind = RewardKind.DistrictBadge,
            title = "$districtName District Crest",
            subtitle = when {
                completedDistricts.isNotEmpty() -> "All stories in this district are complete."
                highlightedProgress != null -> "Complete the remaining stories here to unlock the district crest."
                else -> "Finish every story in a district to unlock this badge."
            },
            accent = Color(0xFF2E2A5F),
            icon = Icons.Filled.LocationOn,
            statusText = statusText,
            active = completedDistricts.isNotEmpty()
        )
    }

    private fun rankReward(profile: ProfileJourney): RewardCardUiModel {
        val badgeCount = profile.badgesEarned.distinctBy { it.placeId }.size
        val currentRank = badgeCount.toExplorerRank()
        val nextRank = ExplorerRank.entries
            .sortedBy { it.badgesRequired }
            .firstOrNull { it.badgesRequired > currentRank.badgesRequired }

        return RewardCardUiModel(
            kind = RewardKind.RankPlaque,
            title = currentRank.title,
            subtitle = currentRank.description,
            accent = Color(0xFF2D5A3D),
            icon = Icons.Filled.Star,
            statusText = nextRank?.let { "$badgeCount / ${it.badgesRequired} badges to ${it.title}" }
                ?: "$badgeCount badges earned",
            active = true
        )
    }
}

private const val KEY_COMPLETED_HERO_IDS = "completed_hero_ids"
private const val KEY_COMPLETED_HERO_EARNED_AT_PREFIX = "completed_hero_earned_at_"

private data class DistrictRewardProgress(
    val completed: Int,
    val total: Int
) {
    val completionRatio: Float
        get() = if (total <= 0) 0f else completed.toFloat() / total.toFloat()
}

private fun Long.formatDate(): String {
    return java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(this))
}
