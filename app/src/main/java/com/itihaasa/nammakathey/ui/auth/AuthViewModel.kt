package com.itihaasa.nammakathey.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itihaasa.nammakathey.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun signInWithGoogle(idToken: String, onSuccess: (Boolean) -> Unit) {
        submit(onSuccess) { profileRepository.signInWithGoogle(idToken) }
    }

    fun submitEmail(onSuccess: (Boolean) -> Unit) {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        if (email.isBlank() || password.length < 6) {
            _uiState.update {
                it.copy(errorMessage = "Enter a valid email and a password with at least 6 characters.")
            }
            return
        }
        if (state.mode == AuthMode.SignUp && state.name.trim().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your name to create your profile.") }
            return
        }

        submit(onSuccess) {
            if (state.mode == AuthMode.SignUp) {
                profileRepository.signUpWithEmail(state.name, email, password)
            } else {
                profileRepository.signInWithEmail(email, password)
            }
        }
    }

    private fun submit(
        onSuccess: (Boolean) -> Unit,
        block: suspend () -> Boolean
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { profileComplete ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    onSuccess(profileComplete)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Authentication failed."
                        )
                    }
                }
        }
    }
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class AuthMode {
    SignIn,
    SignUp
}
