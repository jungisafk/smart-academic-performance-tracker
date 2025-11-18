package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Error
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
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherApplicationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TeacherApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val applications by viewModel.applications.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.loadApplications()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color(0xFF666666)
                            )
                        }
                        Text(
                            text = "My Applications",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.loadApplications() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh Applications",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }
            
            // Summary Card
            if (!uiState.isLoading && applications.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Application Status",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "${applications.size} Application${if (applications.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Track your subject application progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            // Application Icon
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Loading State
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading applications...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            } else {
                // Applications List
                if (applications.isEmpty()) {
                    // Empty State
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No applications found",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Apply for subjects to see your applications here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(applications) { application ->
                        ApplicationCard(
                            application = application,
                            onCancel = { 
                                viewModel.cancelApplication(application.id) 
                            }
                        )
                    }
                }
            }

            // Error Message
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationCard(
    application: TeacherApplication,
    onCancel: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (application.status) {
                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
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
                        ApplicationStatus.PENDING -> MaterialTheme.colorScheme.primary
                        ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (application.status) {
                                ApplicationStatus.PENDING -> Icons.Default.HourglassEmpty
                                ApplicationStatus.APPROVED -> Icons.Default.Check
                                ApplicationStatus.REJECTED -> Icons.Default.Close
                                else -> Icons.Default.HourglassEmpty
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = when (application.status) {
                                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
                                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = application.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (application.status) {
                                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
                                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
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
            
            if (application.adminComments != null && application.adminComments.isNotBlank()) {
                Text(
                    text = "Admin Comments: ${application.adminComments}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Cancel button for pending applications
            if (application.status == ApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Application")
                }
            }
        }
    }
}
