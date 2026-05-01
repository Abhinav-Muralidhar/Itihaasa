package com.itihaasa.nammakathey.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.itihaasa.nammakathey.model.Place
import com.itihaasa.nammakathey.model.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryBottomSheet(
    uiState: StoryUiState,
    onDismiss: () -> Unit
) {
    val place = uiState.place ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        when {
            uiState.isLoading -> StorySkeleton(place)
            uiState.story != null -> StoryContent(place = place, story = uiState.story)
            uiState.errorMessage != null -> StoryError(place = place, message = uiState.errorMessage)
            else -> StorySkeleton(place)
        }
    }
}

@Composable
private fun StoryContent(
    place: Place,
    story: Story
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StoryImage(place = place, story = story)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = story.heroName.ifBlank { place.name },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            AssistChip(
                onClick = {},
                label = { Text(place.type.name.replace('_', ' ')) }
            )
        }
        Text(
            text = story.era.ifBlank { place.era },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = story.storyText,
            style = MaterialTheme.typography.bodyLarge
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = story.significance,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun StoryImage(
    place: Place,
    story: Story
) {
    val imageUrl = story.imageUrl

    if (imageUrl.isNullOrBlank()) {
        PlaceholderImage(place)
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = place.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun PlaceholderImage(place: Place) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = place.era,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StorySkeleton(place: Place) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlaceholderImage(place)
        SkeletonBar(widthFraction = 0.72f, height = 28.dp)
        SkeletonBar(widthFraction = 0.36f, height = 18.dp)
        repeat(5) {
            SkeletonBar(widthFraction = if (it == 4) 0.64f else 1f, height = 16.dp)
        }
    }
}

@Composable
private fun SkeletonBar(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun StoryError(
    place: Place,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlaceholderImage(place)
        Text(
            text = place.name,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
