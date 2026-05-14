package com.itihaasa.nammakathey.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.PlaceType
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

private val MidGray = Color(0xFF777168)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDistrictSheet(
    place: Place,
    isDistrictUnlocked: Boolean,
    homeDistrict: String?,
    activeDistrict: String?,
    onExploreDistrict: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentDistrict = activeDistrict ?: homeDistrict

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Parchment
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(RoyalIndigo),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = place.name,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Parchment,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = HeritageOchre
                        )
                        Text(
                            text = place.district,
                            fontSize = 13.sp,
                            color = ParchmentLight.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(place.type.displayName()) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = HeritageOchre,
                            labelColor = Parchment
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = RoyalIndigo
                        )
                    )
                    Text(text = place.era, fontSize = 12.sp, color = MidGray)
                }



                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = HeritageOchre.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )

                Text(
                    text = place.previewInfo(),
                    fontSize = 14.sp,
                    color = Charcoal,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDistrictUnlocked) ParchmentLight else RoyalIndigo.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDistrictUnlocked) Icons.Filled.MenuBook else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = HeritageOchre,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isDistrictUnlocked) {
                                "Open the story journey for ${place.district}."
                            } else {
                                "${place.district} is locked for now."
                            },
                            fontSize = 13.sp,
                            color = Charcoal,
                            lineHeight = 18.sp
                        )
                    }
                }
                Button(
                    onClick = onExploreDistrict,
                    enabled = isDistrictUnlocked,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalIndigo,
                        contentColor = ParchmentLight,
                        disabledContainerColor = RoyalIndigo.copy(alpha = 0.16f),
                        disabledContentColor = RoyalIndigo.copy(alpha = 0.46f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Explore District Stories")
                }
                if (!isDistrictUnlocked) {
                    Text(
                        text = lockedDistrictMessage(place.district, currentDistrict),
                        fontSize = 12.sp,
                        color = RoyalIndigo.copy(alpha = 0.72f),
                        lineHeight = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun PlaceType.displayName(): String = when (this) {
    PlaceType.FORT -> "Fort"
    PlaceType.TEMPLE -> "Temple"
    PlaceType.HERO_SITE -> "Hero Site"
    PlaceType.BATTLEFIELD -> "Battlefield"
    PlaceType.REFORM_SITE -> "Reform Site"
}

private fun Place.previewInfo(): String {
    val figures = seedKeywords
        .take(3)
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
    return buildString {
        append(name)
        append(" is a ")
        append(type.displayName().lowercase())
        append(" in ")
        append(district)
        append(" from the ")
        append(era)
        append(" era.")
        if (figures != null) {
            append(" Connected with ")
            append(figures)
            append(".")
        }
    }
}

private fun lockedDistrictMessage(
    district: String,
    activeDistrict: String?
): String {
    return if (activeDistrict.isNullOrBlank()) {
        "$district is locked. Choose your home district to begin story mode."
    } else {
        "$district is locked. Complete the current unlocked district, $activeDistrict, to unlock more districts."
    }
}
