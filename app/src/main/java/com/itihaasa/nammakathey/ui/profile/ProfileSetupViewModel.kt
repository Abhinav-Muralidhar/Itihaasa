package com.itihaasa.nammakathey.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.itihaasa.nammakathey.data.local.DistrictDataSource
import com.itihaasa.nammakathey.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    districtDataSource: DistrictDataSource,
    private val profileRepository: ProfileRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val districtNames = districtDataSource.districts
        .map { it.name }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    private val _uiState = MutableStateFlow(
        ProfileSetupUiState(
            name = firebaseAuth.currentUser?.displayName.orEmpty(),
            districts = districtNames,
            homeDistrict = districtNames.firstOrNull().orEmpty()
        )
    )
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onDistrictSelected(value: String) {
        _uiState.update { it.copy(homeDistrict = value, errorMessage = null) }
    }

    fun onLanguageSelected(value: String) {
        _uiState.update { it.copy(preferredLang = value, errorMessage = null) }
    }

    fun save(onComplete: () -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        val homeDistrict = state.homeDistrict.trim()

        if (name.length < 2) {
            _uiState.update { it.copy(errorMessage = "Enter your name to continue.") }
            return
        }
        if (homeDistrict.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Choose your home district.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                profileRepository.completeProfileSetup(
                    name = name,
                    homeDistrict = homeDistrict,
                    preferredLang = state.preferredLang
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onComplete()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "Could not save profile."
                    )
                }
            }
        }
    }

}

data class ProfileSetupUiState(
    val name: String = "",
    val homeDistrict: String = "",
    val preferredLang: String = "en",
    val districts: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
