package com.itihaasa.nammakathey.ui.story

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.itihaasa.nammakathey.model.District
import com.itihaasa.nammakathey.model.StoryCatalogEntry
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@Composable
fun StoryTabScreen(
    viewModel: StoryTabViewModel = hiltViewModel(),
    onStoryClick: (placeId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = HeritageOchre
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StoryModeHeader(
                        homeDistrict = uiState.homeDistrict,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.28f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        DistrictMap(
                            districts = uiState.districts,
                            homeDistrict = uiState.homeDistrict,
                            currentDistrict = uiState.currentDistrict,
                            unlockedDistricts = uiState.unlockedDistricts,
                            completedDistricts = uiState.completedDistricts,
                            districtProgress = uiState.districtProgress,
                            onDistrictTap = { district ->
                                if (district.name in uiState.unlockedDistricts) {
                                    viewModel.onDistrictSelected(district.name)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    }
                }

                uiState.currentDistrict?.let { district ->
                    val progress = uiState.districtProgress[district] ?: DistrictStoryProgress()
                    val total = progress.total
                    val done = progress.completed

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = district,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                color = RoyalIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$done / $total complete",
                                fontSize = 13.sp,
                                color = HeritageOchre
                            )
                        }
                    }

                    item {
                        LinearProgressIndicator(
                            progress = { if (total > 0) done / total.toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = HeritageOchre,
                            trackColor = RoyalIndigo.copy(alpha = 0.15f)
                        )
                    }
                }

                item {
                    Text(
                        text = "Stories",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        color = RoyalIndigo,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.heroesInCurrentDistrict.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f)),
                            color = ParchmentLight
                        ) {
                            Text(
                                text = "No stories are available for this district yet.",
                                modifier = Modifier.padding(16.dp),
                                color = Charcoal.copy(alpha = 0.72f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    itemsIndexed(uiState.heroesInCurrentDistrict) { index, hero ->
                        val isUnlocked = viewModel.isHeroUnlocked(hero)
                        val isCompleted = hero.placeId in uiState.completedHeroIds

                        StoryCard(
                            hero = hero,
                            index = index + 1,
                            isUnlocked = isUnlocked,
                            isCompleted = isCompleted,
                            onClick = {
                                if (isUnlocked) onStoryClick(hero.placeId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryModeHeader(
    homeDistrict: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = RoyalIndigo),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "STORY MODE",
                fontSize = 10.sp,
                color = HeritageOchre,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Karnataka Chronicle",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                color = ParchmentLight,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Unlock districts by completing every available hero story.",
                fontSize = 12.sp,
                color = ParchmentLight.copy(alpha = 0.82f),
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun DistrictMap(
    districts: List<District>,
    homeDistrict: String?,
    currentDistrict: String?,
    unlockedDistricts: Set<String>,
    completedDistricts: Set<String>,
    districtProgress: Map<String, DistrictStoryProgress>,
    onDistrictTap: (District) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(15.3173, 75.7139),
            6f
        )
    }

    LaunchedEffect(currentDistrict, districts) {
        val district = districts.firstOrNull { it.name == currentDistrict }
        if (district != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(district.lat, district.lng),
                    8.25f
                ),
                durationMs = 700
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = false
        ),
        properties = MapProperties(
            minZoomPreference = 4.5f,
            maxZoomPreference = 12f,
            mapStyleOptions = MapStyleOptions(KARNATAKA_MAP_STYLE)
        )
    ) {
        districts.forEach { district ->
            val progress = districtProgress[district.name] ?: DistrictStoryProgress()
            val isCurrent = district.name == currentDistrict
            val isUnlocked = district.name in unlockedDistricts
            val isCompleted = district.name in completedDistricts
            val isHomeInProgress = district.name == homeDistrict && !isCompleted

            MarkerComposable(
                state = MarkerState(LatLng(district.lat, district.lng)),
                title = district.name,
                snippet = when {
                    isCompleted -> "Completed"
                    isCurrent -> "Exploring now"
                    isUnlocked -> "Tap to explore"
                    else -> "Locked"
                },
                onClick = {
                    onDistrictTap(district)
                    false
                },
                zIndex = if (isUnlocked || isCurrent || isCompleted) 2f else 0f
            ) {
                DistrictMarkerContent(
                    label = when {
                        isCompleted -> "✓"
                        isHomeInProgress || isCurrent -> "${progress.completed}/${progress.total}"
                        isUnlocked -> "${progress.completed}/${progress.total}"
                        else -> ""
                    },
                    containerColor = when {
                        isCompleted -> Color(0xFF27AE60)
                        isHomeInProgress || isCurrent -> HeritageOchre
                        isUnlocked -> RoyalIndigo
                        else -> Color(0xFF8D8D8D)
                    },
                    locked = !isUnlocked && !isCompleted,
                    compact = !isUnlocked && !isCompleted,
                    highlighted = isUnlocked || isCurrent || isCompleted
                )
            }
        }
    }
}

@Composable
private fun DistrictMarkerContent(
    label: String,
    containerColor: Color,
    locked: Boolean,
    compact: Boolean,
    highlighted: Boolean
) {
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .border(
                width = if (highlighted) 3.dp else 2.dp,
                color = if (highlighted) HeritageOchre.copy(alpha = 0.65f) else Color.White,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(
                horizontal = when {
                    highlighted -> 12.dp
                    compact -> 8.dp
                    else -> 10.dp
                },
                vertical = if (highlighted) 7.dp else 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        if (locked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (compact) 12.dp else 13.dp)
            )
        } else {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (compact) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private const val KARNATAKA_MAP_STYLE = """
[
  {
    "featureType": "administrative.province",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#2E2A5F"}, {"weight": 2}]
  },
  {
    "featureType": "administrative.country",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#C47D28"}, {"weight": 1.5}]
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
    "stylers": [{"color": "#EDE0C4"}, {"weight": 0.5}]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [{"color": "#C4DDE8"}]
  },
  {
    "featureType": "landscape",
    "elementType": "geometry",
    "stylers": [{"color": "#F6EEDC"}]
  }
]
"""

@Composable
fun StoryCard(
    hero: StoryCatalogEntry,
    index: Int,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> RoyalIndigo.copy(alpha = 0.10f)
                isUnlocked -> ParchmentLight
                else -> Parchment
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 3.dp else 0.dp
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isUnlocked) {
            BorderStroke(1.dp, RoyalIndigo.copy(alpha = if (isCompleted) 0.28f else 0.36f))
        } else {
            BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.10f))
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hero.title,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    color = if (isUnlocked) RoyalIndigo else Charcoal.copy(alpha = 0.58f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isCompleted) {
                    Text(
                        text = if (isUnlocked) "Chronology ${hero.chronologicalOrder}" else "Locked",
                        fontSize = 12.sp,
                        color = if (isUnlocked) HeritageOchre else Charcoal.copy(alpha = 0.3f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier.width(64.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                when {
                    isCompleted -> CompletedStorySeal()
                    !isUnlocked -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RoyalIndigo.copy(alpha = 0.42f),
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = HeritageOchre,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedStorySeal(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .width(72.dp)
            .height(42.dp)
            .graphicsLayer { rotationZ = -7f },
        shape = RoundedCornerShape(999.dp),
        color = ParchmentLight.copy(alpha = 0.72f),
        border = BorderStroke(1.6.dp, RoyalIndigo.copy(alpha = 0.82f))
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .border(
                    width = 1.dp,
                    color = HeritageOchre.copy(alpha = 0.76f),
                    shape = RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CONQUERED",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = RoyalIndigo.copy(alpha = 0.92f),
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "DONE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    color = HeritageOchre,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
