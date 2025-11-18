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
import kotlinx.coroutines.flow.catch
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
        setupRealtimeUserListener()
    }
    
    /**
     * Set up real-time listener for current user changes
     * This ensures profile screens update automatically when user data changes (e.g., year level progression)
     */
    private fun setupRealtimeUserListener() {
        viewModelScope.launch {
            userRepository.getCurrentUserFlow()
                .catch { exception ->
                    android.util.Log.e("AuthViewModel", "Error in current user flow: ${exception.message}", exception)
                    // Fallback to one-time query on error
                    checkAuthState()
                }
                .collect { user ->
                    _currentUser.value = user
                    if (user != null) {
                        _uiState.value = AuthUiState(isSignedIn = true)
                    } else {
                        _uiState.value = AuthUiState(isSignedIn = false)
                    }
                }
        }
    }

    fun checkAuthState() {
        viewModelScope.launch {
            try {
                val result = userRepository.getCurrentUser()
                result.onSuccess { user ->
                    if (user != null) {
                        _currentUser.value = user
                        _uiState.value = AuthUiState(isSignedIn = true)
                    }
                }
            } catch (e: Exception) {
                // Handle error silently for initial check
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
                }.onFailure { exception ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = exception.message ?: "Sign in failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = e.message ?: "Sign in failed"
                )
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole,
        courseId: String? = null,
        yearLevelId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.createUser(email, password, firstName, lastName, role, courseId, yearLevelId)
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
    
    // ==================== ID-BASED AUTHENTICATION METHODS ====================
    
    /**
     * Sign in using Student ID or Teacher ID
     * @param userId Student ID or Teacher ID
     * @param password User's password
     * @param userType The role type (STUDENT or TEACHER)
     */
    fun signInWithId(userId: String, password: String, userType: UserRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.signInWithId(userId, password, userType)
                result.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState(isLoading = false, isSignedIn = true)
                }.onFailure { exception ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = exception.message ?: "Sign in failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = e.message ?: "Sign in failed"
                )
            }
        }
    }
    
    /**
     * Activate account using pre-registered institutional ID
     * @param userId Student ID or Teacher ID from pre-registration
     * @param password New password for the account
     * @param confirmPassword Password confirmation
     * @param userType The role type (STUDENT or TEACHER)
     */
    fun activateAccount(
        userId: String,
        password: String,
        confirmPassword: String,
        userType: UserRole
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.activateAccount(userId, password, confirmPassword, userType)
                result.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState(
                        isLoading = false, 
                        isSignUpSuccess = true,
                        isAccountActivated = true
                    )
                    println("DEBUG: Account activation successful for user ID: $userId")
                }.onFailure { exception ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = exception.message ?: "Account activation failed"
                    )
                    println("DEBUG: Account activation failed: ${exception.message}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = e.message ?: "Account activation failed"
                )
                println("DEBUG: Account activation exception: ${e.message}")
            }
        }
    }
    
    /**
     * Check if a user ID exists in pre-registration
     */
    fun checkUserIdExists(userId: String, userType: UserRole, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = userRepository.checkUserIdExists(userId, userType)
                result.onSuccess { exists ->
                    onResult(exists)
                }.onFailure {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    
    fun clearAccountActivatedFlag() {
        _uiState.value = _uiState.value.copy(isAccountActivated = false)
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val isSignUpSuccess: Boolean = false,
    val isAccountActivated: Boolean = false,  // For first-time account activation
    val error: String? = null
)
