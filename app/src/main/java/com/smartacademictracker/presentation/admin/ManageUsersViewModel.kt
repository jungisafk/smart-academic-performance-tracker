package com.smartacademictracker.presentation.admin

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
class ManageUsersViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageUsersUiState())
    val uiState: StateFlow<ManageUsersUiState> = _uiState.asStateFlow()

    private val _users = MutableStateFlow<List<com.smartacademictracker.data.model.User>>(emptyList())
    val users: StateFlow<List<com.smartacademictracker.data.model.User>> = _users.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.getAllUsers()
                result.onSuccess { usersList ->
                    _users.value = usersList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    println("DEBUG: ManageUsersViewModel - Loaded ${usersList.size} users")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load users"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load users"
                )
            }
        }
    }

    fun updateUserStatus(userId: String, active: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingUsers = _uiState.value.processingUsers + userId,
                error = null
            )
            
            try {
                val result = userRepository.updateUserStatus(userId, active)
                result.onSuccess {
                    // Reload users to reflect changes
                    loadUsers()
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId,
                        error = exception.message ?: "Failed to update user status"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingUsers = _uiState.value.processingUsers - userId,
                    error = e.message ?: "Failed to update user status"
                )
            }
        }
    }

    fun updateUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingUsers = _uiState.value.processingUsers + userId,
                error = null
            )
            
            try {
                val result = userRepository.updateUserRole(userId, newRole)
                result.onSuccess {
                    // Reload users to reflect changes
                    loadUsers()
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId,
                        error = exception.message ?: "Failed to update user role"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingUsers = _uiState.value.processingUsers - userId,
                    error = e.message ?: "Failed to update user role"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshUsers() {
        loadUsers()
    }
}

data class ManageUsersUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingUsers: Set<String> = emptySet()
)
