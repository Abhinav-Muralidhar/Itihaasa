package com.itihaasa.nammakathey.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.PlaceType
import com.itihaasa.nammakathey.utils.pinColorForType
import com.itihaasa.nammakathey.ui.onboarding.HomeDistrictSheet
import com.itihaasa.nammakathey.ui.story.StoryBottomSheet
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

private val KarnatakaCenter = LatLng(15.3173, 75.7139)
private val KarnatakaBounds = LatLngBounds(
    LatLng(11.5, 74.0),
    LatLng(18.5, 78.6)
)

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onPlaceClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(KarnatakaCenter, 6f)
    }

    LaunchedEffect(uiState.cameraTarget) {
        uiState.cameraTarget?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 12f),
                durationMs = 800
            )
            viewModel.onCameraTargetConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true
            ),
            properties = MapProperties(
                latLngBoundsForCameraTarget = KarnatakaBounds,
                minZoomPreference = 6f,
                maxZoomPreference = 12f,
                mapStyleOptions = MapStyleOptions(KARNATAKA_MAP_STYLE)
            )
        ) {
            Clustering(
                items = uiState.filteredPlaces,
                onClusterClick = { cluster ->
                    cluster.position?.let { target ->
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(target, 9f))
                    }
                    true
                },
                onClusterItemClick = { place ->
                    viewModel.onPlaceSelected(place)
                    true
                },
                clusterContent = { cluster -> ClusterMarker(cluster) },
                clusterItemContent = { place ->
                    PlaceMarker(
                        place = place,
                        explored = place.id in uiState.exploredPlaceIds,
                        locked = place.district !in uiState.unlockedDistricts
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilterChipsRow(
                activeFilters = uiState.activeFilters,
                onFilterToggled = viewModel::onFilterToggled
            )
        }

        uiState.todayInHistory?.let { place ->
            TodayInHistoryBanner(
                place = place,
                onTap = { viewModel.onPlaceSelected(place) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (uiState.showHomeDistrictSheet) {
            HomeDistrictSheet(
                onDistrictSelected = viewModel::setHomeDistrict,
                onSkip = viewModel::skipHomeDistrict
            )
        }

        uiState.selectedPlace?.let { selectedPlace ->
            val isDistrictUnlocked = selectedPlace.district in uiState.unlockedDistricts
            StoryBottomSheet(
                place = selectedPlace,
                story = uiState.cachedStory,
                isDistrictUnlocked = isDistrictUnlocked,
                homeDistrict = uiState.homeDistrict,
                onReadStory = {
                    onPlaceClick(selectedPlace.id)
                    viewModel.onPlaceDismissed()
                },
                onDismiss = { viewModel.onPlaceDismissed() }
            )
        }
    }
}
private const val KARNATAKA_MAP_STYLE = """
[
  {
    "featureType": "administrative.province",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#C47D28"}, {"weight": 2.2}]
  },
  {
    "featureType": "administrative.country",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#B87422"}, {"weight": 1.6}]
  },
  {
    "featureType": "poi",
    "stylers": [{"visibility": "off"}]
  },
  {
    "featureType": "transit",
    "stylers": [{"visibility": "off"}]
  },
  {
    "featureType": "road",
    "elementType": "labels",
    "stylers": [{"visibility": "off"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [{"color": "#E6D2AE"}, {"weight": 0.55}]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [{"color": "#BFD7E2"}]
  },
  {
    "featureType": "landscape",
    "elementType": "geometry",
    "stylers": [{"color": "#F3E6C8"}]
  }
]
"""

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search places, heroes, districts") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ParchmentLight,
            unfocusedContainerColor = ParchmentLight,
            focusedBorderColor = HeritageOchre,
            unfocusedBorderColor = HeritageOchre.copy(alpha = 0.38f),
            focusedLeadingIconColor = HeritageOchre,
            unfocusedLeadingIconColor = HeritageOchre.copy(alpha = 0.70f),
            cursorColor = RoyalIndigo
        ),
        singleLine = true,
        maxLines = 1
    )
}

@Composable
private fun FilterChipsRow(
    activeFilters: Set<PlaceType>,
    onFilterToggled: (PlaceType) -> Unit
) {
    val labels = mapOf(
        PlaceType.FORT to "Forts",
        PlaceType.TEMPLE to "Temples",
        PlaceType.HERO_SITE to "Heroes",
        PlaceType.BATTLEFIELD to "Battlefields",
        PlaceType.REFORM_SITE to "Reform Sites"
    )

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { (type, label) ->
            FilterChip(
                selected = activeFilters.contains(type),
                onClick = { onFilterToggled(type) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = activeFilters.contains(type),
                    borderColor = RoyalIndigo.copy(alpha = 0.34f),
                    selectedBorderColor = RoyalIndigo
                )
            )
        }
    }
}

@Composable
private fun PlaceMarker(
    place: Place,
    explored: Boolean,
    locked: Boolean
) {
    Box(
        modifier = Modifier
            .size(if (explored) 41.dp else 38.dp)
            .border(BorderStroke(2.dp, Color.White), CircleShape)
            .background(
                when {
                    explored -> HeritageOchre
                    locked -> RoyalIndigo.copy(alpha = 0.82f)
                    else -> pinColorForType(place.type)
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when {
                explored -> Icons.Default.Check
                locked -> Icons.Default.Lock
                else -> Icons.Default.LocationOn
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (locked) 18.dp else 22.dp)
        )
    }
}
@Composable
private fun ClusterMarker(cluster: Cluster<Place>) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = cluster.size.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodayInHistoryBanner(
    place: Place,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Today in Karnataka History",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Tap to explore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
