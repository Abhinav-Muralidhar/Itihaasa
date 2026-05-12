package com.itihaasa.nammakathey.ui.district

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@Composable
fun DistrictScreen(
    district: String,
    exploredPlaceIds: Set<String> = emptySet(),
    lockedPlaceIds: Set<String> = emptySet(),
    viewModel: DistrictViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onPlaceClick: (String) -> Unit
) {
    val places by viewModel.places.collectAsState()
    val centuries = places.map { it.era }.filter { it.isNotBlank() }.distinct().size
    val compact = LocalConfiguration.current.screenHeightDp < 700

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = district,
                        fontFamily = FontFamily.Serif,
                        fontSize = if (compact) 24.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalIndigo,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${places.size} heroes across $centuries centuries",
                        fontSize = 14.sp,
                        color = HeritageOchre
                    )
                }
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RoyalIndigo)
                }
            }
        }
        if (places.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = ParchmentLight,
                    border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.22f))
                ) {
                    Text(
                        text = "No stories are available in this district yet.",
                        modifier = Modifier.padding(16.dp),
                        color = Charcoal.copy(alpha = 0.72f)
                    )
                }
            }
        } else {
            items(places) { place ->
                TimelinePlaceCard(
                    place = place,
                    explored = place.id in exploredPlaceIds,
                    locked = place.id in lockedPlaceIds,
                    onClick = { if (place.id !in lockedPlaceIds) onPlaceClick(place.id) }
                )
            }
        }
    }
}

@Composable
private fun TimelinePlaceCard(
    place: Place,
    explored: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = place.era,
                fontSize = 10.sp,
                color = HeritageOchre,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        when {
                            explored -> HeritageOchre
                            locked -> Color.Gray
                            else -> RoyalIndigo
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (explored) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Parchment,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .width(1.dp)
                    .height(72.dp)
            ) {
                drawRect(RoyalIndigo.copy(alpha = 0.45f))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            color = if (locked) ParchmentLight.copy(alpha = 0.54f) else ParchmentLight,
            border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = if (locked) 0.12f else 0.22f))
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = place.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalIndigo
                    )
                    Text(
                        text = place.seedKeywords.take(3).joinToString(" Â· "),
                        fontSize = 13.sp,
                        color = Charcoal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            explored -> "Tap to re-open story"
                            locked -> "Locked"
                            else -> "Tap to open story"
                        },
                        fontSize = 12.sp,
                        color = if (locked) Charcoal.copy(alpha = 0.4f) else Charcoal.copy(alpha = 0.7f)
                    )
                }

                if (explored) {
                    CompletedStamp(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedStamp(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.graphicsLayer { rotationZ = -12f },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, Color(0xFF27AE60).copy(alpha = 0.85f))
    ) {
        Text(
            text = "COMPLETED",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF27AE60).copy(alpha = 0.9f),
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

