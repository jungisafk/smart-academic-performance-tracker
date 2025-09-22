package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentApplicationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TeacherStudentApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val applications by viewModel.applications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadApplications()
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
            Text(
                text = "Student Applications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.refreshApplications() },
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading State
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Applications List
            if (applications.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No applications found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Students will appear here when they apply for your subjects",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(applications) { application ->
                        StudentApplicationCard(
                            application = application,
                            onApprove = { 
                                viewModel.updateApplicationStatus(
                                    application.id, 
                                    StudentApplicationStatus.APPROVED,
                                    "Application approved by teacher"
                                )
                            },
                            onReject = { 
                                viewModel.updateApplicationStatus(
                                    application.id, 
                                    StudentApplicationStatus.REJECTED,
                                    "Application rejected by teacher"
                                )
                            },
                            isProcessing = uiState.processingApplications.contains(application.id)
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
}

@Composable
fun StudentApplicationCard(
    application: StudentApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (application.status) {
                StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = application.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${application.subjectCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = when (application.status) {
                        StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.primary
                        StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (application.status) {
                                StudentApplicationStatus.PENDING -> Icons.Default.Person
                                StudentApplicationStatus.APPROVED -> Icons.Default.Check
                                StudentApplicationStatus.REJECTED -> Icons.Default.Close
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = when (application.status) {
                                StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
                                StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                                StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = application.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (application.status) {
                                StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
                                StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                                StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Student Information
            Text(
                text = "Student: ${application.studentName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Email: ${application.studentEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Year Level: ${application.yearLevelName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Course: ${application.courseCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (application.applicationReason.isNotBlank()) {
                Text(
                    text = "Reason: ${application.applicationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Text(
                text = "Applied on: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(application.appliedAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (application.reviewedAt != null) {
                Text(
                    text = "Reviewed on: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(application.reviewedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (application.teacherComments != null && application.teacherComments.isNotBlank()) {
                Text(
                    text = "Comments: ${application.teacherComments}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Action Buttons (only show for pending applications)
            if (application.status == StudentApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}
