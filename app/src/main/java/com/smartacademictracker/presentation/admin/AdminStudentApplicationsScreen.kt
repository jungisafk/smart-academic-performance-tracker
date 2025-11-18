package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentApplicationsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminStudentApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val applications by viewModel.applications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadApplications()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // Error state
        else if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
        // Applications list
        else if (applications.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(applications) { application ->
                    StudentApplicationCard(
                        application = application,
                        onApprove = { viewModel.approveApplication(application.id) },
                        onReject = { viewModel.rejectApplication(application.id) },
                        isLoading = uiState.processingApplications.contains(application.id)
                    )
                }
            }
        }
        // Empty state
        else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Applications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "There are no student applications to review at this time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StudentApplicationCard(
    application: SubjectApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with student info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student ID: ${application.studentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                Surface(
                    color = when (application.status) {
                        ApplicationStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                        ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                        ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = application.status.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (application.status) {
                            ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                            ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimaryContainer
                            ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
                            ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subject details
            Text(
                text = "Subject Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${application.subjectName} (${application.subjectId})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Section: ${application.sectionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Course and year level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Course: ${application.courseName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Year: ${application.yearLevelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Academic period
            Text(
                text = "Academic Period: ${application.academicYear} - ${application.semester}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Application date
            Text(
                text = "Applied: ${java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(java.util.Date(application.appliedDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Action buttons (only for pending applications)
            if (application.status == ApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReject,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject")
                    }
                    
                    Button(
                        onClick = onApprove,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}
