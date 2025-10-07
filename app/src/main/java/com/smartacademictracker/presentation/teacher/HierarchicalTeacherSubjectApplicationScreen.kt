package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.presentation.common.HierarchicalSubjectSelector
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalTeacherSubjectApplicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: HierarchicalTeacherSubjectApplicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val yearLevels by viewModel.yearLevels.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val myApplications by viewModel.myApplications.collectAsState()
    val selectedCourseId by viewModel.selectedCourseId.collectAsState()
    val selectedYearLevelId by viewModel.selectedYearLevelId.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    // Show success snackbar when application is successful
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.isApplicationSuccess) {
        if (uiState.isApplicationSuccess) {
            // Switch to applied tab to show the new application
            selectedTab = 1
            showSuccessSnackbar = true
            // Clear success state after a delay
            delay(3000)
            viewModel.clearApplicationSuccess()
            showSuccessSnackbar = false
        }
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
                text = "Apply to Teach Subjects",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { 
                    viewModel.refreshData()
                },
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Browse Subjects") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { 
                    selectedTab = 1
                    // Force refresh applications when switching to Applied tab
                    viewModel.loadMyApplications()
                },
                text = { Text("My Applications (${myApplications.size})") }
            )
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
            when (selectedTab) {
                0 -> {
                    // Browse Subjects Tab
                    HierarchicalSubjectSelector(
                        courses = courses,
                        yearLevels = yearLevels,
                        subjects = subjects,
                        selectedCourseId = selectedCourseId,
                        selectedYearLevelId = selectedYearLevelId,
                        onCourseSelected = { courseId ->
                            viewModel.selectCourse(courseId)
                        },
                        onYearLevelSelected = { yearLevelId ->
                            viewModel.selectYearLevel(yearLevelId)
                        },
                        onSubjectSelected = { subject ->
                            viewModel.applyForSubject(subject.id)
                        }
                    )
                }
                1 -> {
                    // Applied Subjects Tab
                    if (myApplications.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No applications yet",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Apply to teach subjects to see them here",
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
                            items(myApplications) { application ->
                                AppliedTeacherSubjectCard(
                                    application = application
                                )
                            }
                        }
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
        
        // Success Snackbar
        if (showSuccessSnackbar) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { showSuccessSnackbar = false }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Application submitted successfully!")
                }
            }
        }
    }
}

@Composable
fun AppliedTeacherSubjectCard(
    application: TeacherApplication
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (application.status) {
                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
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
                        ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.secondary
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
                                ApplicationStatus.WITHDRAWN -> Icons.Default.ArrowBack
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = when (application.status) {
                                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
                                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                                ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSecondary
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
                                ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSecondary
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
        }
    }
}
