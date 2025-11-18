package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.model.EnrollmentStatus
import com.smartacademictracker.presentation.common.ChartUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherStudentManagementScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    viewModel: TeacherStudentManagementViewModel = hiltViewModel(),
    studentApplicationsViewModel: TeacherStudentApplicationsViewModel = hiltViewModel(),
    analyticsViewModel: TeacherAnalyticsViewModel = hiltViewModel(),
    showBackButton: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val selectedSectionStudents by viewModel.selectedSectionStudents.collectAsState()

    val studentAppsUiState by studentApplicationsViewModel.uiState.collectAsState()
    val studentApplications by studentApplicationsViewModel.applications.collectAsState()

    // Analytics state
    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    val classPerformance by analyticsViewModel.classPerformance.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadTeacherSections()
    }
    
    LaunchedEffect(studentApplicationsViewModel) {
        studentApplicationsViewModel.loadApplications()
    }
    
    // Load analytics when Analytics tab is selected (only if not already loaded)
    LaunchedEffect(selectedTab, analyticsViewModel) {
        if (selectedTab == 2 && classPerformance.isEmpty()) {
            analyticsViewModel.loadAnalyticsData()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            // Header with horizontal padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF666666)
                        )
                    }
                }
                Text(
                    text = "Student Management",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        viewModel.loadTeacherSections()
                        studentApplicationsViewModel.loadApplications()
                        if (selectedTab == 2) {
                            analyticsViewModel.loadAnalyticsData()
                        }
                    },
                    enabled = !uiState.isLoading && !studentAppsUiState.isLoading && !analyticsUiState.isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF666666)
                    )
                }
            }

            // Summary Card with horizontal padding
            if (!uiState.isLoading && !studentAppsUiState.isLoading && (sections.isNotEmpty() || studentApplications.isNotEmpty())) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Student Management",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append("${sections.size} Sections")
                                    append(" • ")
                                    val newApplicationsCount = studentApplications.count { it.status == StudentApplicationStatus.PENDING }
                                    append("$newApplicationsCount New Applications")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Management Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Loading State
            if (uiState.isLoading || (selectedTab == 1 && studentAppsUiState.isLoading) || (selectedTab == 2 && analyticsUiState.isLoading)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                            text = "Loading...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                // Tab Row for switching between Sections, Applications, and Analytics
                // Full width container - no horizontal padding to maximize space
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF2196F3),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f),
                            text = {
                                Text(
                                    "Sections",
                                    color = if (selectedTab == 0) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f),
                            text = {
                                Text(
                                    "Applications",
                                    color = if (selectedTab == 1) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f),
                            text = {
                                Text(
                                    "Analytics",
                                    color = if (selectedTab == 2) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content with horizontal padding
                when (selectedTab) {
                0 -> {
                    // Sections Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Select Section",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (uiState.error != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFEBEE)
                                    )
                                ) {
                                    Text(
                                        text = uiState.error ?: "Unknown error",
                                        color = Color(0xFFD32F2F),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else if (sections.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Class,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = Color(0xFF999999)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No Sections Assigned",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(sections) { section ->
                                ModernSectionCard(
                                    section = section,
                                    isSelected = section.id == uiState.selectedSectionId,
                                    onClick = { viewModel.selectSection(section.id) }
                                )
                            }
                        }

                        if (uiState.selectedSectionId != null && !uiState.isLoading) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Students in Section",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (selectedSectionStudents.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F5)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = Color(0xFF999999)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "No Students",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFF666666)
                                            )
                                            Text(
                                                text = "No students are currently enrolled in this section",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF999999)
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(selectedSectionStudents) { enrollment ->
                                    ModernStudentEnrollmentCard(
                                        enrollment = enrollment,
                                        onRemoveStudent = {
                                            viewModel.kickStudent(enrollment.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Student Applications Tab
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        ModernStudentApplicationsContent(
                            uiState = studentAppsUiState,
                            applications = studentApplications,
                            onApprove = { applicationId: String -> studentApplicationsViewModel.approveApplication(applicationId) },
                            onReject = { applicationId: String -> studentApplicationsViewModel.rejectApplication(applicationId) }
                        )
                    }
                }
                2 -> {
                    // Analytics Tab - integrated inline
                    AnalyticsTabContent(
                        uiState = analyticsUiState,
                        classPerformance = classPerformance,
                        onRefresh = { analyticsViewModel.refreshData() },
                        onYearLevelChanged = { yearLevelId: String? -> analyticsViewModel.updateYearLevelFilter(yearLevelId) },
                        onCourseChanged = { courseId: String? -> analyticsViewModel.updateCourseFilter(courseId) },
                        onSubjectChanged = { subjectId: String? -> analyticsViewModel.updateSubjectFilter(subjectId) },
                        onSectionChanged = { sectionId: String? -> analyticsViewModel.updateSectionFilter(sectionId) },
                        onClearFilters = { analyticsViewModel.clearAllFilters() }
                    )
                }
            }
        }

        // Show success/error messages
        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccessMessage()
            }
        }

        uiState.error?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(5000)
                viewModel.clearError()
            }
        }
    }
    }
}

