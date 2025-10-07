package com.smartacademictracker.presentation.admin

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalAcademicManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCourse: () -> Unit,
    onNavigateToAddYearLevel: (String) -> Unit, // courseId parameter
    onNavigateToAddSubject: (String, String) -> Unit, // courseId, yearLevelId parameters
    onNavigateToEditCourse: (String) -> Unit = {},
    onNavigateToEditYearLevel: (String) -> Unit = {},
    onNavigateToEditSubject: (String) -> Unit = {},
    viewModel: HierarchicalAcademicManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val yearLevels by viewModel.yearLevels.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAllData()
        // Clean up orphaned year levels (those with empty courseId)
        viewModel.cleanupOrphanedYearLevels()
    }
    
    // Refresh data when screen comes back into focus
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // Refresh data when returning from other screens
    DisposableEffect(Unit) {
        onDispose {
            viewModel.refreshData()
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
                text = "Academic Structure",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { 
                viewModel.refreshData()
                println("DEBUG: Manual refresh triggered")
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = { 
                println("DEBUG: Migration button clicked")
                viewModel.runMigration()
            }) {
                Icon(Icons.Default.Settings, contentDescription = "Migrate Database")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Course Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Academic Structure",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onNavigateToAddCourse) {
                Icon(Icons.Default.Add, contentDescription = "Add Course")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Course")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hierarchical Structure
        if (courses.isEmpty()) {
            EmptyCoursesState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(courses) { course ->
                    val filteredYearLevels = yearLevels.filter { it.courseId == course.id }
                    println("DEBUG: Course ${course.name} (${course.id}) has ${filteredYearLevels.size} year levels")
                    filteredYearLevels.forEach { yearLevel ->
                        println("DEBUG: Year Level for ${course.name}: ${yearLevel.name} (${yearLevel.id})")
                    }
                    CourseHierarchyCard(
                        course = course,
                        yearLevels = filteredYearLevels,
                        subjects = subjects,
                        onEditCourse = { onNavigateToEditCourse(course.id) },
                        onDeleteCourse = { viewModel.deleteCourse(course.id) },
                        onAddYearLevel = { 
                            println("DEBUG: HierarchicalAcademicManagementScreen - Navigating to add year level for course: '${course.id}'")
                            onNavigateToAddYearLevel(course.id) 
                        },
                        onEditYearLevel = onNavigateToEditYearLevel,
                        onDeleteYearLevel = { viewModel.deleteYearLevel(it) },
                        onAddSubject = { yearLevelId -> 
                            println("DEBUG: HierarchicalAcademicManagementScreen - Navigating to add subject for course: '${course.id}', yearLevel: '$yearLevelId'")
                            onNavigateToAddSubject(course.id, yearLevelId) 
                        },
                        onEditSubject = onNavigateToEditSubject,
                        onDeleteSubject = { viewModel.deleteSubject(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCoursesState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = "No Courses",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No courses found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add your first course to start building the academic structure",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CourseHierarchyCard(
    course: Course,
    yearLevels: List<YearLevel>,
    subjects: List<Subject>,
    onEditCourse: () -> Unit,
    onDeleteCourse: () -> Unit,
    onAddYearLevel: () -> Unit,
    onEditYearLevel: (String) -> Unit,
    onDeleteYearLevel: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onEditSubject: (String) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Course Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    Column {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Code: ${course.code}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${yearLevels.size} year level(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEditCourse) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Course")
                    }
                    IconButton(onClick = onDeleteCourse) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Course")
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add Year Level Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Year Levels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onAddYearLevel,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Year Level", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Year Level", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (yearLevels.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No year levels",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add year levels to organize subjects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Year Levels List
                    yearLevels.sortedBy { it.level }.forEach { yearLevel ->
                        YearLevelHierarchyCard(
                            yearLevel = yearLevel,
                            subjects = subjects.filter { it.yearLevelId == yearLevel.id },
                            onEditYearLevel = { onEditYearLevel(yearLevel.id) },
                            onDeleteYearLevel = { onDeleteYearLevel(yearLevel.id) },
                            onAddSubject = { onAddSubject(yearLevel.id) },
                            onEditSubject = onEditSubject,
                            onDeleteSubject = onDeleteSubject
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YearLevelHierarchyCard(
    yearLevel: YearLevel,
    subjects: List<Subject>,
    onEditYearLevel: () -> Unit,
    onDeleteYearLevel: () -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (String) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Year Level Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    Column {
                        Text(
                            text = "Year ${yearLevel.level} - ${yearLevel.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${subjects.size} subject(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEditYearLevel) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Year Level", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteYearLevel) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Year Level", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add Subject Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subjects",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onAddSubject,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subject", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (subjects.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No subjects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add subjects for this year level",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Subjects List
                    subjects.sortedBy { it.name }.forEach { subject ->
                        SubjectHierarchyCard(
                            subject = subject,
                            onEdit = { onEditSubject(subject.id) },
                            onDelete = { onDeleteSubject(subject.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectHierarchyCard(
    subject: Subject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Code: ${subject.code} | Credits: ${subject.credits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                subject.semester?.let { semester ->
                    Text(
                        text = "Semester: $semester",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Subject", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Subject", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
