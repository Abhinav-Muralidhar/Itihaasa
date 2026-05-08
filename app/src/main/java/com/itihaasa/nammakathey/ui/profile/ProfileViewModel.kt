package com.itihaasa.nammakathey.ui.profile

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
import com.itihaasa.nammakathey.model.ExplorerRank
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
    private val storyCatalogDataSource: StoryCatalogDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var profileJob: Job? = null

    init {
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
                    val districtProgress = profile?.let(::buildDistrictProgress).orEmpty()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            rewardCards = profile?.let(::buildRewardCards).orEmpty(),
                            districtProgress = districtProgress,
                            completedDistrictCount = districtProgress.values.count { it.isComplete },
                            errorMessage = null
                        )
                    }
                }
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
