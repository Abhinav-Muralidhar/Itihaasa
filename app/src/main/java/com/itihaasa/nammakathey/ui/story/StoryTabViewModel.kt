package com.itihaasa.nammakathey.ui.story

import android.content.SharedPreferences
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
    private val storyProgressRepository: StoryProgressRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryTabUiState())
    val uiState: StateFlow<StoryTabUiState> = _uiState.asStateFlow()
    @Volatile
    private var isRefreshingProgress = false
    private val progressListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (
            !isRefreshingProgress &&
            (
                key == KEY_HOME_DISTRICT ||
                key == KEY_ACTIVE_DISTRICT ||
                key == KEY_COMPLETED_HERO_IDS ||
                key == KEY_UNLOCKED_DISTRICTS
            )
        ) {
            loadData()
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(progressListener)
        loadData()
    }

    fun refreshProgress() {
        loadData()
    }

    private fun loadData() {
        if (isRefreshingProgress) return
        isRefreshingProgress = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localHomeDistrict = sharedPreferences.getString("home_district", null)
                val progressState = storyProgressRepository.getProgressState(localHomeDistrict)
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
                        isLoading = false
                    )
                }
            } finally {
                isRefreshingProgress = false
            }
        }
    }

    fun onDistrictSelected(districtName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val heroes = storyCatalogDataSource.getStoriesByDistrict(districtName)
            storyProgressRepository.setActiveDistrict(districtName)
            _uiState.update {
                it.copy(
                    currentDistrict = districtName,
                    heroesInCurrentDistrict = heroes
                )
            }
        }
    }

    fun onDistrictUnlocked(districtName: String) {
        _uiState.update {
            it.copy(
                unlockedDistricts = it.unlockedDistricts + districtName,
                currentDistrict = districtName
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            storyProgressRepository.unlockDistrict(districtName)
            storyProgressRepository.setActiveDistrict(districtName)
        }
        onDistrictSelected(districtName)
    }

    fun isHeroUnlocked(hero: StoryCatalogEntry): Boolean {
        val state = _uiState.value
        return hero.district in state.unlockedDistricts && hero.placeId in state.availableStoryIds
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(progressListener)
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
    val isLoading: Boolean = true
)

data class DistrictStoryProgress(
    val completed: Int = 0,
    val available: Int = 0,
    val total: Int = 0
)

private const val KEY_HOME_DISTRICT = "home_district"
private const val KEY_ACTIVE_DISTRICT = "active_district"
private const val KEY_COMPLETED_HERO_IDS = "completed_hero_ids"
private const val KEY_UNLOCKED_DISTRICTS = "unlocked_districts"
