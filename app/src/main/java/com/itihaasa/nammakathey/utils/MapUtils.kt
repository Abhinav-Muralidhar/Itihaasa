package com.itihaasa.nammakathey.utils

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.itihaasa.nammakathey.model.PlaceType

fun pinIconForType(type: PlaceType): BitmapDescriptor {
    val hue = when (type) {
        PlaceType.FORT -> BitmapDescriptorFactory.HUE_AZURE
        PlaceType.TEMPLE -> BitmapDescriptorFactory.HUE_GREEN
        PlaceType.HERO_SITE -> BitmapDescriptorFactory.HUE_ORANGE
        PlaceType.BATTLEFIELD -> BitmapDescriptorFactory.HUE_RED
        PlaceType.REFORM_SITE -> BitmapDescriptorFactory.HUE_VIOLET
    }
    return BitmapDescriptorFactory.defaultMarker(hue)
}

fun pinColorForType(type: PlaceType): Color = when (type) {
    PlaceType.FORT -> Color(0xFF1976D2)
    PlaceType.TEMPLE -> Color(0xFF2E7D32)
    PlaceType.HERO_SITE -> Color(0xFFF57C00)
    PlaceType.BATTLEFIELD -> Color(0xFFC62828)
    PlaceType.REFORM_SITE -> Color(0xFF6A1B9A)
}
