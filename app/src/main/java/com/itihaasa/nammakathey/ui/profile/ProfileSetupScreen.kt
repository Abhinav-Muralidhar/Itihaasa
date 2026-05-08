package com.itihaasa.nammakathey.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@Composable
fun ProfileSetupScreen(
    onBackClick: () -> Unit = {},
    onSetupComplete: () -> Unit = {},
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RoyalIndigo
                )
            }
            Text(
                text = "Profile Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
        }

        Surface(
            color = RoyalIndigo,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.72f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Begin from your home district",
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentLight
                )
                Text(
                    text = "Your first story path opens from home. More districts unlock as your journey grows.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentLight.copy(alpha = 0.86f)
                )
            }
        }

        SetupPanel {
            Text(
                text = "Your Name",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Name shown on your profile") },
                colors = setupFieldColors()
            )
        }

        SetupPanel {
            Text(
                text = "Home District",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.districts.forEach { district ->
                    DistrictChoice(
                        district = district,
                        selected = district == uiState.homeDistrict,
                        onClick = { viewModel.onDistrictSelected(district) }
                    )
                }
            }
        }

        SetupPanel {
            Text(
                text = "Story Language",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LanguageChoice(
                    label = "English",
                    selected = uiState.preferredLang == "en",
                    onClick = { viewModel.onLanguageSelected("en") },
                    modifier = Modifier.weight(1f)
                )
                LanguageChoice(
                    label = "Kannada",
                    selected = uiState.preferredLang == "kn",
                    onClick = { viewModel.onLanguageSelected("kn") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = { viewModel.save(onSetupComplete) },
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoyalIndigo,
                contentColor = ParchmentLight
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ParchmentLight
                )
            } else {
                Text("Open Story Mode")
            }
        }
    }
}

@Composable
private fun SetupPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, HeritageOchre.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DistrictChoice(
    district: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RoyalIndigo.copy(alpha = 0.08f) else Parchment)
            .border(
                width = 1.dp,
                color = if (selected) HeritageOchre else RoyalIndigo.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = district,
            color = Charcoal,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(HeritageOchre),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ParchmentLight,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) RoyalIndigo else Parchment,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) RoyalIndigo else HeritageOchre.copy(alpha = 0.42f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 12.dp),
            color = if (selected) ParchmentLight else RoyalIndigo,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun setupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HeritageOchre,
    unfocusedBorderColor = RoyalIndigo.copy(alpha = 0.28f),
    focusedTextColor = Charcoal,
    unfocusedTextColor = Charcoal,
    cursorColor = HeritageOchre,
    focusedContainerColor = Parchment,
    unfocusedContainerColor = Parchment
)