@Composable
fun ModernSectionCard(
    section: com.smartacademictracker.data.model.SectionAssignment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blue circular icon with two people
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.sectionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = section.status.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (section.status) {
                            com.smartacademictracker.data.model.AssignmentStatus.ACTIVE -> Color(0xFF4CAF50)
                            com.smartacademictracker.data.model.AssignmentStatus.INACTIVE -> Color(0xFF999999)
                            com.smartacademictracker.data.model.AssignmentStatus.TERMINATED -> Color(0xFF999999)
                        }
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ModernStudentEnrollmentCard(
    enrollment: com.smartacademictracker.data.model.StudentEnrollment,
    onRemoveStudent: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Student Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blue circular icon with two people
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = enrollment.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Enrolled: ${formatDate(enrollment.enrollmentDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }

                // Active Status Button
                Surface(
                    color = when (enrollment.status) {
                        EnrollmentStatus.ACTIVE -> Color(0xFF2196F3)
                        EnrollmentStatus.DROPPED -> Color(0xFF999999)
                        EnrollmentStatus.COMPLETED -> Color(0xFF4CAF50)
                        EnrollmentStatus.FAILED -> Color(0xFFF44336)
                        EnrollmentStatus.KICKED -> Color(0xFFF44336)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = enrollment.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Remove Student Button
            Button(
                onClick = { showRemoveDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.PersonRemove,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remove Student",
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Remove Confirmation Dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = {
                Text(
                    "Remove Student",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove ${enrollment.studentName} from ${enrollment.sectionName}? This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveStudent()
                        showRemoveDialog = false
                    }
                ) {
                    Text("Remove", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ModernStudentApplicationsContent(
    uiState: TeacherStudentApplicationsUiState,
    applications: List<com.smartacademictracker.data.model.StudentApplication>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (applications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF999999)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No applications found",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Students will appear here when they apply for your subjects",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF999999)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(applications) { application ->
                ModernStudentApplicationCard(
                    application = application,
                    onApprove = { onApprove(application.id) },
                    onReject = { onReject(application.id) },
                    isProcessing = uiState.processingApplications.contains(application.id)
                )
            }
        }
    }
}

@Composable
fun ModernStudentApplicationCard(
    application: com.smartacademictracker.data.model.StudentApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isProcessing: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blue circular icon with two people
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = application.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = application.subjectName.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status Badge
            Surface(
                color = when (application.status) {
                    StudentApplicationStatus.PENDING -> Color(0xFFFF9800)
                    StudentApplicationStatus.APPROVED -> Color(0xFF4CAF50)
                    StudentApplicationStatus.REJECTED -> Color(0xFFF44336)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = application.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Action buttons for pending applications
        if (application.status == StudentApplicationStatus.PENDING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Processing...")
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                    }
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFF44336),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Processing...")
                    } else {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun AnalyticsTabContent(
    uiState: TeacherAnalyticsUiState,
    classPerformance: List<SubjectPerformanceData>,
    onRefresh: () -> Unit,
    onYearLevelChanged: (String?) -> Unit,
    onCourseChanged: (String?) -> Unit,
    onSubjectChanged: (String?) -> Unit,
    onSectionChanged: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Section
        item {
            AnalyticsFilterSection(
                selectedYearLevel = uiState.selectedYearLevel,
                selectedCourse = uiState.selectedCourse,
                selectedSubject = uiState.selectedSubject,
                selectedSection = uiState.selectedSection,
                yearLevels = uiState.availableYearLevels,
                courses = uiState.availableCourses,
                subjects = uiState.availableSubjects,
                sections = uiState.availableSections,
                onYearLevelChanged = onYearLevelChanged,
                onCourseChanged = onCourseChanged,
                onSubjectChanged = onSubjectChanged,
                onSectionChanged = onSectionChanged,
                onClearFilters = onClearFilters
            )
        }

        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.error != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Summary Card
            if (classPerformance.isNotEmpty() || uiState.totalStudents > 0) {
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
                                    text = "Class Performance Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (uiState.classAverage != null) {
                                    Text(
                                        text = "Average Grade: ${String.format("%.2f", uiState.classAverage)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Passing Rate: ${String.format("%.1f", (uiState.passingStudents.toDouble() / uiState.totalStudents * 100))}%",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "No grade data available yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            // Analytics Icon
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Performance Statistics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ClassStatItem("Total Students", uiState.totalStudents.toString())
                            ClassStatItem("Passing", uiState.passingStudents.toString())
                            ClassStatItem("At Risk", uiState.atRiskStudents.toString())
                        }
                    }
                }

                // Subject Performance Overview
                if (classPerformance.isNotEmpty()) {
                    items(classPerformance) { subjectPerformance ->
                        SubjectPerformanceOverviewCard(
                            subjectName = subjectPerformance.subjectName,
                            totalStudents = subjectPerformance.totalStudents,
                            averageGrade = subjectPerformance.averageGrade,
                            passingRate = subjectPerformance.passingRate,
                            gradeDistribution = subjectPerformance.gradeDistribution
                        )
                    }

                    // Class Performance Comparison Chart
                    if (classPerformance.size > 1) {
                        item {
                            ClassPerformanceComparisonChart(
                                subjects = classPerformance.map {
                                    it.subjectName to it.averageGrade
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Performance Data Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start inputting grades to see analytics",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
    }
}


