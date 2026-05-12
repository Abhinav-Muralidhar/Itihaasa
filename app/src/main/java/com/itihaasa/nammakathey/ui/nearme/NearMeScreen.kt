package com.itihaasa.nammakathey.ui.nearme

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import kotlin.math.roundToInt

@Composable
fun NearMeScreen(
    heroPlaceId: String? = null,
    viewModel: NearMeViewModel = hiltViewModel(),
    onOpenStory: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPermissionGranted(granted)
        if (granted) viewModel.refresh()
    }

    LaunchedEffect(heroPlaceId) {
        viewModel.setHero(heroPlaceId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.heroTitle?.let { "Near ${it}" } ?: "Near Me",
                            fontFamily = FontFamily.Serif,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalIndigo,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Places within ${uiState.radiusKm.roundToInt()} km",
                            fontSize = 13.sp,
                            color = HeritageOchre
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RoyalIndigo)
                    }
                }
            }

            if (!uiState.permissionGranted) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ParchmentLight,
                        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Allow location to see nearby heritage places.",
                                color = Charcoal,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                                colors = ButtonDefaults.buttonColors(containerColor = HeritageOchre),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Parchment)
                                Spacer(modifier = Modifier.height(0.dp))
                                Text("Enable location", color = Parchment, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ParchmentLight,
                        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Radius",
                                color = Charcoal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5.0, 10.0, 15.0).forEach { km ->
                                    val selected = uiState.radiusKm == km
                                    Surface(
                                        modifier = Modifier
                                            .clickable { viewModel.setRadiusKm(km) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (selected) HeritageOchre else Parchment,
                                        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                                    ) {
                                        Text(
                                            text = "${km.roundToInt()}km",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            color = if (selected) Parchment else RoyalIndigo,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ParchmentLight,
                            border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                modifier = Modifier.padding(14.dp),
                                color = Charcoal.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (!uiState.isLoading && uiState.nearby.isEmpty() && uiState.errorMessage == null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ParchmentLight,
                            border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = "No places found in this radius. Try increasing it.",
                                modifier = Modifier.padding(14.dp),
                                color = Charcoal.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    items(uiState.nearby) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ParchmentLight,
                            border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val uri = Uri.parse("geo:${item.lat},${item.lng}?q=${Uri.encode(item.title)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalIndigo,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${"%.1f".format(item.distanceKm)} km",
                                        fontSize = 12.sp,
                                        color = HeritageOchre
                                    )
                                }
                                Text(
                                    text = item.subtitle,
                                    fontSize = 13.sp,
                                    color = Charcoal.copy(alpha = 0.78f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.permissionGranted && uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = HeritageOchre
            )
        }
    }
}

