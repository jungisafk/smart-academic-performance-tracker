package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.GradeComment
import com.smartacademictracker.data.model.CommentType
import com.smartacademictracker.presentation.utils.ErrorMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeCommentScreen(
    gradeId: String,
    studentId: String,
    studentName: String,
    subjectId: String,
    subjectName: String,
    onNavigateBack: () -> Unit = {},
    viewModel: TeacherGradeCommentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(gradeId) {
        viewModel.loadComments(gradeId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Comments") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Show add comment dialog */ }
            ) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Student Info Card
            StudentInfoCard(
                studentName = studentName,
                subjectName = subjectName
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Chips
            FilterChipsSection(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { filter ->
                    viewModel.filterCommentsByType(filter)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                ErrorMessage(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadComments(gradeId) }
                )
            } else {
                // Comments List
                if (uiState.comments.isEmpty()) {
                    EmptyCommentsState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.comments) { comment ->
                            CommentCard(
                                comment = comment,
                                onEdit = { commentId, updatedText ->
                                    viewModel.updateComment(commentId, updatedText)
                                },
                                onDelete = { commentId ->
                                    viewModel.deleteComment(commentId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentInfoCard(
    studentName: String,
    subjectName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Student Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Student: $studentName",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = "Subject: $subjectName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterChipsSection(
    selectedFilter: CommentType?,
    onFilterSelected: (CommentType?) -> Unit
) {
    Column {
        Text(
            text = "Filter by Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All") }
            )
            CommentType.values().forEach { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { onFilterSelected(type) },
                    label = { Text(type.displayName) }
                )
            }
        }
    }
}

@Composable
fun CommentCard(
    comment: GradeComment,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.teacherName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommentTypeChip(type = comment.commentType)
                    if (comment.isPrivate) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Private",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = comment.comment,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showEditDialog = true }
                    ) {
                        Text("Edit")
                    }
                    TextButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
    
    if (showEditDialog) {
        EditCommentDialog(
            comment = comment,
            onDismiss = { showEditDialog = false },
            onSave = { updatedText ->
                onEdit(comment.id, updatedText)
                showEditDialog = false
            }
        )
    }
    
    if (showDeleteDialog) {
        DeleteCommentDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDelete(comment.id)
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun CommentTypeChip(type: CommentType) {
    val (backgroundColor, contentColor) = when (type) {
        CommentType.FEEDBACK -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        CommentType.SUGGESTION -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        CommentType.ENCOURAGEMENT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        CommentType.CONCERN -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        CommentType.ACHIEVEMENT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EditCommentDialog(
    comment: GradeComment,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var updatedText by remember { mutableStateOf(comment.comment) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Comment") },
        text = {
            OutlinedTextField(
                value = updatedText,
                onValueChange = { updatedText = it },
                label = { Text("Comment") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(updatedText) },
                enabled = updatedText.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteCommentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Comment") },
        text = { Text("Are you sure you want to delete this comment? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyCommentsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No comments yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Add feedback and comments for this student's grade",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        .format(Date(timestamp))
}
