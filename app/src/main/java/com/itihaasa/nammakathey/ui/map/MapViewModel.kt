package com.itihaasa.nammakathey.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.maps.model.LatLng
import com.itihaasa.nammakathey.data.local.LocationsDataSource
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.PlaceType
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
    val searchQuery: String = "",
    val activeFilters: Set<PlaceType> = PlaceType.entries.toSet(),
    val isLoading: Boolean = true,
    val todayInHistory: Place? = null,
    val cameraTarget: LatLng? = null,
    val exploredPlaceIds: Set<String> = emptySet(),
    val exploredDistricts: Set<String> = emptySet(),
    val unlockedDistricts: Set<String> = emptySet(),
    val homeDistrict: String? = null,
    val activeDistrict: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationsDataSource: LocationsDataSource,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private var progressRegistration: ListenerRegistration? = null

    init {
        loadPlaces()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()

        if (query.trim().length >= 3) {
            viewModelScope.launch(Dispatchers.IO) {
                val activeFilters = _uiState.value.activeFilters
                val results = locationsDataSource.searchPlaces(query)
                    .filter { place -> place.type in activeFilters }
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
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun onPlaceDismissed() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    fun onCameraTargetConsumed() {
        _uiState.update { it.copy(cameraTarget = null) }
    }

    fun refreshUserProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            loadUserProgressOnce()
        }
    }

    private fun loadPlaces() {
        viewModelScope.launch(Dispatchers.IO) {
            val places = locationsDataSource.getAllPlaces()
            _uiState.update {
                it.copy(
                    allPlaces = places,
                    filteredPlaces = places,
                    isLoading = false,
                    todayInHistory = locationsDataSource.getTodayInHistory()
                )
            }
            observeUserProgress()
        }
    }

    private fun observeUserProgress() {
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return
        progressRegistration?.remove()
        progressRegistration = firestore.collection("users")
            .document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Could not sync your map progress.")
                    }
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    _uiState.update {
                        it.copy(errorMessage = "Complete your profile setup to unlock your map journey.")
                    }
                    return@addSnapshotListener
                }
                val badges = (snapshot?.get("badgesEarned") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    .orEmpty()
                applyProgressSnapshot(
                    badges = badges,
                    completedHeroIds = (snapshot.get("completedHeroIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                        .toSet(),
                    homeDistrict = snapshot.getString("homeDistrict")?.takeIf { it.isNotBlank() },
                    activeDistrict = snapshot.getString("activeDistrict")?.takeIf { it.isNotBlank() },
                    remoteUnlocked = (snapshot.get("unlockedDistricts") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                        .toSet()
                )
            }
    }

    private suspend fun loadUserProgressOnce() {
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return
        runCatching {
            firestore.collection("users").document(user.uid).get().await()
        }.onSuccess { snapshot ->
            if (!snapshot.exists()) return@onSuccess
            val badges = (snapshot.get("badgesEarned") as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                .orEmpty()
            applyProgressSnapshot(
                badges = badges,
                completedHeroIds = (snapshot.get("completedHeroIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                    .toSet(),
                homeDistrict = snapshot.getString("homeDistrict")?.takeIf { it.isNotBlank() },
                activeDistrict = snapshot.getString("activeDistrict")?.takeIf { it.isNotBlank() },
                remoteUnlocked = (snapshot.get("unlockedDistricts") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                    .toSet()
            )
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(errorMessage = throwable.message ?: "Could not refresh your map progress.")
            }
        }
    }

    private fun applyProgressSnapshot(
        badges: List<Map<*, *>>,
        completedHeroIds: Set<String>,
        homeDistrict: String?,
        activeDistrict: String?,
        remoteUnlocked: Set<String>
    ) {
        val exploredIds = badges.mapNotNull { it["placeId"] as? String }.toSet() + completedHeroIds
        val exploredDistricts = badges.mapNotNull { it["district"] as? String }.toSet()
        _uiState.update { state ->
            val nextHomeDistrict = homeDistrict ?: state.homeDistrict
            val nextActiveDistrict = activeDistrict ?: state.activeDistrict ?: nextHomeDistrict
            state.copy(
                exploredPlaceIds = exploredIds,
                exploredDistricts = exploredDistricts,
                unlockedDistricts = remoteUnlocked + listOfNotNull(nextHomeDistrict, nextActiveDistrict),
                homeDistrict = nextHomeDistrict,
                activeDistrict = nextActiveDistrict,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        progressRegistration?.remove()
        super.onCleared()
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
}
