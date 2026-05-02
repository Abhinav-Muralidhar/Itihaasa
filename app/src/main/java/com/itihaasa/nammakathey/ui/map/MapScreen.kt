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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.itihaasa.nammakathey.ui.story.StoryBottomSheet
import com.itihaasa.nammakathey.ui.story.StoryViewModel
import com.itihaasa.nammakathey.utils.pinColorForType

private val KarnatakaCenter = LatLng(15.3173, 75.7139)
private val KarnatakaBounds = LatLngBounds(
    LatLng(11.5, 74.0),
    LatLng(18.5, 78.6)
)

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    storyViewModel: StoryViewModel = hiltViewModel(),
    onProfileClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val storyUiState by storyViewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(KarnatakaCenter, 6.5f)
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

    LaunchedEffect(uiState.selectedPlace?.id) {
        uiState.selectedPlace?.let { place ->
            storyViewModel.loadStory(place)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true
            ),
            properties = MapProperties(
                latLngBoundsForCameraTarget = KarnatakaBounds,
                minZoomPreference = 5.5f,
                mapStyleOptions = MapStyleOptions(ParchmentMapStyleJson)
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
                    false
                },
                clusterContent = { cluster -> ClusterMarker(cluster) },
                clusterItemContent = { place -> PlaceMarker(place) }
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
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    tonalElevation = 2.dp
                ) {
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilterChipsRow(
                activeFilters = uiState.activeFilters,
                onFilterToggled = viewModel::onFilterToggled
            )
        }

        uiState.todayInHistory?.takeIf { uiState.selectedPlace == null }?.let { place ->
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

        if (storyUiState.place != null) {
            StoryBottomSheet(
                uiState = storyUiState,
                onQuestionSubmitted = storyViewModel::sendChatQuestion,
                onLanguageSelected = storyViewModel::switchLanguage,
                onGoogleSignInToken = storyViewModel::signInWithGoogle,
                onSaveBadge = storyViewModel::saveBadge,
                onDismiss = {
                    storyViewModel.clearStory()
                    viewModel.onSelectedPlaceDismissed()
                }
            )
        }
    }
}

private val ParchmentMapStyleJson = """
[
  {
    "featureType": "all",
    "elementType": "geometry",
    "stylers": [
      { "color": "#F5EDD6" }
    ]
  },
  {
    "featureType": "all",
    "elementType": "labels.icon",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "all",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#2C2C2C" }
    ]
  },
  {
    "featureType": "all",
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#F5EDD6" }
    ]
  },
  {
    "featureType": "administrative.country",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#9A5F2E" },
      { "weight": 1.8 }
    ]
  },
  {
    "featureType": "administrative.province",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#C17B3F" },
      { "weight": 1.7 }
    ]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#C17B3F" },
      { "weight": 0.9 },
      { "visibility": "on" }
    ]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "labels",
    "stylers": [
      { "visibility": "on" }
    ]
  },
  {
    "featureType": "administrative.neighborhood",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#D09A67" },
      { "weight": 0.6 },
      { "visibility": "on" }
    ]
  },
  {
    "featureType": "administrative.land_parcel",
    "elementType": "all",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "landscape",
    "elementType": "geometry",
    "stylers": [
      { "color": "#F5EDD6" }
    ]
  },
  {
    "featureType": "landscape.natural",
    "elementType": "geometry",
    "stylers": [
      { "color": "#EFE3C8" }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "geometry",
    "stylers": [
      { "color": "#EFE4CB" },
      { "visibility": "simplified" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      { "color": "#D4C4A8" },
      { "visibility": "simplified" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      { "color": "#CDB78F" },
      { "weight": 0.9 },
      { "visibility": "simplified" }
    ]
  },
  {
    "featureType": "road.arterial",
    "elementType": "geometry",
    "stylers": [
      { "color": "#D8C8AA" },
      { "weight": 0.7 },
      { "visibility": "simplified" }
    ]
  },
  {
    "featureType": "road.local",
    "elementType": "geometry",
    "stylers": [
      { "color": "#E4D8C2" },
      { "weight": 0.45 },
      { "visibility": "simplified" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "transit",
    "elementType": "all",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      { "color": "#C4DDE8" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#53717C" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#DDEAEF" }
    ]
  }
]
""".trimIndent()

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
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
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
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    selectedBorderColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}

@Composable
private fun PlaceMarker(place: Place) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .border(BorderStroke(2.dp, Color.White), CircleShape)
            .background(pinColorForType(place.type), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
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

@Composable
private fun PlaceDetailsCard(
    place: Place,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(pinColorForType(place.type), CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${place.district} - ${place.era}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}
