package com.itihaasa.nammakathey.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.ParchmentLight

data class HeritageBadgeVisual(
    val title: String,
    val subtitle: String,
    val typeLabel: String,
    val districtCode: String,
    val primary: Color,
    val secondary: Color,
    val surface: Color = ParchmentLight,
    val emblemText: String = "",
    val style: HeritageBadgeStyle = HeritageBadgeStyle.Hero
)

enum class HeritageBadgeStyle {
    Hero,
    District,
    Rank
}

@Composable
fun HeritageBadge(
    visual: HeritageBadgeVisual,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    labelVisible: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val outerShape = visual.badgeShape()
    val hasBottomRibbon = visual.style != HeritageBadgeStyle.District
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(size)
                .height(size * 1.14f)
                .border(
                    width = if (visual.style == HeritageBadgeStyle.Rank) 2.dp else 1.dp,
                    color = visual.secondary.copy(alpha = 0.72f),
                    shape = outerShape
                ),
            shape = outerShape,
            color = visual.primary,
            border = BorderStroke(1.dp, visual.surface.copy(alpha = 0.20f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .width(size * 0.82f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(visual.secondary.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = visual.districtCode.ifBlank { visual.typeLabel.take(3).uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = visual.surface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(size * 0.66f)
                        .clip(CircleShape)
                        .background(visual.surface.copy(alpha = 0.15f))
                        .border(1.dp, visual.surface.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.52f)
                            .clip(CircleShape)
                            .border(1.dp, visual.secondary.copy(alpha = 0.78f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (visual.style == HeritageBadgeStyle.District) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                OrnamentLine(color = visual.secondary, width = size * 0.24f)
                                Text(
                                    text = "ಕರ್ನಾಟಕ",
                                    color = visual.surface,
                                    fontSize = when {
                                        size < 60.dp -> 6.sp
                                        size < 90.dp -> 8.sp
                                        else -> 10.sp
                                    },
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                OrnamentLine(color = visual.secondary, width = size * 0.24f)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                OrnamentLine(color = visual.secondary, width = size * 0.26f)
                                Text(
                                    text = visual.emblemText.ifBlank { visual.typeLabel.uppercase() },
                                    color = visual.surface,
                                    fontSize = when {
                                        size < 60.dp -> 7.sp
                                        size < 90.dp -> 9.sp
                                        else -> 12.sp
                                    },
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                OrnamentLine(color = visual.secondary, width = size * 0.26f)
                            }
                        }
                    }
                }

                if (hasBottomRibbon) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 9.dp)
                            .width(size * 0.82f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(visual.secondary.copy(alpha = 0.86f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = visual.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 4.dp),
                            color = visual.surface,
                            fontSize = when {
                                size < 60.dp -> 6.sp
                                size < 90.dp -> 8.sp
                                else -> 10.sp
                            },
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = when {
                                size < 60.dp -> 7.sp
                                size < 90.dp -> 9.sp
                                else -> 11.sp
                            }
                        )
                    }
                }
            }
        }

        if (labelVisible) {
            Text(
                text = visual.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Charcoal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (visual.subtitle.isNotBlank()) {
                Text(
                    text = visual.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Charcoal.copy(alpha = 0.68f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun OrnamentLine(
    color: Color,
    width: Dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(width * 0.28f)
                .height(1.dp)
                .background(color.copy(alpha = 0.78f))
        )
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.92f))
        )
        Box(
            modifier = Modifier
                .width(width * 0.28f)
                .height(1.dp)
                .background(color.copy(alpha = 0.78f))
        )
    }
}

private fun HeritageBadgeVisual.badgeShape(): Shape = when (style) {
    HeritageBadgeStyle.Hero -> RoundedCornerShape(
        topStart = 34.dp,
        topEnd = 34.dp,
        bottomStart = 14.dp,
        bottomEnd = 14.dp
    )
    HeritageBadgeStyle.District -> RoundedCornerShape(18.dp)
    HeritageBadgeStyle.Rank -> CircleShape
}
