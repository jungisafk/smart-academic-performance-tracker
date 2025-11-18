package com.smartacademictracker.presentation.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGradeEditRequestsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminGradeEditRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradeRequests by viewModel.gradeRequests.collectAsState()
    val gradeHistory by viewModel.gradeHistory.collectAsState()
    
    // Track previous count to detect new items
    var previousCount by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    var showHistory by remember { mutableStateOf(false) }
    
    // Scroll to top when new items are added
    LaunchedEffect(gradeRequests.size) {
        if (gradeRequests.size > previousCount && previousCount > 0) {
            // New item added - scroll to top with animation
            listState.animateScrollToItem(0)
        }
        previousCount = gradeRequests.size
    }

    // Handle success/error messages
    LaunchedEffect(uiState.successMessage) {
        val message = uiState.successMessage
        if (message != null) {
            // Clear after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null) {
            // Clear after 5 seconds
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    if (uiState.isLoading && gradeRequests.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (uiState.error != null) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Success message
                val successMessage = uiState.successMessage
                if (successMessage != null) {
                    item {
                        AnimatedVisibility(
                            visible = successMessage != null,
                            enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                                animationSpec = tween(300),
                                expandFrom = Alignment.Top
                            ),
                            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                                animationSpec = tween(300),
                                shrinkTowards = Alignment.Top
                            )
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = successMessage,
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                // Header with toggle
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (showHistory) "Edit Request History" else "Pending Requests",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    AnimatedContent(
                                        targetState = if (showHistory) gradeHistory.size else uiState.requestCount,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(300)) + slideInVertically(
                                                animationSpec = tween(300),
                                                initialOffsetY = { -it }
                                            ) togetherWith fadeOut(animationSpec = tween(300)) + slideOutVertically(
                                                animationSpec = tween(300),
                                                targetOffsetY = { it }
                                            )
                                        },
                                        label = "requestCount"
                                    ) { count ->
                                        Text(
                                            text = if (showHistory) "$count item(s)" else "$count request(s)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Toggle button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !showHistory,
                                    onClick = { showHistory = false },
                                    label = { Text("Pending") },
                                    leadingIcon = if (!showHistory) {
                                        { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = showHistory,
                                    onClick = { showHistory = true },
                                    label = { Text("History") },
                                    leadingIcon = if (showHistory) {
                                        { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Show pending requests or history based on toggle
                if (!showHistory) {
                    // Pending Requests Section
                    if (gradeRequests.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No Edit Requests",
                                message = "There are no pending grade edit requests.",
                                icon = Icons.Default.CheckCircle
                            )
                        }
                    } else {
                        items(
                            items = gradeRequests,
                            key = { it.id }
                        ) { grade ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = FastOutSlowInEasing
                                    )
                                ) + slideInVertically(
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = FastOutSlowInEasing
                                    ),
                                    initialOffsetY = { -it / 2 }
                                ) + expandVertically(
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = FastOutSlowInEasing
                                    ),
                                    expandFrom = Alignment.Top
                                ),
                                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                                    animationSpec = tween(200),
                                    targetOffsetY = { it }
                                ) + shrinkVertically(
                                    animationSpec = tween(200),
                                    shrinkTowards = Alignment.Top
                                )
                            ) {
                                GradeEditRequestCard(
                                    grade = grade,
                                    onApprove = { viewModel.approveEditRequest(grade.id) },
                                    onReject = { reason -> viewModel.rejectEditRequest(grade.id, reason) }
                                )
                            }
                        }
                    }
                } else {
                    // History Section
                    if (gradeHistory.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No History",
                                message = "No grade edit request history available.",
                                icon = Icons.Default.Schedule
                            )
                        }
                    } else {
                        items(
                            items = gradeHistory,
                            key = { it.id }
                        ) { grade ->
                            GradeEditHistoryCard(grade = grade)
                        }
                    }
                }
            }
        }
    }

@Composable
fun GradeEditHistoryCard(
    grade: com.smartacademictracker.data.model.Grade
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grade.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = grade.subjectName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text(
                        text = grade.gradePeriod.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Grade details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Grade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${grade.score}% (${grade.letterGrade})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Teacher ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = grade.teacherId.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // History info
            if (grade.unlockedAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Approved on ${java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(java.util.Date(grade.unlockedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (grade.lockedAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Originally locked on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(grade.lockedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GradeEditRequestCard(
    grade: com.smartacademictracker.data.model.Grade,
    onApprove: () -> Unit,
    onReject: (String?) -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grade.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = grade.subjectName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(
                    containerColor = Color(0xFFFF9800)
                ) {
                    Text(
                        text = grade.gradePeriod.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Grade details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Grade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${grade.score}% (${grade.letterGrade})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Teacher ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = grade.teacherId.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Lock info
            if (grade.lockedAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Locked on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(grade.lockedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reject button
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject")
                }
                
                // Approve button
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve")
                }
            }
        }
    }
    
    // Reject confirmation dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = {
                Text(
                    text = "Reject Edit Request",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Are you sure you want to reject this grade edit request?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("Enter rejection reason...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(if (rejectReason.isBlank()) null else rejectReason)
                        showRejectDialog = false
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        rejectReason = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

