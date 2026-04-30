package com.itihaasa.nammakathey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.itihaasa.nammakathey.ui.theme.NammakatheyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NammakatheyTheme {
                EmptyMapScreen()
            }
        }
    }
}

@Composable
fun EmptyMapScreen(modifier: Modifier = Modifier) {
    val karnatakaCenter = LatLng(14.5204, 75.7224)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(karnatakaCenter, 6.2f)
    }

    GoogleMap(
        cameraPositionState = cameraPositionState,
        modifier = modifier.fillMaxSize()
    )
}

@Preview(showBackground = true)
@Composable
fun EmptyMapPreview() {
    NammakatheyTheme {
        EmptyMapScreen()
    }
}
