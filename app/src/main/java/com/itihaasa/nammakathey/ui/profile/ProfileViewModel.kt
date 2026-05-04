package com.itihaasa.nammakathey.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load profile."
                        )
                    }
                }
                .collect { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.update {
            it.copy(isSigningIn = true, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                profileRepository.signInWithGoogle(idToken)
            }.onSuccess {
                _uiState.update {
                    it.copy(isSigningIn = false, errorMessage = null)
                }
                observeProfile()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        errorMessage = throwable.message ?: "Google sign-in failed."
                    )
                }
            }
        }
    }

    fun signOut() {
        profileRepository.signOut()
        _uiState.update {
            it.copy(profile = null, isSigningIn = false, errorMessage = null)
        }
        observeProfile()
    }
}
