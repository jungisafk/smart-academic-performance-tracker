package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.GradeComment
import com.smartacademictracker.data.model.CommentType
import com.smartacademictracker.data.repository.GradeCommentRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherGradeCommentViewModel @Inject constructor(
    private val commentRepository: GradeCommentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherGradeCommentUiState())
    val uiState: StateFlow<TeacherGradeCommentUiState> = _uiState.asStateFlow()

    fun loadComments(gradeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = commentRepository.getCommentsByGrade(gradeId)
                result.onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        comments = comments,
                        currentGradeId = gradeId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load comments"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load comments"
                )
            }
        }
    }

    fun addComment(
        gradeId: String,
        studentId: String,
        studentName: String,
        subjectId: String,
        subjectName: String,
        comment: String,
        commentType: CommentType,
        isPrivate: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    val newComment = GradeComment(
                        gradeId = gradeId,
                        studentId = studentId,
                        studentName = studentName,
                        teacherId = currentUser.id,
                        teacherName = "${currentUser.firstName} ${currentUser.lastName}",
                        subjectId = subjectId,
                        subjectName = subjectName,
                        comment = comment,
                        commentType = commentType,
                        isPrivate = isPrivate
                    )
                    
                    val result = commentRepository.createComment(newComment)
                    result.onSuccess {
                        // Reload comments to show the new one
                        loadComments(gradeId)
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to add comment"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get current user"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add comment"
                )
            }
        }
    }

    fun updateComment(commentId: String, updatedComment: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = commentRepository.updateComment(commentId, updatedComment)
                result.onSuccess {
                    // Reload comments to show the updated one
                    val currentGradeId = _uiState.value.currentGradeId
                    if (currentGradeId.isNotEmpty()) {
                        loadComments(currentGradeId)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to update comment"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update comment"
                )
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = commentRepository.deleteComment(commentId)
                result.onSuccess {
                    // Reload comments to reflect the deletion
                    val currentGradeId = _uiState.value.currentGradeId
                    if (currentGradeId.isNotEmpty()) {
                        loadComments(currentGradeId)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to delete comment"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete comment"
                )
            }
        }
    }

    fun filterCommentsByType(commentType: CommentType?) {
        val currentComments = _uiState.value.allComments
        val filteredComments = if (commentType != null) {
            currentComments.filter { it.commentType == commentType }
        } else {
            currentComments
        }
        
        _uiState.value = _uiState.value.copy(
            comments = filteredComments,
            selectedFilter = commentType
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TeacherGradeCommentUiState(
    val isLoading: Boolean = false,
    val comments: List<GradeComment> = emptyList(),
    val allComments: List<GradeComment> = emptyList(),
    val selectedFilter: CommentType? = null,
    val currentGradeId: String = "",
    val error: String? = null
)
