package com.smartacademictracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.presentation.common.HierarchicalSubjectSelector
import com.smartacademictracker.presentation.common.SectionBasedSubjectSelector
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalStudentSubjectApplicationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApplicationDetail: (String) -> Unit = {},
    viewModel: HierarchicalStudentSubjectApplicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val yearLevels by viewModel.yearLevels.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val myApplications by viewModel.myApplications.collectAsState()
    val sectionAssignments by viewModel.sectionAssignments.collectAsState()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Enhanced Header Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Apply for Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Browse and apply for academic subjects",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // Refresh Button
                IconButton(
                    onClick = { 
                        viewModel.refreshData()
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 100.dp)
        ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Enhanced Tab Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = Color(0xFF1976D2)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Browse Subjects",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color(0xFF1976D2) else Color(0xFF666666)
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        // Force refresh applications when switching to Applied tab
                        viewModel.loadMyApplications()
                    },
                    text = { 
                        Text(
                            "My Applications (${myApplications.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color(0xFF1976D2) else Color(0xFF666666)
                        ) 
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Enhanced Loading State
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1976D2),
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading subjects...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            when (selectedTab) {
                0 -> {
                    // Browse Subjects Tab
                    SectionBasedSubjectSelector(
                        courses = courses,
                        yearLevels = yearLevels,
                        subjects = subjects,
                        selectedCourseId = selectedCourseId,
                        selectedYearLevelId = selectedYearLevelId,
                        sectionAssignments = sectionAssignments,
                        onCourseSelected = { courseId ->
                            viewModel.selectCourse(courseId)
                        },
                        onYearLevelSelected = { yearLevelId ->
                            viewModel.selectYearLevel(yearLevelId)
                        },
                        onSubjectWithSectionSelected = { subject, sectionName ->
                            viewModel.applyForSubject(subject.id, sectionName)
                        }
                    )
                }
                1 -> {
                    // Applied Subjects Tab
                    if (myApplications.isEmpty()) {
                        // Enhanced Empty State
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE3F2FD)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Assignment,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFF1976D2)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "No Applications Yet",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Browse subjects and apply to see your applications here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1976D2),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(myApplications) { application ->
                                AppliedSubjectCard(
                                    application = application,
                                    onNavigateToDetail = { onNavigateToApplicationDetail(application.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Enhanced Error Message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Success Snackbar
        if (showSuccessSnackbar) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFFE8F5E8),
                contentColor = Color(0xFF2E7D32),
                action = {
                    TextButton(
                        onClick = { showSuccessSnackbar = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2E7D32))
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Application submitted successfully!",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    }
}

@Composable
fun AppliedSubjectCard(
    application: SubjectApplication,
    onNavigateToDetail: () -> Unit
) {
    Card(
        onClick = onNavigateToDetail,
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
                        text = application.subjectId, // Using subjectId as code since subjectCode doesn't exist
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
                    Text(
                        text = application.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (application.status) {
                            ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onPrimary
                            ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                            ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                            ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Year: ${application.yearLevelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Course: ${application.courseName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Applied: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(application.appliedDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (application.notes != null && application.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${application.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
