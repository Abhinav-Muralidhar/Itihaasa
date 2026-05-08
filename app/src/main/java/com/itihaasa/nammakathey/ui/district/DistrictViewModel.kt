package com.itihaasa.nammakathey.ui.district

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.itihaasa.nammakathey.data.local.LocationsDataSource
import com.itihaasa.nammakathey.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class DistrictViewModel @Inject constructor(
    locationsDataSource: LocationsDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val district = savedStateHandle.get<String>("district").orEmpty()
    private val _places = MutableStateFlow(locationsDataSource.getPlacesByDistrict(district))
    val places: StateFlow<List<Place>> = _places.asStateFlow()
}
