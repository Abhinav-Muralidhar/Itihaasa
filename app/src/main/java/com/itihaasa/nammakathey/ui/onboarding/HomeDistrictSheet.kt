package com.itihaasa.nammakathey.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDistrictSheet(
    onDistrictSelected: (String) -> Unit,
    onSkip: () -> Unit
) {
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalIndigo)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        OnboardingLogo()
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Where should your story begin?",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Parchment,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose any Karnataka district as your home base.",
            fontSize = 14.sp,
            color = HeritageOchre,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDistrict.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                placeholder = { Text("Select district") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ParchmentLight,
                    unfocusedContainerColor = ParchmentLight,
                    focusedBorderColor = HeritageOchre,
                    unfocusedBorderColor = Parchment.copy(alpha = 0.68f),
                    focusedTextColor = RoyalIndigo,
                    unfocusedTextColor = RoyalIndigo,
                    focusedPlaceholderColor = RoyalIndigo.copy(alpha = 0.55f),
                    unfocusedPlaceholderColor = RoyalIndigo.copy(alpha = 0.55f)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(ParchmentLight)
            ) {
                KarnatakaDistricts.forEach { district ->
                    DropdownMenuItem(
                        text = { Text(district, color = RoyalIndigo) },
                        onClick = {
                            selectedDistrict = district
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { selectedDistrict?.let(onDistrictSelected) },
            enabled = selectedDistrict != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = HeritageOchre,
                contentColor = Parchment,
                disabledContainerColor = HeritageOchre.copy(alpha = 0.34f),
                disabledContentColor = Parchment.copy(alpha = 0.72f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Begin my journey")
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onSkip) {
            Text(
                text = "Skip for now",
                color = Parchment.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OnboardingLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.Center) {
            "itihaasa".forEach { char ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(width = 18.dp, height = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (char == 'i') {
                            Box(
                                modifier = Modifier
                                    .size(width = 7.dp, height = 9.dp)
                                    .background(HeritageOchre, RoundedCornerShape(999.dp))
                            )
                        }
                    }
                    Text(
                        text = if (char == 'i') "\u0131" else char.toString(),
                        fontFamily = FontFamily.Serif,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Parchment
                    )
                }
            }
        }
        Text(
            text = "\u0CA8\u0CAE\u0CCD\u0CAE \u0C95\u0CA5\u0CC6",
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp,
            color = HeritageOchre,
            textAlign = TextAlign.Center
        )
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
    "Vijayanagara",
    "Vijayapura",
    "Yadgir"
)
