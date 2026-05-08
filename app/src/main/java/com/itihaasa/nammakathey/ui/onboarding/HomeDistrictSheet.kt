package com.itihaasa.nammakathey.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun HomeDistrictSheet(
    onDistrictSelected: (String) -> Unit,
    onSkip: () -> Unit
) {
    var selectedDistrict by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalIndigo)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Parchment),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ಇ",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
        }
        Text(
            text = "itihaasa",
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Parchment
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Where is your home?",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Parchment,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your heritage journey begins at home.",
            fontSize = 14.sp,
            color = HeritageOchre,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            KarnatakaDistricts.forEach { district ->
                val selected = district == selectedDistrict
                FilterChip(
                    selected = selected,
                    onClick = { selectedDistrict = district },
                    label = {
                        Text(
                            text = district,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ParchmentLight,
                        labelColor = RoyalIndigo,
                        selectedContainerColor = HeritageOchre,
                        selectedLabelColor = Parchment
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        selectedDistrict?.let { district ->
            Button(
                onClick = { onDistrictSelected(district) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HeritageOchre,
                    contentColor = Parchment
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Begin my journey")
            }
        }
        TextButton(onClick = onSkip) {
            Text(
                text = "Skip for now",
                color = Parchment.copy(alpha = 0.5f)
            )
        }
    }
}

val KarnatakaDistricts = listOf(
    "Bagalkot",
    "Ballari",
    "Belagavi",
    "Bengaluru Rural",
    "Bengaluru Urban",
    "Bidar",
    "Chamarajanagar",
    "Chikkaballapur",
    "Chikkamagaluru",
    "Chitradurga",
    "Dakshina Kannada",
    "Davanagere",
    "Dharwad",
    "Gadag",
    "Hassan",
    "Haveri",
    "Kalaburagi",
    "Kodagu",
    "Kolar",
    "Koppal",
    "Mandya",
    "Mysuru",
    "Raichur",
    "Ramanagara",
    "Shivamogga",
    "Tumakuru",
    "Udupi",
    "Uttara Kannada",
    "Vijayapura",
    "Yadgir"
)
