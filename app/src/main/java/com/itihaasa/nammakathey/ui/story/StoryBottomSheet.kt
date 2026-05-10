package com.itihaasa.nammakathey.ui.story

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.PlaceType
import com.itihaasa.nammakathey.model.Story
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

private val MidGray = Color(0xFF777168)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryBottomSheet(
    place: Place,
    story: Story?,
    isDistrictUnlocked: Boolean,
    homeDistrict: String?,
    onReadStory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imageUrl = story?.imageUrl

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
            if (imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(RoyalIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = place.name,
                        modifier = Modifier.padding(18.dp),
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Parchment,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = place.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
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

                Text(
                    text = place.name,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = HeritageOchre
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HeritageOchre
                    )
                    Text(text = place.district, fontSize = 13.sp, color = Charcoal)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = HeritageOchre.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )

                Text(
                    text = story?.significance
                        ?.takeIf { it.isNotBlank() && story?.cacheType != "offline_fallback" }
                        ?: place.previewInfo(),
                    fontSize = 14.sp,
                    color = Charcoal,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                if (isDistrictUnlocked) {
                    Button(
                        onClick = onReadStory,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalIndigo,
                            contentColor = ParchmentLight
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Go to story")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RoyalIndigo.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = HeritageOchre,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = lockedDistrictMessage(place.district, homeDistrict),
                            fontSize = 13.sp,
                            color = Charcoal,
                            lineHeight = 18.sp
                        )
                    }
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
    homeDistrict: String?
): String {
    return if (homeDistrict.isNullOrBlank()) {
        "$district is locked. Choose your home district to begin story mode."
    } else {
        "$district is locked. Complete stories from $homeDistrict to unlock new districts."
    }
}
