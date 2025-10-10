package com.smartacademictracker.presentation.admin

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.SectionAssignment
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.presentation.common.LoadingStateCard

// Data class for year level grouping
data class YearLevelGroup(
    val yearLevelId: String,
    val yearLevelName: String,
    val subjects: List<Subject>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSectionAssignmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: TeacherSectionAssignmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val sectionAssignments by viewModel.sectionAssignments.collectAsState()
    val teacherApplications by viewModel.teacherApplications.collectAsState()

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Show loading state with label
        if (uiState.isLoading && subjects.isEmpty()) {
            LoadingStateCard(
                title = "Loading Teacher Assignments",
                message = "Please wait while we load subjects, section assignments, and teacher applications"
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Enhanced Header Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    tint = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Teacher Section Assignments",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Manage teacher assignments to subject sections",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            // Assignment Icon
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
                
                // Content Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Enhanced Information Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Only teachers who have applied for a subject can be assigned to its sections. Teachers must submit an application before they can be assigned.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                Spacer(modifier = Modifier.height(20.dp))

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
                                    text = "Loading assignments...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                    }
                } else if (uiState.error != null) {
                    // Enhanced Error State
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = uiState.error ?: "Unknown error",
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Enhanced Content with Collapsible Course Sections
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group subjects by course first, then by year level
                        // Filter out subjects with null or empty courseId to avoid grouping issues
                        val validSubjects = subjects.filter { 
                            it.courseId.isNotBlank() && it.courseName.isNotBlank()
                        }
                        val groupedByCourse = validSubjects.groupBy { it.courseId }
                        
                        // Also handle subjects with null/empty courseId separately if needed
                        val subjectsWithoutCourse = subjects.filter { 
                            it.courseId.isBlank() || it.courseName.isBlank()
                        }
                        
                        groupedByCourse.forEach { (courseId, courseSubjects) ->
                            // Get course name from first subject
                            val courseName = courseSubjects.firstOrNull()?.courseName ?: "Unknown Course"
                            
                            // Group subjects within this course by year level
                            // Include all subjects, even those without yearLevelId
                            val groupedByYearLevel = courseSubjects.groupBy { 
                                it.yearLevelId.ifBlank { "NO_YEAR_LEVEL" }
                            }
                            
                            // Only show course card if it has at least one subject
                            if (courseSubjects.isNotEmpty()) {
                                item {
                                    CollapsibleCourseCard(
                                        courseName = courseName,
                                        courseId = courseId,
                                        yearLevelGroups = groupedByYearLevel.map { (yearLevelId, yearSubjects) ->
                                            YearLevelGroup(
                                                yearLevelId = if (yearLevelId == "NO_YEAR_LEVEL") "" else yearLevelId,
                                                yearLevelName = if (yearLevelId == "NO_YEAR_LEVEL") {
                                                    "No Year Level"
                                                } else {
                                                    yearSubjects.firstOrNull()?.yearLevelName ?: "Unknown Year"
                                                },
                                                subjects = yearSubjects
                                            )
                                        },
                                        sectionAssignments = sectionAssignments,
                                        teacherApplications = teacherApplications,
                                        onAssignTeacher = { subjectId: String, sectionName: String, teacherId: String ->
                                            viewModel.assignTeacherToSection(subjectId, sectionName, teacherId)
                                        },
                                        onRemoveAssignment = { assignmentId: String ->
                                            viewModel.removeSectionAssignment(assignmentId)
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Handle subjects without valid courseId (shouldn't happen, but just in case)
                        if (subjectsWithoutCourse.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Warning: ${subjectsWithoutCourse.size} subject(s) without valid course information",
                                        modifier = Modifier.padding(16.dp),
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
    }
    }
}

@Composable
fun EnhancedSubjectSectionCard(
    subject: Subject,
    sectionAssignments: List<SectionAssignment>,
    teacherApplications: List<TeacherApplication>,
    onAssignTeacher: (String, String, String) -> Unit,
    onRemoveAssignment: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced Subject Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "Subject",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "Code: ${subject.code} | Sections: ${subject.numberOfSections}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Enhanced Teacher Applications Section
            if (teacherApplications.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Applications",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Teacher Applications (${teacherApplications.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        teacherApplications.forEach { application ->
                            EnhancedTeacherApplicationCard(
                                application = application,
                                availableSections = subject.sections,
                                assignedSections = sectionAssignments.map { it.sectionName },
                                onAssign = { sectionName: String -> 
                                    onAssignTeacher(subject.id, sectionName, application.teacherId) 
                                }
                            )
                        }
                    }
                }
            }

            // Enhanced Sections List
            subject.sections.forEach { sectionName ->
                val assignment = sectionAssignments.find { it.sectionName == sectionName }
                
                EnhancedSectionStatusCard(
                    sectionName = sectionName,
                    assignment = assignment,
                    onAssignTeacher = { teacherId: String ->
                        onAssignTeacher(subject.id, sectionName, teacherId)
                    },
                    onRemoveAssignment = {
                        assignment?.let { onRemoveAssignment(it.id) }
                    }
                )
            }
        }
    }
}

@Composable
fun EnhancedSectionStatusCard(
    sectionName: String,
    assignment: SectionAssignment?,
    onAssignTeacher: (String) -> Unit,
    onRemoveAssignment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (assignment != null) {
                Color(0xFFE8F5E8)
            } else {
                Color(0xFFF5F5F5)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Section Icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (assignment != null) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFF666666).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (assignment != null) Icons.Default.Person else Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (assignment != null) Color(0xFF4CAF50) else Color(0xFF666666)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = sectionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = if (assignment != null) "Assigned" else "Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (assignment != null) Color(0xFF4CAF50) else Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    color = if (assignment != null) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFF666666).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (assignment != null) "ASSIGNED" else "AVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (assignment != null) Color(0xFF4CAF50) else Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (assignment != null) {
                // Enhanced Assigned Teacher Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Teacher",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Teacher: ${assignment.teacherName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Email: ${assignment.teacherEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                // Enhanced Remove Button
                Button(
                    onClick = onRemoveAssignment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Assignment", fontWeight = FontWeight.Bold)
                }
            } else {
                // Enhanced Available State
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No teacher assigned to this section",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedTeacherApplicationCard(
    application: TeacherApplication,
    availableSections: List<String>,
    assignedSections: List<String>,
    onAssign: (String) -> Unit
) {
    var showSectionDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F8FF)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                            Icons.Default.Person,
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
                            text = application.teacherEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                // Enhanced Status Chip
                EnhancedApplicationStatusChip(status = application.status)
            }
            
            // Enhanced Application Reason
            if (application.applicationReason.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Reason",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reason:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = application.applicationReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
            
            // Enhanced Action Button based on status
            when (application.status) {
                ApplicationStatus.APPROVED -> {
                    Button(
                        onClick = { showSectionDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = availableSections.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assign to Section", fontWeight = FontWeight.Bold)
                    }
                }
                ApplicationStatus.PENDING -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Pending",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Application pending approval. Please approve the application first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                ApplicationStatus.REJECTED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Rejected",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Application rejected. Cannot assign to section.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                ApplicationStatus.WITHDRAWN -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Withdrawn",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Application withdrawn by teacher.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            if (availableSections.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No available sections for assignment",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    // Enhanced Section Selection Dialog
    if (showSectionDialog) {
        EnhancedSectionSelectionDialog(
            teacherName = application.teacherName,
            availableSections = availableSections,
            assignedSections = assignedSections,
            onSectionSelected = { sectionName: String ->
                onAssign(sectionName)
                showSectionDialog = false
            },
            onDismiss = { showSectionDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelectionDialog(
    teacherName: String,
    availableSections: List<String>,
    assignedSections: List<String>,
    onSectionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Assign Teacher to Section")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select a section to assign $teacherName:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (availableSections.isEmpty()) {
                    Text(
                        text = "No available sections for assignment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    availableSections.forEach { sectionName ->
                        val isAssigned = assignedSections.contains(sectionName)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAssigned) 
                                    MaterialTheme.colorScheme.errorContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            onClick = {
                                if (!isAssigned) {
                                    onSectionSelected(sectionName)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sectionName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (isAssigned) {
                                    Text(
                                        text = "Already Assigned",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EnhancedCourseYearLevelHeader(
    courseName: String,
    yearLevelName: String,
    subjectCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Course Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Course",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = yearLevelName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            // Subject Count Badge
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$subjectCount subject${if (subjectCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun EnhancedApplicationStatusChip(status: ApplicationStatus) {
    val (backgroundColor, contentColor, icon) = when (status) {
        ApplicationStatus.PENDING -> Triple(Color(0xFF2196F3).copy(alpha = 0.1f), Color(0xFF2196F3), Icons.Default.Schedule)
        ApplicationStatus.APPROVED -> Triple(Color(0xFF4CAF50).copy(alpha = 0.1f), Color(0xFF4CAF50), Icons.Default.CheckCircle)
        ApplicationStatus.REJECTED -> Triple(Color(0xFFF44336).copy(alpha = 0.1f), Color(0xFFF44336), Icons.Default.Cancel)
        ApplicationStatus.WITHDRAWN -> Triple(Color(0xFF666666).copy(alpha = 0.1f), Color(0xFF666666), Icons.Default.Undo)
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.displayName,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSectionSelectionDialog(
    teacherName: String,
    availableSections: List<String>,
    assignedSections: List<String>,
    onSectionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = "Assign",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Assign Teacher to Section",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Teacher",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select a section to assign $teacherName:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (availableSections.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No available sections for assignment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    availableSections.forEach { sectionName ->
                        val isAssigned = assignedSections.contains(sectionName)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAssigned) 
                                    Color(0xFFFFEBEE) 
                                else 
                                    Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                if (!isAssigned) {
                                    onSectionSelected(sectionName)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isAssigned) Icons.Default.Block else Icons.Default.Assignment,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isAssigned) Color(0xFFF44336) else Color(0xFF666666)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = sectionName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAssigned) Color(0xFFD32F2F) else Color(0xFF333333)
                                    )
                                }
                                
                                if (isAssigned) {
                                    Surface(
                                        color = Color(0xFFF44336).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Already Assigned",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF666666))
            ) {
                Text("Cancel", fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
fun CollapsibleCourseCard(
    courseName: String,
    courseId: String,
    yearLevelGroups: List<YearLevelGroup>,
    sectionAssignments: List<SectionAssignment>,
    teacherApplications: List<TeacherApplication>,
    onAssignTeacher: (String, String, String) -> Unit,
    onRemoveAssignment: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isCourseNameExpanded by remember { mutableStateOf(false) }
    
    // Calculate total subjects across all year levels
    val totalSubjects = yearLevelGroups.sumOf { it.subjects.size }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Course Header (Expandable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                onClick = { isExpanded = !isExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Course name and info - can shrink if needed
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Course Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCourseNameExpanded = !isCourseNameExpanded }
                        ) {
                            Text(
                                text = courseName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = if (isCourseNameExpanded) Int.MAX_VALUE else 2,
                                overflow = if (isCourseNameExpanded) androidx.compose.ui.text.style.TextOverflow.Visible else androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${yearLevelGroups.size} year level${if (yearLevelGroups.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    // Badge and Dropdown - Fixed width, always visible
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Subject Count Badge - Always show, even if 0
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "$totalSubjects subject${if (totalSubjects != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Dropdown Icon - Always show
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Collapsible Content - Year Levels
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    yearLevelGroups.forEach { yearLevelGroup ->
                        CollapsibleYearLevelCard(
                            yearLevelName = yearLevelGroup.yearLevelName,
                            yearLevelId = yearLevelGroup.yearLevelId,
                            subjects = yearLevelGroup.subjects,
                            sectionAssignments = sectionAssignments,
                            teacherApplications = teacherApplications,
                            onAssignTeacher = onAssignTeacher,
                            onRemoveAssignment = onRemoveAssignment
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleYearLevelCard(
    yearLevelName: String,
    yearLevelId: String,
    subjects: List<Subject>,
    sectionAssignments: List<SectionAssignment>,
    teacherApplications: List<TeacherApplication>,
    onAssignTeacher: (String, String, String) -> Unit,
    onRemoveAssignment: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column {
            // Year Level Header (Expandable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF66BB6A)),
                onClick = { isExpanded = !isExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Year Level Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = yearLevelName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${subjects.size} subject${if (subjects.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    // Dropdown Icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Collapsible Content - Subjects
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subject ->
                        EnhancedSubjectSectionCard(
                            subject = subject,
                            sectionAssignments = sectionAssignments.filter { it.subjectId == subject.id },
                            teacherApplications = teacherApplications.filter { it.subjectId == subject.id },
                            onAssignTeacher = onAssignTeacher,
                            onRemoveAssignment = onRemoveAssignment
                        )
                    }
                }
            }
        }
    }
}
