package com.itihaasa.nammakathey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itihaasa.nammakathey.ui.map.MapScreen
import com.itihaasa.nammakathey.ui.profile.ProfileScreen
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Profile : Screen("profile")
}

@Composable
fun NammaKatheyRoot() {
    val navController = rememberNavController()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Map.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Map.route) {
                    MapScreen(
                        onProfileClick = { navController.navigate(Screen.Profile.route) }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut()
        ) {
            NammaKatheySplash()
        }
    }
}

@Composable
private fun NammaKatheySplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NammaKatheyLogo()
            Text(
                text = "Itihaasa",
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Namma Kathey",
                fontSize = 13.sp,
                color = Charcoal.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HeritageOchre)
            )
        }
    }
}

@Composable
private fun NammaKatheyLogo() {
    Box(
        modifier = Modifier
            .size(126.dp)
            .clip(CircleShape)
            .background(RoyalIndigo),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(104.dp)) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = HeritageOchre,
                radius = w * 0.16f,
                center = Offset(w * 0.5f, h * 0.24f)
            )
            drawRoundRect(
                color = ParchmentLight,
                topLeft = Offset(w * 0.12f, h * 0.42f),
                size = Size(w * 0.76f, h * 0.36f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawLine(
                color = HeritageOchre,
                start = Offset(w * 0.5f, h * 0.42f),
                end = Offset(w * 0.5f, h * 0.78f),
                strokeWidth = 3.dp.toPx()
            )
            repeat(3) { index ->
                val y = h * (0.49f + index * 0.08f)
                drawLine(
                    color = Charcoal.copy(alpha = 0.28f),
                    start = Offset(w * 0.2f, y),
                    end = Offset(w * 0.43f, y - 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Charcoal.copy(alpha = 0.28f),
                    start = Offset(w * 0.57f, y - 4.dp.toPx()),
                    end = Offset(w * 0.8f, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
            drawArc(
                color = HeritageOchre,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(w * 0.2f, h * 0.14f),
                size = Size(w * 0.6f, h * 0.42f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawLine(
                color = Color(0xFF8C4D3F),
                start = Offset(w * 0.18f, h * 0.83f),
                end = Offset(w * 0.82f, h * 0.83f),
                strokeWidth = 6.dp.toPx()
            )
        }
    }
}
