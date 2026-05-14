package com.itihaasa.nammakathey.ui.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.local.DistrictDataSource
import com.itihaasa.nammakathey.data.local.OfflineStoryDataSource
import com.itihaasa.nammakathey.data.local.StoryCatalogDataSource
import com.itihaasa.nammakathey.data.repository.StoryProgressRepository
import com.itihaasa.nammakathey.model.District
import com.itihaasa.nammakathey.model.StoryCatalogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StoryTabViewModel @Inject constructor(
    private val districtDataSource: DistrictDataSource,
    private val storyCatalogDataSource: StoryCatalogDataSource,
    private val offlineStoryDataSource: OfflineStoryDataSource,
    private val storyProgressRepository: StoryProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryTabUiState())
    val uiState: StateFlow<StoryTabUiState> = _uiState.asStateFlow()
    @Volatile
    private var isRefreshingProgress = false
    @Volatile
    private var refreshQueued = false

    init {
        loadData()
    }

    fun refreshProgress() {
        loadData()
    }

    private fun loadData() {
        if (isRefreshingProgress) {
            refreshQueued = true
            return
        }
        isRefreshingProgress = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val progressState = storyProgressRepository.getProgressState(localHomeDistrict = null)
                val homeDistrict = progressState.homeDistrict
                val activeDistrict = progressState.activeDistrict

                val availableStoryIds = offlineStoryDataSource.getStoryPlaceIds()
                val completedHeroIds = progressState.completedHeroIds
                val allHeroes = storyCatalogDataSource.stories
                val districtProgress = allHeroes
                    .groupBy { it.district }
                    .mapValues { (_, heroes) ->
                        DistrictStoryProgress(
                            completed = heroes.count { it.placeId in completedHeroIds },
                            available = heroes.count { it.placeId in availableStoryIds },
                            total = heroes.size
                        )
                    }
                val completedDistricts = districtProgress
                    .filter { (_, progress) -> progress.total > 0 && progress.completed == progress.total }
                    .keys
                    .toSet()
                val unlockedDistricts = progressState.unlockedDistricts
                val currentDistrict = activeDistrict
                    ?: homeDistrict
                    ?: districtDataSource.districts.firstOrNull()?.name

                val heroes = currentDistrict?.let { district ->
                    storyCatalogDataSource.getStoriesByDistrict(district)
                } ?: emptyList()

                _uiState.update {
                    it.copy(
                        districts = districtDataSource.districts,
                        homeDistrict = homeDistrict,
                        currentDistrict = currentDistrict,
                        unlockedDistricts = unlockedDistricts,
                        completedDistricts = completedDistricts,
                        districtProgress = districtProgress,
                        heroesInCurrentDistrict = heroes,
                        availableStoryIds = availableStoryIds,
                        completedHeroIds = completedHeroIds,
                        lockedDistrictNotice = null,
                        isLoading = false
                    )
                }
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lockedDistrictNotice = throwable.message ?: "Could not load your saved story progress."
                    )
                }
            } finally {
                isRefreshingProgress = false
                if (refreshQueued) {
                    refreshQueued = false
                    loadData()
                }
            }
        }
    }

    fun onDistrictSelected(districtName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val heroes = storyCatalogDataSource.getStoriesByDistrict(districtName)
            runCatching {
                storyProgressRepository.setActiveDistrict(districtName)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        currentDistrict = districtName,
                        heroesInCurrentDistrict = heroes,
                        lockedDistrictNotice = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        lockedDistrictNotice = throwable.message ?: "Could not switch districts. Check your connection and try again."
                    )
                }
            }
        }
    }

    fun openDistrictFromMap(districtName: String?) {
        val requestedDistrict = districtName?.takeIf { it.isNotBlank() } ?: return
        val state = _uiState.value
        if (state.isLoading) return
        if (requestedDistrict in state.unlockedDistricts) {
            onDistrictSelected(requestedDistrict)
            return
        }

        val fallbackDistrict = state.currentDistrict ?: state.homeDistrict
        val fallbackHeroes = fallbackDistrict
            ?.let { storyCatalogDataSource.getStoriesByDistrict(it) }
            .orEmpty()
        _uiState.update {
            it.copy(
                currentDistrict = fallbackDistrict,
                heroesInCurrentDistrict = fallbackHeroes,
                lockedDistrictNotice = if (fallbackDistrict.isNullOrBlank()) {
                    "$requestedDistrict is locked. Choose your home district to begin story mode."
                } else {
                    "$requestedDistrict is locked. Complete every story in $fallbackDistrict to unlock more districts."
                }
            )
        }
    }

    fun onDistrictUnlocked(districtName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                storyProgressRepository.unlockDistrict(districtName)
                storyProgressRepository.setActiveDistrict(districtName)
            }.onSuccess {
                val heroes = storyCatalogDataSource.getStoriesByDistrict(districtName)
                _uiState.update {
                    it.copy(
                        unlockedDistricts = it.unlockedDistricts + districtName,
                        currentDistrict = districtName,
                        heroesInCurrentDistrict = heroes,
                        lockedDistrictNotice = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        lockedDistrictNotice = throwable.message ?: "Could not unlock this district. Check your connection and try again."
                    )
                }
            }
        }
    }

    fun isHeroUnlocked(hero: StoryCatalogEntry): Boolean {
        val state = _uiState.value
        return hero.district in state.unlockedDistricts && hero.placeId in state.availableStoryIds
    }

    override fun onCleared() {
        super.onCleared()
    }
}

data class StoryTabUiState(
    val districts: List<District> = emptyList(),
    val homeDistrict: String? = null,
    val currentDistrict: String? = null,
    val unlockedDistricts: Set<String> = emptySet(),
    val completedDistricts: Set<String> = emptySet(),
    val districtProgress: Map<String, DistrictStoryProgress> = emptyMap(),
    val heroesInCurrentDistrict: List<StoryCatalogEntry> = emptyList(),
    val availableStoryIds: Set<String> = emptySet(),
    val completedHeroIds: Set<String> = emptySet(),
    val lockedDistrictNotice: String? = null,
    val isLoading: Boolean = true
)

data class DistrictStoryProgress(
    val completed: Int = 0,
    val available: Int = 0,
    val total: Int = 0
)
