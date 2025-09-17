package com.smartacademictracker.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val result = userRepository.getCurrentUser()
                result.onSuccess { user ->
                    if (user != null) {
                        _currentUser.value = user
                        _uiState.value = AuthUiState(isSignedIn = true)
                        println("DEBUG: User already signed in: ${user.email}")
                    }
                }
            } catch (e: Exception) {
                // Handle error silently for initial check
                println("DEBUG: No existing user session")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.signIn(email, password)
                result.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState(isLoading = false, isSignedIn = true)
                    println("DEBUG: Sign in successful for user: ${user.email}")
                    println("DEBUG: Current user set: ${_currentUser.value?.email}, isSignedIn: ${_uiState.value.isSignedIn}")
                }.onFailure { exception ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = exception.message ?: "Sign in failed"
                    )
                    println("DEBUG: Sign in failed: ${exception.message}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = e.message ?: "Sign in failed"
                )
                println("DEBUG: Sign in exception: ${e.message}")
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.createUser(email, password, firstName, lastName, role)
                result.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState(isLoading = false, isSignUpSuccess = true)
                }.onFailure { exception ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = exception.message ?: "Sign up failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = e.message ?: "Sign up failed"
                )
            }
        }
    }

    fun signOut() {
        userRepository.signOut()
        _currentUser.value = null
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSignUpSuccess() {
        _uiState.value = _uiState.value.copy(isSignUpSuccess = false)
    }

    fun clearSignInSuccess() {
        _uiState.value = _uiState.value.copy(isSignedIn = false)
    }

    fun isUserSignedIn(): Boolean {
        return userRepository.isUserSignedIn()
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val isSignUpSuccess: Boolean = false,
    val error: String? = null
)
