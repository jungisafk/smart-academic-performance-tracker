package com.smartacademictracker.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load additional profile data if needed
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }
    
    fun updateProfile(
        userId: String,
        firstName: String,
        lastName: String,
        middleName: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.updateUserProfile(userId, firstName, lastName, middleName)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = exception.message ?: "Failed to update profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }
    
    fun changePassword(
        currentPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.changePassword(currentPassword, newPassword)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = exception.message ?: "Failed to change password"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = e.message ?: "Failed to change password"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
