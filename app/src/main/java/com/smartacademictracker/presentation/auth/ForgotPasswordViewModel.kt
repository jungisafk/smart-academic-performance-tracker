package com.smartacademictracker.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun sendPasswordReset(identifier: String, userType: UserRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            
            if (identifier.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Please enter your email address or ID"
                )
                return@launch
            }
            
            android.util.Log.d("ForgotPasswordViewModel", "Sending password reset for identifier: $identifier, userType: $userType")
            
            val result = userRepository.sendPasswordResetEmail(identifier.trim(), userType)
            result.onSuccess {
                android.util.Log.d("ForgotPasswordViewModel", "Password reset email sent successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Password reset email sent! Please check your email inbox (and spam folder) for instructions.",
                    error = null
                )
            }.onFailure { exception ->
                android.util.Log.e("ForgotPasswordViewModel", "Failed to send password reset email: ${exception.message}", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to send password reset email",
                    successMessage = null
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

