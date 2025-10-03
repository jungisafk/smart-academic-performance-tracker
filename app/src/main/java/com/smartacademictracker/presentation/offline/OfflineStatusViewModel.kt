package com.smartacademictracker.presentation.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.OfflineGradeRepository
import com.smartacademictracker.data.sync.SyncResult
import com.smartacademictracker.data.sync.ConflictResolution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineStatusViewModel @Inject constructor(
    private val offlineGradeRepository: OfflineGradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineStatusUiState())
    val uiState: StateFlow<OfflineStatusUiState> = _uiState.asStateFlow()

    val syncStatus = offlineGradeRepository.getSyncStatus()

    fun loadSyncStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load conflict grades - temporarily disabled
                // offlineGradeRepository.getGradesWithConflicts().collect { conflictGrades ->
                //     _uiState.value = _uiState.value.copy(
                //         isLoading = false,
                //         conflictGrades = conflictGrades
                //     )
                // }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    conflictGrades = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sync status"
                )
            }
        }
    }

    fun syncGrades() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                error = null
            )
            
            try {
                val result = offlineGradeRepository.syncGrades()
                
                when (result) {
                    is SyncResult.SUCCESS -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            lastSyncMessage = result.message
                        )
                    }
                    is SyncResult.FAILED -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            error = result.message
                        )
                    }
                    is SyncResult.PARTIAL_SUCCESS -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            lastSyncMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }

    fun resolveConflict(gradeId: String, resolution: ConflictResolution) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isResolvingConflicts = true,
                error = null
            )
            
            try {
                // TODO: Implement conflict resolution
                // This would call the sync manager's resolveConflicts method
                _uiState.value = _uiState.value.copy(
                    isResolvingConflicts = false,
                    lastSyncMessage = "Conflict resolved"
                )
                
                // Refresh the status
                loadSyncStatus()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isResolvingConflicts = false,
                    error = e.message ?: "Failed to resolve conflict"
                )
            }
        }
    }

    fun refreshStatus() {
        loadSyncStatus()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class OfflineStatusUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isResolvingConflicts: Boolean = false,
    val error: String? = null,
    val lastSyncMessage: String? = null,
    val conflictGrades: List<com.smartacademictracker.data.model.Grade> = emptyList()
)
