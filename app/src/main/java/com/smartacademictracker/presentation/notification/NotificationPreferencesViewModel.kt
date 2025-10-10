package com.smartacademictracker.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.NotificationPreferences
import com.smartacademictracker.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = notificationRepository.getNotificationPreferences(getCurrentUserId())
                result.fold(
                    onSuccess = { prefs ->
                        _preferences.value = prefs
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load preferences"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun savePreferences() {
        viewModelScope.launch {
            try {
                val result = notificationRepository.updateNotificationPreferences(_preferences.value)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            showSuccessMessage = true,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to save preferences"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun updateEmailNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(emailNotifications = enabled)
    }

    fun updatePushNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(pushNotifications = enabled)
    }

    fun updateInAppNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(inAppNotifications = enabled)
    }

    fun updateGradeUpdateNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(gradeUpdateNotifications = enabled)
    }

    fun updateApplicationStatusNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(applicationStatusNotifications = enabled)
    }

    fun updateDeadlineReminderNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(deadlineReminderNotifications = enabled)
    }

    fun updateSystemAnnouncementNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(systemAnnouncementNotifications = enabled)
    }

    fun updatePerformanceAlertNotifications(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(performanceAlertNotifications = enabled)
    }

    fun updateQuietHoursStart(startTime: String) {
        _preferences.value = _preferences.value.copy(quietHoursStart = startTime.ifEmpty { null })
    }

    fun updateQuietHoursEnd(endTime: String) {
        _preferences.value = _preferences.value.copy(quietHoursEnd = endTime.ifEmpty { null })
    }

    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }

    private fun getCurrentUserId(): String {
        // This should be replaced with actual user ID from authentication
        // For now, return a placeholder
        return "current_user_id"
    }
}

data class NotificationPreferencesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSuccessMessage: Boolean = false
)
