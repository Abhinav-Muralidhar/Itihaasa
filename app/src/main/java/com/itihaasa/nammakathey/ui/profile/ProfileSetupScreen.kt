package com.itihaasa.nammakathey.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
            border = BorderStroke(1.dp, RoyalIndigo)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Set your starting district",
                    fontFamily = FontFamily.Serif,
                    fontSize = 25.sp,
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

        SetupPanel(title = "Your Name") {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Name shown on your profile") },
                colors = setupFieldColors()
            )
        }

        SetupPanel(title = "Home District") {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.homeDistrict,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("Choose district") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = setupFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.districts.forEach { district ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = district,
                                    color = Charcoal,
                                    fontWeight = if (district == uiState.homeDistrict) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    }
                                )
                            },
                            onClick = {
                                viewModel.onDistrictSelected(district)
                                expanded = false
                            },
                            trailingIcon = if (district == uiState.homeDistrict) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = HeritageOchre,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

        SetupPanel(title = "Story Language") {
            CompactLanguageRow(
                selected = uiState.preferredLang,
                onLanguageSelected = viewModel::onLanguageSelected
            )
        }

        Surface(
            color = ParchmentLight.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.12f))
        ) {
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
private fun SetupPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = ParchmentLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
            content()
        }
    }
}

@Composable
private fun CompactLanguageRow(
    selected: String,
    onLanguageSelected: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LanguageChoice(
            label = "English",
            selected = selected == "en",
            onClick = { onLanguageSelected("en") },
            modifier = Modifier.weight(1f)
        )
        LanguageChoice(
            label = "Kannada",
            selected = selected == "kn",
            onClick = { onLanguageSelected("kn") },
            modifier = Modifier.weight(1f)
        )
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
        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = if (selected) 1f else 0.32f))
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
    focusedBorderColor = RoyalIndigo,
    unfocusedBorderColor = RoyalIndigo.copy(alpha = 0.32f),
    focusedTextColor = Charcoal,
    unfocusedTextColor = Charcoal,
    cursorColor = HeritageOchre,
    focusedContainerColor = Parchment,
    unfocusedContainerColor = Parchment
)
