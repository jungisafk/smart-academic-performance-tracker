package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSubjectsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToGradeInput: (String) -> Unit = {},
    viewModel: TeacherSubjectsViewModel = hiltViewModel(),
    showBackButton: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val mySubjects by viewModel.mySubjects.collectAsState()
    val appliedSubjects by viewModel.appliedSubjects.collectAsState()
    val applications by viewModel.applications.collectAsState()
    val approvedApplications by viewModel.approvedApplications.collectAsState()


    // Load subjects when screen is composed, using ViewModel as key to prevent unnecessary reloads
    LaunchedEffect(viewModel) {
        viewModel.loadSubjects(forceRefresh = false)
    }

    // Clear success message after 3 seconds
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccessMessage()
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    text = "My Subjects",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.refreshSubjects() },
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Refresh",
                        tint = Color(0xFF666666)
                    )
                }
            }
            
            // Summary Card
            if (!uiState.isLoading && (mySubjects.isNotEmpty() || applications.isNotEmpty() || availableSubjects.isNotEmpty())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = "Subject Management",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append("${mySubjects.size} Assigned")
                                    append(" • ")
                                    append("${applications.size} Applied")
                                    append(" • ")
                                    append("${availableSubjects.size} Available")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        // Subject Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
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
            if (uiState.isLoading) {
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
                            text = "Loading subjects...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                // Tab Row for switching between My Subjects, Applied, and Available Subjects
                var selectedTab by remember { mutableStateOf(0) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF2196F3)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { 
                                Text(
                                    "My Subjects",
                                    color = if (selectedTab == 0) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { 
                                Text(
                                    "Applied",
                                    color = if (selectedTab == 1) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { 
                                Text(
                                    "Available",
                                    color = if (selectedTab == 2) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // My Subjects - Only show actually assigned subjects
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2196F3)
                                )
                            }
                        } else if (mySubjects.isEmpty()) {
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
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFF2196F3)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No subjects assigned",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "You haven't been assigned to any subjects yet. Apply for available subjects to get started.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(mySubjects) { subject ->
                                    MySubjectCardWithSections(
                                        subject = subject,
                                        onNavigateToGradeInput = { onNavigateToGradeInput(subject.id) },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Applied Subjects - Show all applications (pending, approved, rejected)
                        if (applications.isEmpty()) {
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
                                        Icons.Default.Assignment,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFF2196F3)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No applications submitted",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "You haven't applied for any subjects yet. Browse available subjects to apply.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Show pending applications with cancel option
                                items(applications.filter { it.status == ApplicationStatus.PENDING }) { application ->
                                    ApplicationCardWithCancel(
                                        application = application,
                                        onCancel = { 
                                            viewModel.cancelApplication(application.id) 
                                        }
                                    )
                                }
                                
                                // Show approved applications waiting for section assignment
                                items(approvedApplications) { application ->
                                    ApprovedApplicationCard(application = application)
                                }
                                
                                // Show rejected applications
                                items(applications.filter { it.status == ApplicationStatus.REJECTED }) { application ->
                                    ApplicationCardWithCancel(
                                        application = application,
                                        onCancel = { } // No cancel for rejected
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Available Subjects
                        if (availableSubjects.isEmpty()) {
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
                                        Icons.Default.Assignment,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFF2196F3)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No available subjects",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "There are currently no subjects available for application.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            items(availableSubjects) { subject ->
                                var sectionAvailability by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
                                
                                LaunchedEffect(subject.id) {
                                    sectionAvailability = viewModel.getSectionAvailability(subject.id)
                                }
                                
                                AvailableSubjectCard(
                                    subject = subject,
                                    onApply = { 
                                        viewModel.applyForSubject(subject.id) 
                                    },
                                    isApplying = uiState.applyingSubjects.contains(subject.id),
                                    hasApplied = appliedSubjects.any { it.id == subject.id },
                                    sectionAvailability = sectionAvailability
                                )
                            }
                        }
                    }
                }
            }
        }

            // Success Message
            uiState.successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = message,
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearSuccessMessage() }
                        ) {
                            Text(
                                "Dismiss",
                                color = Color(0xFF4CAF50)
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

@Composable
fun MySubjectCard(
    subject: Subject,
    onNavigateToGradeInput: () -> Unit,
    viewModel: TeacherSubjectsViewModel = hiltViewModel()
) {
    var studentCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(subject.id) {
        studentCount = viewModel.getStudentCountForSubject(subject.id)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subject.description.isNotBlank()) {
                        Text(
                            text = subject.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "ASSIGNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
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
                    text = "Credits: ${subject.credits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Semester: ${subject.semester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grade Input Button
            Button(
                onClick = onNavigateToGradeInput,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = "Input Grades",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Input Grades")
            }
        }
    }
}

@Composable
fun AppliedSubjectCard(
    subject: Subject
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subject.description.isNotBlank()) {
                        Text(
                            text = subject.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "APPLIED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
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
                    text = "Credits: ${subject.credits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Semester: ${subject.semester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Status: Pending Admin Review",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun AvailableSubjectCard(
    subject: Subject,
    onApply: () -> Unit,
    isApplying: Boolean,
    hasApplied: Boolean = false,
    sectionAvailability: Map<String, Boolean> = emptyMap()
) {
    var showSections by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subject.yearLevelName.isNotBlank()) {
                        Text(
                            text = "Year Level: ${subject.yearLevelName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (subject.sections.isNotEmpty()) {
                        Text(
                            text = "Sections: ${subject.sections.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (subject.description.isNotBlank()) {
                        Text(
                            text = subject.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Credits: ${subject.credits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Semester: ${subject.semester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Sections expandable section
            if (subject.sections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sections (${subject.sections.size}): ${subject.sections.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showSections = !showSections },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showSections) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showSections) "Hide sections" else "Show sections",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Expandable sections list
                if (showSections) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Available Sections:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                subject.sections.forEach { section ->
                                    val isAvailable = sectionAvailability[section] ?: true
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = section,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Surface(
                                            color = if (isAvailable) 
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else 
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = if (isAvailable) "Available" else "Assigned",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isAvailable) 
                                                    MaterialTheme.colorScheme.primary
                                                else 
                                                    MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Apply Button or Status
            if (hasApplied) {
                // Show applied status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Already Applied",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Show apply button
                Button(
                    onClick = {
                        onApply()
                    },
                    enabled = !isApplying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Applying...")
                    } else {
                        Text("Apply for Subject")
                    }
                }
            }
        }
    }
}

