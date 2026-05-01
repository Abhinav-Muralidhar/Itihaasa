Map Screen — Implementation Plan

Project: NammaKathey
Package: com.itihaasa.nammakathey

🚀 Phase 1 — App Entry Setup (Hilt + Activity)
✅ Goal

Enable dependency injection and bootstrap Compose UI.

🔧 Steps

Create / update:

MainActivity.kt

package com.itihaasa.nammakathey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.itihaasa.nammakathey.ui.theme.NammaKatheyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
enableEdgeToEdge()
setContent {
NammaKatheyTheme {
NammaKatheyApp()
}
}
}
}
🧠 Outcome
Hilt is ready
Compose UI root is connected
🧭 Phase 2 — Navigation Setup
✅ Goal

Create a simple navigation graph (Map → Profile)

🔧 Steps

Create:

NammaKatheyApp.kt

package com.itihaasa.nammakathey

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*

import com.itihaasa.nammakathey.ui.map.MapScreen
import com.itihaasa.nammakathey.ui.profile.ProfileScreen

sealed class Screen(val route: String) {
object Map : Screen("map")
object Profile : Screen("profile")
}

@Composable
fun NammaKatheyApp() {
val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
🧠 Outcome
Navigation works
Map is default screen
🧠 Phase 3 — ViewModel (Core Logic Layer)
✅ Goal

Manage map data, filtering, and UI state

🔧 Steps

Create:

ui/map/MapViewModel.kt

⚠️ Replace package imports accordingly

package com.itihaasa.nammakathey.ui.map

✔ Copy your existing ViewModel code (no logic changes needed)

🧠 Responsibilities
Load places from LocationsDataSource
Manage filters & search
Expose StateFlow<MapUiState>
🧱 Phase 4 — Domain Model
✅ Goal

Define Place + PlaceType

🔧 Steps

Update:

model/Place.kt

package com.itihaasa.nammakathey.model

✔ Keep same structure

⚠️ Compatibility note:

PlaceType.entries.toSet()
// OR (if error)
PlaceType.values().toSet()
🗺️ Phase 5 — Map UI (Core Feature)
✅ Goal

Render Google Map + markers + filters + search

🔧 Steps

Create:

ui/map/MapScreen.kt

package com.itihaasa.nammakathey.ui.map

✔ Copy your full MapScreen implementation

🧠 What this screen includes
1. 🗺 Map Layer
   Google Map Compose
   Marker per Place
2. 🔍 Search
   Filters by:
   name
   district
   keywords
3. 🧩 Filter Chips
   Forts
   Temples
   Heroes
   Battlefields
   Reform sites
4. 🗓 Today in History Banner
   Contextual storytelling hook
5. ⏳ Loading State
   Circular indicator
   👤 Phase 6 — Profile Placeholder
   ✅ Goal

Avoid navigation crashes

🔧 Steps

Create:

ui/profile/ProfileScreen.kt

package com.itihaasa.nammakathey.ui.profile

✔ Minimal placeholder UI

🎨 Phase 7 — Theme (Brand Identity)
✅ Goal

Apply heritage-inspired design

🔧 Steps

Update:

ui/theme/Color.kt

package com.itihaasa.nammakathey.ui.theme

Add palette:

Forest Green
Ochre
Parchment
Charcoal

Update:

Theme.kt

private val LightColorScheme = lightColorScheme(
primary = ForestGreen,
secondary = HeritageOchre,
background = Parchment,
surface = Color.White,
onPrimary = Color.White,
onBackground = Charcoal,
onSurface = Charcoal,
)
▶️ Phase 8 — Run & Verify



The missing pieces from your goal list are:

Custom marker drawables by type (colored pins)
Marker clustering
Camera bounds locked to Karnataka
Search animates camera to result

Let me give you exactly those.

Step 1 — Add Clustering Dependency
In app/build.gradle.kts dependencies, you should already have:
kotlinimplementation("com.google.maps.android:maps-compose:4.3.3")
Add the clustering library below it:
kotlinimplementation("com.google.maps.android:maps-compose-utils:4.3.3")
Sync Gradle.

Step 2 — Create Custom Pin Colors
In Android, Google Maps markers support BitmapDescriptorFactory for colored pins. Create utils/MapUtils.kt:
kotlinpackage com.abhinav.nammakathey.utils

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.abhinav.nammakathey.model.PlaceType

fun pinIconForType(type: PlaceType): BitmapDescriptor {
val hue = when (type) {
PlaceType.FORT       -> BitmapDescriptorFactory.HUE_AZURE
PlaceType.TEMPLE     -> BitmapDescriptorFactory.HUE_GREEN
PlaceType.HERO_SITE  -> BitmapDescriptorFactory.HUE_ORANGE
PlaceType.BATTLEFIELD -> BitmapDescriptorFactory.HUE_RED
PlaceType.REFORM_SITE -> BitmapDescriptorFactory.HUE_VIOLET
}
return BitmapDescriptorFactory.defaultMarker(hue)
}

Step 3 — Update MapViewModel for Camera Animation
Add this to MapViewModel.kt — a new state field and function for search-triggered camera moves:
kotlinimport com.google.android.gms.maps.model.LatLng

data class MapUiState(
val allPlaces: List<Place> = emptyList(),
val filteredPlaces: List<Place> = emptyList(),
val selectedPlace: Place? = null,
val searchQuery: String = "",
val activeFilters: Set<PlaceType> = PlaceType.entries.toSet(),
val isLoading: Boolean = true,
val todayInHistory: Place? = null,
val cameraTarget: LatLng? = null  // ← add this
)
Add this function inside MapViewModel:
kotlinfun onSearchQueryChanged(query: String) {
_uiState.update { it.copy(searchQuery = query) }
applyFilters()

    // animate camera if exactly one result matches
    if (query.length >= 3) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = locationsDataSource.searchPlaces(query)
            if (results.size == 1) {
                _uiState.update {
                    it.copy(cameraTarget = LatLng(results[0].lat, results[0].lng))
                }
            }
        }
    } else {
        _uiState.update { it.copy(cameraTarget = null) }
    }
}

