package com.itihaasa.nammakathey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import com.itihaasa.nammakathey.ui.auth.AuthScreen
import com.itihaasa.nammakathey.ui.map.MapScreen
import com.itihaasa.nammakathey.ui.nearme.NearMeScreen
import com.itihaasa.nammakathey.ui.profile.ProfileScreen
import com.itihaasa.nammakathey.ui.profile.ProfileSetupScreen
import com.itihaasa.nammakathey.ui.story.StoryScreen
import com.itihaasa.nammakathey.ui.story.StoryTabScreen
import com.itihaasa.nammakathey.ui.district.DistrictScreen
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Stories : Screen("stories")
    data object NearMe : Screen("near_me?placeId={placeId}") {
        fun route(placeId: String? = null): String =
            if (placeId.isNullOrBlank()) "near_me?placeId=" else "near_me?placeId=$placeId"
    }
    data object Profile : Screen("profile")
    data object Auth : Screen("auth")
    data object ProfileSetup : Screen("profile_setup")
    data object District : Screen("district/{district}") {
        fun route(district: String): String = "district/${Uri.encode(district)}"
    }
    data object Story : Screen("story/{placeId}") {
        fun route(placeId: String): String = "story/$placeId"
    }
}

@Composable
fun NammaKatheyRoot() {
    val navController = rememberNavController()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar = currentRoute in setOf(
            Screen.Map.route,
            Screen.Stories.route,
            Screen.NearMe.route,
            Screen.Profile.route
        )

        if (!showSplash) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        AppBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Map.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Map.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Map.route) {
                        MapScreen(
                            onProfileClick = { navController.navigate(Screen.Profile.route) },
                            onPlaceClick = { placeId -> navController.navigate(Screen.Story.route(placeId)) },
                            onDistrictClick = { district -> navController.navigate(Screen.District.route(district)) }
                        )
                    }
                    composable(Screen.Stories.route) {
                        StoryTabScreen(
                            onStoryClick = { placeId ->
                                navController.navigate(Screen.Story.route(placeId))
                            }
                        )
                    }
                    composable(
                        route = Screen.NearMe.route,
                        arguments = listOf(
                            navArgument("placeId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId")
                        NearMeScreen(
                            heroPlaceId = placeId,
                            onOpenStory = { id -> navController.navigate(Screen.Story.route(id)) }
                        )
                    }
                    composable(
                        route = Screen.District.route,
                        arguments = listOf(navArgument("district") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val district = Uri.decode(backStackEntry.arguments?.getString("district").orEmpty())
                        DistrictScreen(
                            district = district,
                            onBackClick = { navController.popBackStack() },
                            onPlaceClick = { placeId -> navController.navigate(Screen.Story.route(placeId)) }
                        )
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            onBackClick = { navController.popBackStack() },
                            onAuthClick = { navController.navigate(Screen.Auth.route) },
                            onSetupClick = { navController.navigate(Screen.ProfileSetup.route) }
                        )
                    }
                    composable(Screen.Auth.route) {
                        AuthScreen(
                            onBackClick = { navController.popBackStack() },
                            onAuthComplete = { profileComplete ->
                                val destination = if (profileComplete) {
                                    Screen.Profile.route
                                } else {
                                    Screen.ProfileSetup.route
                                }
                                navController.navigate(destination) {
                                    popUpTo(Screen.Map.route) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(Screen.ProfileSetup.route) {
                        ProfileSetupScreen(
                            onBackClick = { navController.popBackStack() },
                            onSetupComplete = {
                                navController.navigate(Screen.Stories.route) {
                                    popUpTo(Screen.ProfileSetup.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.Story.route,
                        arguments = listOf(navArgument("placeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId").orEmpty()
                        StoryScreen(
                            placeId = placeId,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenNearMe = { heroPlaceId ->
                                navController.navigate(Screen.NearMe.route(heroPlaceId))
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSplash, exit = fadeOut()) {
            StandardIntroScreen(onComplete = { showSplash = false })
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        selectedTextColor = RoyalIndigo,
        indicatorColor = HeritageOchre,
        unselectedIconColor = RoyalIndigo.copy(alpha = 0.62f),
        unselectedTextColor = RoyalIndigo.copy(alpha = 0.74f)
    )
    NavigationBar(
        containerColor = ParchmentLight,
        contentColor = RoyalIndigo,
        tonalElevation = 2.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Map.route,
            onClick = { onNavigate(Screen.Map.route) },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Stories.route,
            onClick = { onNavigate(Screen.Stories.route) },
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.NearMe.route,
            onClick = { onNavigate(Screen.NearMe.route()) },
            icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            colors = itemColors
        )
    }
}

@Composable
private fun StandardIntroScreen(onComplete: () -> Unit) {
    var wordVisible by rememberSaveable { mutableStateOf(true) }
    var letterPhase by rememberSaveable { mutableStateOf(0) }
    var taglineVisible by rememberSaveable { mutableStateOf(false) }
    val letterVariants = "itihaasa".map { it.toString() }
    val transientVariants = listOf(
        "\u0C87",
        "\u0BA4",
        "\u0C07",
        "\u0D39",
        "\u0905",
        "\u0985",
        "\u0AB8",
        "\u0B05"
    )

    LaunchedEffect(Unit) {
        delay(360)
        repeat(letterVariants.size) { index ->
            letterPhase = index + 1
            delay(210)
        }
        taglineVisible = true
        delay(950)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = wordVisible,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.72f)) + fadeIn()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    letterVariants.forEachIndexed { index, char ->
                        AnimatedContent(
                            targetState = if (index < letterPhase) char else transientVariants[index],
                            transitionSpec = {
                                (fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.78f))) togetherWith fadeOut()
                            },
                            label = "standard-intro-letter-$index"
                        ) { value ->
                            StandardLogoLetter(
                                value = value,
                                showDot = char == "i"
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = taglineVisible, enter = fadeIn()) {
                Text(
                    text = "\u0CA8\u0CAE\u0CCD\u0CAE \u0C95\u0CA5\u0CC6",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    color = HeritageOchre,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StandardLogoLetter(
    value: String,
    showDot: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(13.dp)
                .width(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showDot && value == "i") {
                FlameDot()
            }
        }
        Text(
            text = if (value == "i") "\u0131" else value,
            fontFamily = FontFamily.Serif,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalIndigo
        )
        Box(
            modifier = Modifier
                .height(5.dp)
                .width(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value == "a") {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(15.dp)
                        .background(HeritageOchre, RoundedCornerShape(999.dp))
                )
            }
        }
    }
}

@Composable
private fun FlameDot() {
    Canvas(modifier = Modifier.size(10.dp)) {
        val w = size.width
        val h = size.height
        val flame = Path().apply {
            moveTo(w * 0.5f, 0f)
            quadraticBezierTo(w, h * 0.38f, w * 0.5f, h)
            quadraticBezierTo(0f, h * 0.38f, w * 0.5f, 0f)
            close()
        }
        drawPath(flame, HeritageOchre)
    }
}

/*
@Composable
fun IntroScreen(onComplete: () -> Unit) {
    var markVisible by rememberSaveable { mutableStateOf(false) }
    var wordVisible by rememberSaveable { mutableStateOf(false) }
    var letterPhase by rememberSaveable { mutableStateOf(-1) }
    var taglineVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        markVisible = true
        delay(400)
        wordVisible = true
        delay(400)
        repeat(8) { index ->
            letterPhase = index
            delay(150)
            letterPhase = -1
            delay(60)
        }
        taglineVisible = true
        delay(2500L - 800L - 8L * 210L)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(
                visible = markVisible,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.55f)) + fadeIn()
            ) {
                Text(
                    text = "ಇ",
                    fontFamily = FontFamily.Serif,
                    fontSize = 0.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent
                )
            }
            AnimatedVisibility(visible = wordVisible, enter = fadeIn()) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    val latin = "itihaasa"
                    val displayScripts = listOf(
                        "\u0C87",
                        "\u0924",
                        "\u0987",
                        "\u0C39",
                        "\u0A05",
                        "\u0B85",
                        "\u09B8",
                        "\u0C85"
                    )
                    latin.forEachIndexed { index, char ->
                        AnimatedContent(
                            targetState = if (letterPhase == index) displayScripts[index] else char.toString(),
                            label = "intro-letter-$index"
                        ) { value ->
                            LogoLetter(value = value, latin = char)
                        }
                    }
                }
            }
            AnimatedVisibility(visible = taglineVisible, enter = fadeIn()) {
                Text(
                    text = "karnataka · heritage · discovery",
                    fontSize = 12.sp,
                    color = HeritageOchre,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
*/

@Composable
private fun LogoLetter(
    value: String,
    latin: Char
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(8.dp)
                .width(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (latin == 'i') {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(HeritageOchre)
                )
            }
        }
        Text(
            text = value,
            fontFamily = FontFamily.Serif,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalIndigo
        )
        Box(
            modifier = Modifier
                .height(5.dp)
                .width(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (latin == 'a') {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(15.dp)
                        .background(HeritageOchre, RoundedCornerShape(999.dp))
                )
            }
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
