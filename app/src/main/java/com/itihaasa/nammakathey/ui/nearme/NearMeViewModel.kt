package com.itihaasa.nammakathey.ui.nearme

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.itihaasa.nammakathey.data.local.LocationsDataSource
import com.itihaasa.nammakathey.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NearMeUiState(
    val heroPlaceId: String? = null,
    val heroTitle: String? = null,
    val radiusKm: Double = 15.0,
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val nearby: List<NearMeItem> = emptyList(),
    val errorMessage: String? = null
)

data class NearMeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
    val placeId: String? = null
)

@HiltViewModel
class NearMeViewModel @Inject constructor(
    private val locationsDataSource: LocationsDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(NearMeUiState())
    val uiState: StateFlow<NearMeUiState> = _uiState.asStateFlow()

    fun setHero(placeId: String?) {
        val heroPlace = placeId?.let { locationsDataSource.getPlaceById(it) }
        _uiState.update {
            it.copy(
                heroPlaceId = placeId,
                heroTitle = heroPlace?.name
            )
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted, errorMessage = null) }
    }

    fun setRadiusKm(radiusKm: Double) {
        _uiState.update { it.copy(radiusKm = radiusKm) }
        refresh()
    }

    fun refresh() {
        val state = _uiState.value
        if (!state.permissionGranted) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val fused = LocationServices.getFusedLocationProviderClient(context)
            val token = CancellationTokenSource()
            val location = runCatching {
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token).await()
            }.getOrNull()

            if (location == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn’t get your location. Turn on Location and try again."
                    )
                }
                return@launch
            }

            val lat = location.latitude
            val lng = location.longitude
            val radius = _uiState.value.radiusKm
            val rawPlaces = locationsDataSource.getPlacesNear(lat, lng, radiusKm = radius)
            val items = buildItems(
                origin = location,
                places = rawPlaces,
                heroPlaceId = _uiState.value.heroPlaceId
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentLat = lat,
                    currentLng = lng,
                    nearby = items,
                    errorMessage = null
                )
            }
        }
    }

    private fun buildItems(
        origin: Location,
        places: List<Place>,
        heroPlaceId: String?
    ): List<NearMeItem> {
        val filtered = if (heroPlaceId.isNullOrBlank()) {
            places
        } else {
            places.filter { it.id == heroPlaceId }
        }

        val placeItems = filtered.map { place ->
            val d = origin.distanceTo(place.toLocation()) / 1000.0
            NearMeItem(
                id = place.id,
                title = place.name,
                subtitle = "${place.district} • ${place.era}",
                lat = place.lat,
                lng = place.lng,
                distanceKm = d,
                placeId = place.id
            )
        }

        val memorialItems = filtered
            .filter { it.memorialLat != null && it.memorialLng != null && !it.memorialName.isNullOrBlank() }
            .map { place ->
                val memorialLat = place.memorialLat ?: place.lat
                val memorialLng = place.memorialLng ?: place.lng
                val memorialLoc = Location("memorial").apply {
                    latitude = memorialLat
                    longitude = memorialLng
                }
                val d = origin.distanceTo(memorialLoc) / 1000.0
                NearMeItem(
                    id = "${place.id}::memorial",
                    title = place.memorialName ?: "Memorial",
                    subtitle = "${place.name} • ${place.district}",
                    lat = memorialLat,
                    lng = memorialLng,
                    distanceKm = d,
                    placeId = place.id
                )
            }

        return (placeItems + memorialItems).sortedBy { it.distanceKm }
    }

    private fun Place.toLocation(): Location = Location("place").apply {
        latitude = lat
        longitude = lng
    }
}

