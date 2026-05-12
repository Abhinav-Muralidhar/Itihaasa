package com.itihaasa.nammakathey.ui.map

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.maps.model.LatLng
import com.itihaasa.nammakathey.data.local.LocationsDataSource
import com.itihaasa.nammakathey.data.repository.StoryRepository
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.PlaceType
import com.itihaasa.nammakathey.model.Story
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MapUiState(
    val allPlaces: List<Place> = emptyList(),
    val filteredPlaces: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val cachedStory: Story? = null,
    val searchQuery: String = "",
    val activeFilters: Set<PlaceType> = PlaceType.entries.toSet(),
    val isLoading: Boolean = true,
    val todayInHistory: Place? = null,
    val cameraTarget: LatLng? = null,
    val exploredPlaceIds: Set<String> = emptySet(),
    val exploredDistricts: Set<String> = emptySet(),
    val unlockedDistricts: Set<String> = emptySet(),
    val homeDistrict: String? = null,
    val showHomeDistrictSheet: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationsDataSource: LocationsDataSource,
    private val storyRepository: StoryRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences(MAP_PREFERENCES, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private var progressRegistration: ListenerRegistration? = null
    private val progressListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_COMPLETED_HERO_IDS) {
            applyLocalProgress()
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(progressListener)
        loadPlaces()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()

        if (query.trim().length >= 3) {
            viewModelScope.launch(Dispatchers.IO) {
                val results = locationsDataSource.searchPlaces(query)
                    .filter { place -> _uiState.value.activeFilters.contains(place.type) }
                val target = results.singleOrNull()?.let { LatLng(it.lat, it.lng) }
                _uiState.update { it.copy(cameraTarget = target) }
            }
        } else {
            _uiState.update { it.copy(cameraTarget = null) }
        }
    }

    fun onFilterToggled(type: PlaceType) {
        _uiState.update { state ->
            val nextFilters = if (type in state.activeFilters) {
                state.activeFilters - type
            } else {
                state.activeFilters + type
            }
            state.copy(activeFilters = nextFilters)
        }
        applyFilters()
    }

    fun onPlaceSelected(place: Place) {
        _uiState.update { it.copy(selectedPlace = place, cachedStory = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val cached = runCatching {
                storyRepository.getStory(place.id, "en")
            }.getOrNull()
            _uiState.update { state ->
                if (state.selectedPlace?.id == place.id) {
                    state.copy(cachedStory = cached)
                } else {
                    state
                }
            }
        }
    }

    fun setHomeDistrict(district: String) {
        preferences.edit()
            .putString(KEY_HOME_DISTRICT, district)
            .putBoolean(KEY_HOME_DISTRICT_SET, true)
            .apply()
        val target = locationsDataSource.getDistrictCentroid(district)
        _uiState.update {
            it.copy(
                homeDistrict = district,
                showHomeDistrictSheet = false,
                cameraTarget = target
            )
        }
        updateUnlockedDistricts()
        saveHomeDistrictToFirestore(district)
    }

    fun skipHomeDistrict() {
        preferences.edit().putBoolean(KEY_HOME_DISTRICT_SET, true).apply()
        _uiState.update { it.copy(showHomeDistrictSheet = false) }
        updateUnlockedDistricts()
    }

    fun onPlaceDismissed() {
        _uiState.update { it.copy(selectedPlace = null, cachedStory = null) }
    }

    fun onCameraTargetConsumed() {
        _uiState.update { it.copy(cameraTarget = null) }
    }

    private fun loadPlaces() {
        viewModelScope.launch(Dispatchers.IO) {
            val places = locationsDataSource.getAllPlaces()
            val homeDistrict = preferences.getString(KEY_HOME_DISTRICT, null)
            val homeDistrictSet = preferences.getBoolean(KEY_HOME_DISTRICT_SET, false)
            val signedInUser = firebaseAuth.currentUser?.takeUnless { it.isAnonymous }
            val localExploredIds = getLocalCompletedHeroIds()
            val localExploredDistricts = places
                .filter { it.id in localExploredIds }
                .map { it.district }
                .toSet()
            _uiState.update {
                it.copy(
                    allPlaces = places,
                    filteredPlaces = places,
                    isLoading = false,
                    todayInHistory = locationsDataSource.getTodayInHistory(),
                    exploredPlaceIds = localExploredIds,
                    exploredDistricts = localExploredDistricts,
                    homeDistrict = homeDistrict,
                    showHomeDistrictSheet = !homeDistrictSet && signedInUser == null
                )
            }
            observeUserProgress()
            updateUnlockedDistricts()
        }
    }

    private fun observeUserProgress() {
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return
        progressRegistration?.remove()
        progressRegistration = firestore.collection("users")
            .document(user.uid)
            .addSnapshotListener { snapshot, _ ->
                val badges = (snapshot?.get("badgesEarned") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    .orEmpty()
                val exploredIds = badges.mapNotNull { it["placeId"] as? String }.toSet()
                val exploredDistricts = badges.mapNotNull { it["district"] as? String }.toSet()
                val localExploredIds = getLocalCompletedHeroIds()
                val firestoreHomeDistrict = snapshot?.getString("homeDistrict")
                    ?.takeIf { it.isNotBlank() }
                _uiState.update {
                    val mergedExploredIds = it.exploredPlaceIds + exploredIds + localExploredIds
                    val localExploredDistricts = it.allPlaces
                        .filter { place -> place.id in mergedExploredIds }
                        .map { place -> place.district }
                        .toSet()
                    it.copy(
                        exploredPlaceIds = mergedExploredIds,
                        exploredDistricts = exploredDistricts + localExploredDistricts,
                        homeDistrict = firestoreHomeDistrict ?: it.homeDistrict,
                        showHomeDistrictSheet = when {
                            firestoreHomeDistrict != null -> false
                            it.homeDistrict.isNullOrBlank() -> true
                            else -> it.showHomeDistrictSheet
                        }
                    )
                }
                firestoreHomeDistrict?.let { district ->
                    preferences.edit()
                        .putString(KEY_HOME_DISTRICT, district)
                        .putBoolean(KEY_HOME_DISTRICT_SET, true)
                        .apply()
                }
                updateUnlockedDistricts()
            }
    }

    private fun applyLocalProgress() {
        val localExploredIds = getLocalCompletedHeroIds()
        _uiState.update { state ->
            val mergedExploredIds = state.exploredPlaceIds + localExploredIds
            state.copy(
                exploredPlaceIds = mergedExploredIds,
                exploredDistricts = state.exploredDistricts + state.allPlaces
                    .filter { it.id in mergedExploredIds }
                    .map { it.district }
                    .toSet()
            )
        }
        updateUnlockedDistricts()
    }

    private fun getLocalCompletedHeroIds(): Set<String> =
        preferences.getStringSet(KEY_COMPLETED_HERO_IDS, emptySet()).orEmpty()

    override fun onCleared() {
        progressRegistration?.remove()
        preferences.unregisterOnSharedPreferenceChangeListener(progressListener)
        super.onCleared()
    }

    private fun updateUnlockedDistricts() {
        _uiState.update { state ->
            val badgeCount = state.exploredPlaceIds.size
            val homeDistrict = state.homeDistrict
            val unlocked = buildSet {
                if (!homeDistrict.isNullOrBlank()) add(homeDistrict)
                addAll(state.exploredDistricts)
                if (badgeCount >= 1 && !homeDistrict.isNullOrBlank()) {
                    addAll(
                        state.allPlaces
                            .filter { it.district == homeDistrict }
                            .flatMap { it.adjacentDistricts }
                    )
                }
                if (badgeCount >= 3 && !homeDistrict.isNullOrBlank()) {
                    addAll(state.allPlaces.map { it.district })
                }
            }
            state.copy(unlockedDistricts = unlocked)
        }
    }

    private fun saveHomeDistrictToFirestore(district: String) {
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                firestore.collection("users")
                    .document(user.uid)
                    .set(mapOf("homeDistrict" to district), SetOptions.merge())
            }
        }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val query = state.searchQuery.trim().lowercase()
            val filtered = state.allPlaces.filter { place ->
                val matchesFilter = place.type in state.activeFilters
                val matchesQuery = query.isBlank() ||
                    place.name.lowercase().contains(query) ||
                    place.district.lowercase().contains(query) ||
                    place.seedKeywords.any { keyword -> keyword.lowercase().contains(query) }
                matchesFilter && matchesQuery
            }
            state.copy(filteredPlaces = filtered)
        }
    }

    private companion object {
        const val MAP_PREFERENCES = "itihaasa_prefs"
        const val KEY_HOME_DISTRICT = "home_district"
        const val KEY_HOME_DISTRICT_SET = "home_district_set"
        const val KEY_COMPLETED_HERO_IDS = "completed_hero_ids"
    }
}
