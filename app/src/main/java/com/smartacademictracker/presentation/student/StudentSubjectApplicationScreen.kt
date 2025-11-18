package com.smartacademictracker.presentation.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.CheckCircle
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
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.presentation.student.StudentApplicationCardWithCancel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSubjectApplicationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApplicationDetail: (String) -> Unit = {},
    viewModel: StudentSubjectApplicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val myApplications by viewModel.myApplications.collectAsState()
    val canApplyForSubject by viewModel.canApplyForSubject.collectAsState()
    val selectedYearLevel by viewModel.selectedYearLevel.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }

    // Load data when screen is composed, using ViewModel as key to prevent unnecessary reloads
    LaunchedEffect(viewModel) {
        viewModel.loadAvailableSubjects()
        viewModel.loadMyApplications()
    }
    
    // Show success snackbar when application is successful
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(uiState.isApplicationSuccess) {
        if (uiState.isApplicationSuccess) {
            // Switch to applied tab to show the new application
            selectedTab = 1
            showSuccessSnackbar = true
            successMessage = "Application submitted successfully!"
            // Clear success state after a delay
            delay(3000)
            viewModel.clearApplicationSuccess()
            showSuccessSnackbar = false
            successMessage = null
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            successMessage = message
            showSuccessSnackbar = true
            delay(3000)
            showSuccessSnackbar = false
            successMessage = null
            viewModel.clearError() // Clear success message from state
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Custom Tab Row - Modern rounded buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Available Tab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedTab = 0 },
                color = if (selectedTab == 0) Color(0xFFFFC107) else Color(0xFF2196F3),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Available (${availableSubjects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) Color(0xFF333333) else Color.White
                    )
                }
            }
            
            // Applied Tab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { 
                        selectedTab = 1
                        // Force refresh applications when switching to Applied tab
                        viewModel.loadMyApplications()
                    },
                color = if (selectedTab == 1) Color(0xFFFFC107) else Color(0xFF2196F3),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Applied (${myApplications.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) Color(0xFF333333) else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                    // Available Subjects Tab
                    if (availableSubjects.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No subjects available",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No subjects match your current filters or year level.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableSubjects) { subject ->
                                // Check if student can apply for this subject
                                // If there's a PENDING application, can't apply
                                // If there's an APPROVED application, check enrollment status
                                val hasPending = myApplications.any { 
                                    it.subjectId == subject.id && 
                                    it.status == StudentApplicationStatus.PENDING 
                                }
                                val hasApproved = myApplications.any { 
                                    it.subjectId == subject.id && 
                                    it.status == StudentApplicationStatus.APPROVED 
                                }
                                // Use canApplyForSubject map if available, otherwise fallback to checking applications
                                val canApply = canApplyForSubject[subject.id] ?: (!hasPending && !hasApproved)
                                
                                AvailableSubjectCard(
                                    subject = subject,
                                    isApplied = !canApply, // Show "Already Applied" if can't apply
                                    onApply = { viewModel.applyForSubject(subject.id) },
                                    isApplying = uiState.applyingSubjects.contains(subject.id)
                                )
                            }
                        }
                    }
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
                                    text = "Apply for subjects to see them here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(myApplications) { application ->
                                StudentApplicationCardWithCancel(
                                    application = application,
                                    onCancel = { 
                                        viewModel.cancelApplication(application.id) 
                                    }
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
        if (showSuccessSnackbar && successMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { 
                        showSuccessSnackbar = false
                        successMessage = null
                    }) {
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
                    Text(successMessage ?: "Operation successful!")
                }
            }
        }
    }
}

@Composable
fun AvailableSubjectCard(
    subject: Subject,
    isApplied: Boolean,
    onApply: () -> Unit,
    isApplying: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isApplied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subject.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onApply,
                enabled = !isApplied && !isApplying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Applying...",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (isApplied) {
                    Text(
                        text = "Already Applied",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Apply for Subject",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}