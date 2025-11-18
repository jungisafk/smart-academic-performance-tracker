package com.smartacademictracker.presentation.admin

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
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.model.SectionAssignment
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.presentation.admin.TeacherSectionAssignmentViewModel
import com.smartacademictracker.presentation.admin.CollapsibleCourseCard
import com.smartacademictracker.presentation.admin.EnhancedSubjectSectionCard
import com.smartacademictracker.presentation.admin.YearLevelGroup
import com.smartacademictracker.presentation.admin.CollapsibleYearLevelCard
import com.smartacademictracker.presentation.admin.CollapsibleSubjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApplicationsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminApplicationsViewModel = hiltViewModel(),
    sectionAssignmentViewModel: TeacherSectionAssignmentViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    val applications by viewModel.applications.collectAsState()

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        
        viewModel.loadApplications()
        if (selectedTab == 1) {
            sectionAssignmentViewModel.loadData()
        }
    }
    
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            sectionAssignmentViewModel.loadData()
        }
    }
    
    // Reload applications when screen becomes visible
    LaunchedEffect(applications.size) {
        
        applications.forEach { app ->
            
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Show loading only if no data exists yet
        if (uiState.isLoading && applications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2196F3))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF2196F3),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = tabPositions[selectedTab].left)
                                .width(tabPositions[selectedTab].width),
                            color = Color(0xFF2196F3),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Applications")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Assignment,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Assignments")
                            }
                        }
                    )
                }
            
                // Content Section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Applications Tab Content
                            // Enhanced Loading State
                            if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading applications...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF333333)
                                )
                                }
                            }
                        }
                    } else {
                        // Applications List
                        if (applications.isEmpty()) {
                            // Enhanced Empty State
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Icon with background
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Assignment,
                                            contentDescription = "No Applications",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Text(
                                        text = "No applications found",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "Teacher applications will appear here once they apply for subjects",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(applications) { application ->
                                    EnhancedApplicationCard(
                                        application = application,
                                        onApprove = { viewModel.approveApplication(application.id) },
                                        onReject = { viewModel.rejectApplication(application.id) }
                                    )
                                }
                            }
                        }
                    }
                        }
                        1 -> {
                            // Assignments Tab Content - Use full TeacherSectionAssignmentScreen functionality
                            val sectionUiState by sectionAssignmentViewModel.uiState.collectAsState()
                            val sectionSubjects by sectionAssignmentViewModel.subjects.collectAsState()
                            val sectionAssignments by sectionAssignmentViewModel.sectionAssignments.collectAsState()
                            val sectionTeacherApplications by sectionAssignmentViewModel.teacherApplications.collectAsState()
                            val sectionYearLevels by sectionAssignmentViewModel.yearLevels.collectAsState()
                            
                            
                            
                            if (sectionUiState.isLoading && sectionSubjects.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2196F3))
                                }
                            } else if (sectionSubjects.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No subjects available. Please add subjects first.",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF666666)
                                    )
                                }
                            } else {
                                // Use the full TeacherSectionAssignmentScreen content structure
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Separate MAJOR and MINOR subjects
                                    val majorSubjects = sectionSubjects.filter { 
                                        it.subjectType == com.smartacademictracker.data.model.SubjectType.MAJOR &&
                                        it.courseId.isNotBlank() && it.courseName.isNotBlank()
                                    }
                                    val minorSubjects = sectionSubjects.filter { 
                                        it.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                    }
                                    
                                    val groupedByCourse = majorSubjects.groupBy { it.courseId }
                                    
                                    groupedByCourse.forEach { (courseId, courseSubjects) ->
                                        val courseName = courseSubjects.firstOrNull()?.courseName ?: "Unknown Course"
                                        
                                        val groupedByYearLevel = courseSubjects.groupBy { 
                                            it.yearLevelId.ifBlank { "NO_YEAR_LEVEL" }
                                        }
                                        
                                        if (courseSubjects.isNotEmpty()) {
                                            item {
                                                // Use CollapsibleCourseCard from TeacherSectionAssignmentScreen
                                                CollapsibleCourseCard(
                                                    courseName = courseName,
                                                    courseId = courseId,
                                                    yearLevelGroups = groupedByYearLevel.map { (yearLevelId, yearSubjects) ->
                                                        val yearLevel = if (yearLevelId != "NO_YEAR_LEVEL" && yearLevelId.isNotBlank()) {
                                                            sectionYearLevels.find { it.id == yearLevelId }
                                                        } else null
                                                        
                                                        YearLevelGroup(
                                                            yearLevelId = if (yearLevelId == "NO_YEAR_LEVEL") "" else yearLevelId,
                                                            yearLevelName = if (yearLevelId == "NO_YEAR_LEVEL") {
                                                                "No Year Level"
                                                            } else {
                                                                yearLevel?.let { "Year ${it.level}" } ?: (yearSubjects.firstOrNull()?.yearLevelName ?: "Unknown Year")
                                                            },
                                                            subjects = yearSubjects
                                                        )
                                                    }.sortedBy { group ->
                                                        if (group.yearLevelId.isEmpty()) {
                                                            999
                                                        } else {
                                                            val yearLevel = sectionYearLevels.find { it.id == group.yearLevelId }
                                                            yearLevel?.level ?: 999
                                                        }
                                                    },
                                                    sectionAssignments = sectionAssignments,
                                                    teacherApplications = sectionTeacherApplications,
                                                    onAssignTeacher = { subjectId: String, sectionName: String, teacherId: String ->
                                                        sectionAssignmentViewModel.assignTeacherToSection(subjectId, sectionName, teacherId)
                                                    },
                                                    onRemoveAssignment = { assignmentId: String ->
                                                        sectionAssignmentViewModel.removeSectionAssignment(assignmentId)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    // MINOR Subjects Section with Collapsible Dropdown
                                    if (minorSubjects.isNotEmpty()) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(20.dp)
                                                        .background(Color(0xFFFF9800), RoundedCornerShape(2.dp))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Minor Subjects",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF333333)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "(Cross-departmental - All teachers can apply)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF666666)
                                                )
                                            }
                                        }
                                        
                                        // Group MINOR subjects by year level NUMBER (not ID) for unified view
                                        val uniqueYearLevelNumbers = minorSubjects
                                            .mapNotNull { subject ->
                                                if (subject.yearLevelId.isNotBlank()) {
                                                    sectionYearLevels.find { it.id == subject.yearLevelId }?.level
                                                } else null
                                            }
                                            .distinct()
                                            .sorted()
                                        
                                        uniqueYearLevelNumbers.forEach { yearLevelNumber ->
                                            val yearLevel = sectionYearLevels.find { it.level == yearLevelNumber }
                                            val yearLevelMinorSubjects = minorSubjects.filter { subject ->
                                                if (subject.yearLevelId.isNotBlank()) {
                                                    sectionYearLevels.find { it.id == subject.yearLevelId }?.level == yearLevelNumber
                                                } else false
                                            }
                                            
                                            if (yearLevelMinorSubjects.isNotEmpty() && yearLevel != null) {
                                                item {
                                                    CollapsibleYearLevelCard(
                                                        yearLevelName = "Year ${yearLevel.level}",
                                                        yearLevelId = yearLevel.id,
                                                        subjects = yearLevelMinorSubjects,
                                                        sectionAssignments = sectionAssignments,
                                                        teacherApplications = sectionTeacherApplications,
                                                        onAssignTeacher = { subjectId: String, sectionName: String, teacherId: String ->
                                                            sectionAssignmentViewModel.assignTeacherToSection(subjectId, sectionName, teacherId)
                                                        },
                                                        onRemoveAssignment = { assignmentId: String ->
                                                            sectionAssignmentViewModel.removeSectionAssignment(assignmentId)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Handle MINOR subjects without year level
                                        val minorSubjectsWithoutYearLevel = minorSubjects.filter { subject ->
                                            subject.yearLevelId.isBlank()
                                        }
                                        
                                        if (minorSubjectsWithoutYearLevel.isNotEmpty()) {
                                            item {
                                                CollapsibleYearLevelCard(
                                                    yearLevelName = "No Year Level",
                                                    yearLevelId = "",
                                                    subjects = minorSubjectsWithoutYearLevel,
                                                    sectionAssignments = sectionAssignments,
                                                    teacherApplications = sectionTeacherApplications,
                                                    onAssignTeacher = { subjectId: String, sectionName: String, teacherId: String ->
                                                        sectionAssignmentViewModel.assignTeacherToSection(subjectId, sectionName, teacherId)
                                                    },
                                                    onRemoveAssignment = { assignmentId: String ->
                                                        sectionAssignmentViewModel.removeSectionAssignment(assignmentId)
                                                    }
                                                )
                                            }
                                        }
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
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedApplicationCard(
    application: TeacherApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Teacher Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Teacher",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFF9800)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = application.teacherName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = application.subjectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "Code: ${application.subjectCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999)
                        )
                    }
                }
                
                // Enhanced Status Badge
                Surface(
                    color = getStatusColor(application.status).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = getStatusIcon(application.status),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = getStatusColor(application.status)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = application.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = getStatusColor(application.status)
                        )
                    }
                }
            }
            
            // Application Date
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Date",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Applied on: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(application.appliedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Enhanced Action Buttons (only show for pending applications)
            if (application.status == ApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Approve",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFF2196F3)
        ApplicationStatus.APPROVED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFF44336)
        else -> Color(0xFF666666)
    }
}

private fun getStatusIcon(status: ApplicationStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        ApplicationStatus.PENDING -> Icons.Default.Schedule
        ApplicationStatus.APPROVED -> Icons.Default.CheckCircle
        ApplicationStatus.REJECTED -> Icons.Default.Cancel
        else -> Icons.Default.Help
    }
}
