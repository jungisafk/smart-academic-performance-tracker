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
import com.smartacademictracker.data.model.AssignmentSubmission
import com.smartacademictracker.data.model.SubmissionStatus
import com.smartacademictracker.presentation.utils.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSubmissionTrackingScreen(
    subjectId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: TeacherSubmissionTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(subjectId) {
        viewModel.loadSubmissions(subjectId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submission Tracking") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
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
                    onRetry = { viewModel.loadSubmissions(subjectId) }
                )
            } else {
                // Statistics Card
                uiState.statistics?.let { statistics ->
                    SubmissionStatisticsCard(statistics = statistics)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Filter Chips
                FilterChipsSection(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { filter ->
                        viewModel.filterSubmissionsByStatus(filter)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Submissions List
                if (uiState.submissions.isEmpty()) {
                    EmptySubmissionsState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.submissions) { submission ->
                            SubmissionCard(
                                submission = submission,
                                onGrade = { grade, feedback ->
                                    viewModel.gradeSubmission(submission.id, grade, feedback)
                                },
                                onMarkLate = {
                                    viewModel.markAsLate(submission.id)
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
fun SubmissionStatisticsCard(statistics: com.smartacademictracker.data.repository.SubmissionStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Submission Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Total",
                    value = statistics.totalAssignments.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatisticItem(
                    label = "Submitted",
                    value = statistics.submittedCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatisticItem(
                    label = "Late",
                    value = statistics.lateCount.toString(),
                    color = MaterialTheme.colorScheme.error
                )
                StatisticItem(
                    label = "Graded",
                    value = statistics.gradedCount.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = (statistics.submissionRate / 100).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Submission Rate: ${String.format("%.1f", statistics.submissionRate)}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


@Composable
fun FilterChipsSection(
    selectedFilter: SubmissionStatus?,
    onFilterSelected: (SubmissionStatus?) -> Unit
) {
    Column {
        Text(
            text = "Filter by Status",
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
            SubmissionStatus.values().forEach { status ->
                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { onFilterSelected(status) },
                    label = { Text(status.displayName) }
                )
            }
        }
    }
}

@Composable
fun SubmissionCard(
    submission: AssignmentSubmission,
    onGrade: (Double, String) -> Unit,
    onMarkLate: () -> Unit
) {
    var showGradeDialog by remember { mutableStateOf(false) }
    
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
                    text = submission.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                StatusChip(status = submission.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = submission.assignmentTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (submission.assignmentDescription.isNotEmpty()) {
                Text(
                    text = submission.assignmentDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Due: ${formatDate(submission.dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                submission.submittedDate?.let { submittedDate ->
                    Text(
                        text = "Submitted: ${formatDate(submittedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            submission.grade?.let { grade ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grade: ${String.format("%.1f", grade)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            submission.feedback?.let { feedback ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Feedback: $feedback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (submission.status == SubmissionStatus.SUBMITTED || submission.status == SubmissionStatus.LATE) {
                    Button(
                        onClick = { showGradeDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grade")
                    }
                }
                
                if (submission.status == SubmissionStatus.SUBMITTED && submission.lateSubmission) {
                    OutlinedButton(
                        onClick = onMarkLate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Late")
                    }
                }
            }
        }
    }
    
    if (showGradeDialog) {
        GradeSubmissionDialog(
            submission = submission,
            onDismiss = { showGradeDialog = false },
            onGrade = { grade, feedback ->
                onGrade(grade, feedback)
                showGradeDialog = false
            }
        )
    }
}

@Composable
fun StatusChip(status: SubmissionStatus) {
    val (backgroundColor, contentColor) = when (status) {
        SubmissionStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SubmissionStatus.SUBMITTED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SubmissionStatus.LATE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SubmissionStatus.GRADED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SubmissionStatus.RETURNED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GradeSubmissionDialog(
    submission: AssignmentSubmission,
    onDismiss: () -> Unit,
    onGrade: (Double, String) -> Unit
) {
    var grade by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grade Submission") },
        text = {
            Column {
                Text(
                    text = "Student: ${submission.studentName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Assignment: ${submission.assignmentTitle}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("Grade (0-100)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Feedback (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val gradeValue = grade.toDoubleOrNull()
                    if (gradeValue != null && gradeValue >= 0 && gradeValue <= 100) {
                        onGrade(gradeValue, feedback)
                    }
                },
                enabled = grade.toDoubleOrNull() != null
            ) {
                Text("Submit Grade")
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
fun EmptySubmissionsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No submissions found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Students haven't submitted any assignments yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        .format(Date(timestamp))
}
