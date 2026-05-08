package com.itihaasa.nammakathey.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class RewardCardUiModel(
    val kind: RewardKind,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val icon: ImageVector,
    val statusText: String,
    val active: Boolean
)

enum class RewardKind {
    QuizBadge,
    DistrictBadge,
    RankPlaque
}
