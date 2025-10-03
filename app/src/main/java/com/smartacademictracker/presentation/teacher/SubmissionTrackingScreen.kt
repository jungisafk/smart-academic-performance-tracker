package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.utils.GradeCalculationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionTrackingScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    viewModel: SubmissionTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val submissionStatus by viewModel.submissionStatus.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    LaunchedEffect(subjectId) {
        viewModel.loadSubmissionStatus(subjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Submission Tracking",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subject?.name ?: "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Refresh button
            IconButton(
                onClick = { viewModel.refreshSubmissionStatus() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Grade Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradePeriod.values().forEach { period ->
                        FilterChip(
                            onClick = { viewModel.setSelectedPeriod(period) },
                            label = { Text(period.displayName) },
                            selected = selectedPeriod == period,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubmissionSummaryCard(
                title = "Total Students",
                count = submissionStatus.totalStudents,
                icon = Icons.Default.Person,
                color = MaterialTheme.colorScheme.primary
            )
            
            SubmissionSummaryCard(
                title = "Submitted",
                count = submissionStatus.submittedCount,
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.primary
            )
            
            SubmissionSummaryCard(
                title = "Pending",
                count = submissionStatus.pendingCount,
                icon = Icons.Default.Schedule,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter options
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
                        text = "Filter by Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { viewModel.toggleFilterMenu() }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
                
                if (filterStatus.showFilterMenu) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { viewModel.setStatusFilter(SubmissionStatusFilter.ALL) },
                            label = { Text("All") },
                            selected = filterStatus.statusFilter == SubmissionStatusFilter.ALL,
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            onClick = { viewModel.setStatusFilter(SubmissionStatusFilter.SUBMITTED) },
                            label = { Text("Submitted") },
                            selected = filterStatus.statusFilter == SubmissionStatusFilter.SUBMITTED,
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            onClick = { viewModel.setStatusFilter(SubmissionStatusFilter.PENDING) },
                            label = { Text("Pending") },
                            selected = filterStatus.statusFilter == SubmissionStatusFilter.PENDING,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Students list
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val filteredStudents = submissionStatus.students.filter { student ->
                when (filterStatus.statusFilter) {
                    SubmissionStatusFilter.ALL -> true
                    SubmissionStatusFilter.SUBMITTED -> student.isSubmitted
                    SubmissionStatusFilter.PENDING -> !student.isSubmitted
                }
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredStudents) { student ->
                    StudentSubmissionCard(
                        student = student,
                        onRetrySubmission = {
                            viewModel.retrySubmission(student.studentId)
                        }
                    )
                }
            }
        }
    }

    // Error Message
    uiState.error?.let { error ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SubmissionSummaryCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
fun StudentSubmissionCard(
    student: StudentSubmissionStatus,
    onRetrySubmission: () -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Student Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.studentName.first().toString().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = student.studentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${student.studentId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status indicator
                SubmissionStatusIndicator(
                    status = if (student.isSubmitted) SubmissionStatus.SUBMITTED else SubmissionStatus.PENDING,
                    lastUpdated = student.lastUpdated
                )
            }
            
            if (student.isSubmitted) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Grade: ${String.format("%.1f", student.gradePercentage)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            student.gradePercentage >= 90 -> MaterialTheme.colorScheme.primary
                            student.gradePercentage >= 75 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    Text(
                        text = "Submitted: ${formatDate(student.lastUpdated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No grade submitted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (student.hasError) {
                        OutlinedButton(
                            onClick = onRetrySubmission,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubmissionStatusIndicator(
    status: SubmissionStatus,
    lastUpdated: Long
) {
    Surface(
        color = when (status) {
            SubmissionStatus.SUBMITTED -> MaterialTheme.colorScheme.primaryContainer
            SubmissionStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
            SubmissionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    SubmissionStatus.SUBMITTED -> Icons.Default.CheckCircle
                    SubmissionStatus.PENDING -> Icons.Default.Schedule
                    SubmissionStatus.ERROR -> Icons.Default.Error
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when (status) {
                    SubmissionStatus.SUBMITTED -> MaterialTheme.colorScheme.primary
                    SubmissionStatus.PENDING -> MaterialTheme.colorScheme.secondary
                    SubmissionStatus.ERROR -> MaterialTheme.colorScheme.error
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (status) {
                    SubmissionStatus.SUBMITTED -> "Submitted"
                    SubmissionStatus.PENDING -> "Pending"
                    SubmissionStatus.ERROR -> "Error"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class SubmissionStatus {
    SUBMITTED,
    PENDING,
    ERROR
}

enum class SubmissionStatusFilter {
    ALL,
    SUBMITTED,
    PENDING
}

data class StudentSubmissionStatus(
    val studentId: String,
    val studentName: String,
    val isSubmitted: Boolean,
    val gradePercentage: Double,
    val lastUpdated: Long,
    val hasError: Boolean = false
)

data class SubmissionStatusSummary(
    val totalStudents: Int,
    val submittedCount: Int,
    val pendingCount: Int,
    val students: List<StudentSubmissionStatus>
)

data class FilterStatus(
    val showFilterMenu: Boolean = false,
    val statusFilter: SubmissionStatusFilter = SubmissionStatusFilter.ALL
)

private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(date)
}
