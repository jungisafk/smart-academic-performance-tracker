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
    viewModel: AdminGradeEditRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradeRequests by viewModel.gradeRequests.collectAsState()
    
    // Track previous count to detect new items
    var previousCount by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Edit Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && gradeRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
        } else if (gradeRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    title = "No Edit Requests",
                    message = "There are no pending grade edit requests.",
                    icon = Icons.Default.CheckCircle
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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

                // Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pending Requests",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                AnimatedContent(
                                    targetState = uiState.requestCount,
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
                                        text = "$count request(s)",
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
                    }
                }

                // Grade requests list with smooth animations
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
                            onApprove = { viewModel.approveEditRequest(grade.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeEditRequestCard(
    grade: com.smartacademictracker.data.model.Grade,
    onApprove: () -> Unit
) {
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

            // Approve button
            Button(
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Approve Edit Request")
            }
        }
    }
}