fun onCameraTargetConsumed() {
_uiState.update { it.copy(cameraTarget = null) }
}

Step 4 — Update MapScreen.kt Completely
Replace your entire MapScreen.kt with this:
kotlinpackage com.abhinav.nammakathey.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.nammakathey.model.Place
import com.abhinav.nammakathey.model.PlaceType
import com.abhinav.nammakathey.utils.pinIconForType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import kotlinx.coroutines.launch

val KARNATAKA_CENTER = LatLng(15.3173, 75.7139)

// Karnataka bounding box — camera won't go outside this
val KARNATAKA_BOUNDS = LatLngBounds(
LatLng(11.5, 74.0),  // SW corner
LatLng(18.5, 78.6)   // NE corner
)

@Composable
fun MapScreen(
viewModel: MapViewModel = hiltViewModel()
) {
val uiState by viewModel.uiState.collectAsState()
val cameraPositionState = rememberCameraPositionState {
position = CameraPosition.fromLatLngZoom(KARNATAKA_CENTER, 6.5f)
}
val scope = rememberCoroutineScope()

    // animate camera when search finds a single result
    LaunchedEffect(uiState.cameraTarget) {
        uiState.cameraTarget?.let { target ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(target, 12f),
                    durationMs = 800
                )
            }
            viewModel.onCameraTargetConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Map ──────────────────────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true
            ),
            properties = MapProperties(
                latLngBoundsForCameraTarget = KARNATAKA_BOUNDS,
                minZoomPreference = 5.5f
            )
        ) {
            // Clustering — groups nearby pins when zoomed out
            Clustering(
                items = uiState.filteredPlaces,
                onClusterItemClick = { place ->
                    viewModel.onPlaceSelected(place)
                    false
                },
                clusterItemContent = null,
                clusterContent = null
            )

            // Individual markers with color by type
            uiState.filteredPlaces.forEach { place ->
                Marker(
                    state = MarkerState(LatLng(place.lat, place.lng)),
                    title = place.name,
                    snippet = "${place.district} · ${place.era}",
                    icon = pinIconForType(place.type),
                    onClick = {
                        viewModel.onPlaceSelected(place)
                        false
                    }
                )
            }
        }

        // ── Search + Filter overlay ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 48.dp)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterChipsRow(
                activeFilters = uiState.activeFilters,
                onFilterToggled = viewModel::onFilterToggled
            )
        }

        // ── Today in history banner ───────────────────────────────────
        uiState.todayInHistory?.let { place ->
            TodayInHistoryBanner(
                place = place,
                onTap = { viewModel.onPlaceSelected(place) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            )
        }

        // ── Loading indicator ─────────────────────────────────────────
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SearchBar(
query: String,
onQueryChange: (String) -> Unit
) {
OutlinedTextField(
value = query,
onValueChange = onQueryChange,
modifier = Modifier.fillMaxWidth(),
placeholder = { Text("Search places, heroes, districts…") },
leadingIcon = {
Icon(Icons.Default.Search, contentDescription = "Search")
},
shape = RoundedCornerShape(24.dp),
colors = OutlinedTextFieldDefaults.colors(
focusedContainerColor = MaterialTheme.colorScheme.surface,
unfocusedContainerColor = MaterialTheme.colorScheme.surface,
unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
),
singleLine = true
)
}

@Composable
fun FilterChipsRow(
activeFilters: Set<PlaceType>,
onFilterToggled: (PlaceType) -> Unit
) {
val labels = mapOf(
PlaceType.FORT        to "🏰 Forts",
PlaceType.TEMPLE      to "🛕 Temples",
PlaceType.HERO_SITE   to "⚔️ Heroes",
PlaceType.BATTLEFIELD to "🔴 Battlefields",
PlaceType.REFORM_SITE to "✊ Reform Sites"
)

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { (type, label) ->
            FilterChip(
                selected = activeFilters.contains(type),
                onClick = { onFilterToggled(type) },
                label = {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            )
        }
    }
}

@Composable
fun TodayInHistoryBanner(
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
shape = RoundedCornerShape(16.dp)
) {
Row(
modifier = Modifier.padding(12.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text("🗓", style = MaterialTheme.typography.titleLarge)
Spacer(modifier = Modifier.width(10.dp))
Column {
Text(
"Today in Karnataka History",
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.primary
)
Text(
place.name,
style = MaterialTheme.typography.titleSmall,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
Text(
"Tap to explore →",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
}
}
}

Step 5 — Make Place implement ClusterItem
Clustering requires your Place model to implement ClusterItem. Update model/Place.kt:
kotlinpackage com.abhinav.nammakathey.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class Place(
val id: String = "",
val name: String = "",
val lat: Double = 0.0,
val lng: Double = 0.0,
val type: PlaceType = PlaceType.HERO_SITE,
val district: String = "",
val stateId: String = "KA",
val era: String = "",
val seedKeywords: List<String> = emptyList(),
val historicalDate: String? = null
) : ClusterItem {
override fun getPosition(): LatLng = LatLng(lat, lng)
override fun getTitle(): String = name
override fun getSnippet(): String = "$district · $era"
override fun getZIndex(): Float = 0f
}

enum class PlaceType {
FORT, TEMPLE, HERO_SITE, BATTLEFIELD, REFORM_SITE
}

Step 6 — Build and Run
Sync Gradle → Run. You should now see:

Karnataka map locked to Karnataka bounds
25 colored pins by type
Clusters when zoomed out
Filter chips working
Search animates camera to matching location