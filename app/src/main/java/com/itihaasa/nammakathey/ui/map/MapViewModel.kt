package com.itihaasa.nammakathey.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class MapUiState(
    val allPlaces: List<Place> = emptyList(),
    val filteredPlaces: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val searchQuery: String = "",
    val activeFilters: Set<PlaceType> = PlaceType.entries.toSet(),
    val isLoading: Boolean = true,
    val todayInHistory: Place? = null,
    val cameraTarget: LatLng? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationsDataSource: LocationsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
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
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun onSelectedPlaceDismissed() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    fun onCameraTargetConsumed() {
        _uiState.update { it.copy(cameraTarget = null) }
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
}
