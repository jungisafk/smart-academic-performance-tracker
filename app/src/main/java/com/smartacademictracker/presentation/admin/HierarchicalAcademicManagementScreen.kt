package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.presentation.common.NoActiveAcademicPeriodCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalAcademicManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCourse: () -> Unit,
    onNavigateToAddYearLevel: (String) -> Unit, // courseId parameter
    onNavigateToAddSubject: (String, String) -> Unit, // courseId, yearLevelId parameters
    onNavigateToAddMinorSubject: (String) -> Unit, // yearLevelId parameter for MINOR subjects
    onNavigateToEditCourse: (String) -> Unit = {},
    onNavigateToEditYearLevel: (String) -> Unit = {},
    onNavigateToEditSubject: (String) -> Unit = {},
    onNavigateToAcademicPeriods: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HierarchicalAcademicManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val yearLevels by viewModel.yearLevels.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadAllData()
        // Clean up orphaned year levels (those with empty courseId)
        viewModel.cleanupOrphanedYearLevels()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Add Course Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Academic Structure",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = onNavigateToAddCourse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Add Course",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Course",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Show loading state
                if (uiState.isLoading) {
                    LoadingAcademicStructureState()
                } else {
                    // Check for error state (no active academic period)
                    val error = uiState.error
                    if (error != null && error.contains("No active academic period")) {
                        NoActiveAcademicPeriodCard(
                            onCreateAcademicPeriod = onNavigateToAcademicPeriods
                        )
                    } else if (courses.isEmpty()) {
                        EnhancedEmptyCoursesState()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Courses Section (MAJOR subjects)
                            items(courses) { course ->
                                val filteredYearLevels = yearLevels.filter { it.courseId == course.id }
                                EnhancedCourseHierarchyCard(
                                    course = course,
                                    yearLevels = filteredYearLevels,
                                    // Only show MAJOR subjects in courses (filter out MINOR subjects)
                                    subjects = subjects.filter { 
                                        it.subjectType == com.smartacademictracker.data.model.SubjectType.MAJOR 
                                    },
                                    onDeleteCourse = { viewModel.deleteCourse(course.id) },
                                    onAddYearLevel = { 
                                        
                                        onNavigateToAddYearLevel(course.id) 
                                    },
                                    onEditYearLevel = onNavigateToEditYearLevel,
                                    onDeleteYearLevel = { viewModel.deleteYearLevel(it) },
                                    onAddSubject = { yearLevelId -> 
                                        
                                        onNavigateToAddSubject(course.id, yearLevelId) 
                                    },
                                    onEditSubject = onNavigateToEditSubject,
                                    onDeleteSubject = { viewModel.deleteSubject(it) }
                                )
                            }
                            
                            // MINOR Subjects Section (separate from courses)
                            item {
                                val minorSubjects = subjects.filter { 
                                    it.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR 
                                }
                                
                                if (minorSubjects.isNotEmpty() || yearLevels.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // MINOR Subjects Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 8.dp),
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
                                    
                                    // Group MINOR subjects by year level NUMBER (not ID) for unified view
                                    // This ensures we show one card per year level (Year 1, Year 2, etc.) regardless of course
                                    val minorSubjectsByYearLevelNumber = minorSubjects.groupBy { subject ->
                                        // Find the year level for this subject to get its level number
                                        yearLevels.find { it.id == subject.yearLevelId }?.level ?: 0
                                    }
                                    
                                    // Get unique year level numbers and create a unified view
                                    val uniqueYearLevelNumbers = yearLevels.map { it.level }.distinct().sorted()
                                    
                                    // Create a map of level number to a representative year level (for display)
                                    val yearLevelByNumber = yearLevels.associateBy { it.level }
                                    
                                    uniqueYearLevelNumbers.forEachIndexed { index, levelNumber ->
                                        val representativeYearLevel = yearLevelByNumber[levelNumber]
                                        if (representativeYearLevel != null) {
                                            // Get all MINOR subjects for this year level number (across all courses)
                                            val yearLevelMinorSubjects = minorSubjectsByYearLevelNumber[levelNumber] ?: emptyList()
                                            
                                            // Use the first year level ID with this level number for adding subjects
                                            val firstYearLevelWithNumber = yearLevels.firstOrNull { it.level == levelNumber }
                                            if (firstYearLevelWithNumber != null) {
                                                if (index > 0) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                                MinorSubjectYearLevelCard(
                                                    yearLevel = representativeYearLevel,
                                                    subjects = yearLevelMinorSubjects,
                                                    onAddSubject = { 
                                                        onNavigateToAddMinorSubject(firstYearLevelWithNumber.id)
                                                    },
                                                    onEditSubject = onNavigateToEditSubject,
                                                    onDeleteSubject = { viewModel.deleteSubject(it) }
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
    }
}

@Composable
fun LoadingAcademicStructureState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Loading indicator with background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color(0xFF2196F3),
                    strokeWidth = 3.dp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Loading Academic Structure",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please wait while we load your courses, year levels, and subjects",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EnhancedEmptyCoursesState() {
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
                    Icons.Default.School,
                    contentDescription = "No Courses",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "No courses found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add your first course to start building the academic structure",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick action button
            OutlinedButton(
                onClick = { /* Add course action */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Course",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Course")
            }
        }
    }
}

@Composable
fun EnhancedCourseHierarchyCard(
    course: Course,
    yearLevels: List<YearLevel>,
    subjects: List<Subject>,
    onDeleteCourse: () -> Unit,
    onAddYearLevel: () -> Unit,
    onEditYearLevel: (String) -> Unit,
    onDeleteYearLevel: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onEditSubject: (String) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    // Use remember with key to persist state across recompositions
    // Default to false (collapsed) so cards don't auto-expand when scrolling
    var expanded by remember(course.id) { mutableStateOf(false) }
    var isCourseNameExpanded by remember(course.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Course Header - Fixed Rows Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left side: Course info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // Course Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "Course",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF2196F3)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 0.dp)
                    ) {
                        // Row 1 & 2: Course Name (clickable to expand)
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            maxLines = if (isCourseNameExpanded) Int.MAX_VALUE else 2,
                            overflow = if (isCourseNameExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCourseNameExpanded = !isCourseNameExpanded }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Row 3: Details (Code and Year Levels) - Fixed layout to prevent cutoff
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Code: ${course.code}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Icon(
                                    Icons.Default.Class,
                                    contentDescription = "Year Levels",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${yearLevels.size} year level(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                }
                
                // Right side: Action Buttons (only expand/collapse and delete)
                Row {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = Color(0xFF666666)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onDeleteCourse,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete Course",
                            tint = Color(0xFFF44336)
                        )
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
                    yearLevels.sortedBy { it.level }.forEachIndexed { index, yearLevel ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        YearLevelHierarchyCard(
                            yearLevel = yearLevel,
                            // Only show MAJOR subjects in courses (filter out MINOR subjects)
                            subjects = subjects.filter { 
                                it.yearLevelId == yearLevel.id && 
                                it.subjectType == com.smartacademictracker.data.model.SubjectType.MAJOR 
                            },
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

    // Color scheme based on year level for distinction
    val yearLevelColors = listOf(
        Color(0xFFE3F2FD) to Color(0xFF1976D2), // Year 1 - Light Blue
        Color(0xFFF3E5F5) to Color(0xFF7B1FA2), // Year 2 - Light Purple
        Color(0xFFFFF3E0) to Color(0xFFE65100), // Year 3 - Light Orange
        Color(0xFFE8F5E9) to Color(0xFF388E3C), // Year 4 - Light Green
        Color(0xFFFFEBEE) to Color(0xFFC62828), // Year 5+ - Light Red
    )
    
    val (backgroundColor, accentColor) = yearLevelColors.getOrElse((yearLevel.level - 1).coerceIn(0, yearLevelColors.size - 1)) {
        Color(0xFFF5F5F5) to Color(0xFF757575) // Default gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.3f)
        )
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
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Year ${yearLevel.level} - ${yearLevel.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = "Subjects",
                                modifier = Modifier.size(14.dp),
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${subjects.size} subject(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row {
                    IconButton(
                        onClick = onEditYearLevel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Year Level",
                            modifier = Modifier.size(18.dp),
                            tint = accentColor
                        )
                    }
                    IconButton(
                        onClick = onDeleteYearLevel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Year Level",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFD32F2F)
                        )
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
fun MinorSubjectYearLevelCard(
    yearLevel: YearLevel,
    subjects: List<Subject>,
    onAddSubject: () -> Unit,
    onEditSubject: (String) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Color scheme based on year level for distinction (same as major subjects)
    val yearLevelColors = listOf(
        Color(0xFFE3F2FD) to Color(0xFF1976D2), // Year 1 - Light Blue
        Color(0xFFF3E5F5) to Color(0xFF7B1FA2), // Year 2 - Light Purple
        Color(0xFFFFF3E0) to Color(0xFFE65100), // Year 3 - Light Orange
        Color(0xFFE8F5E9) to Color(0xFF388E3C), // Year 4 - Light Green
        Color(0xFFFFEBEE) to Color(0xFFC62828), // Year 5+ - Light Red
    )
    
    val (backgroundColor, accentColor) = yearLevelColors.getOrElse((yearLevel.level - 1).coerceIn(0, yearLevelColors.size - 1)) {
        Color(0xFFF5F5F5) to Color(0xFF757575) // Default gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.3f)
        )
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
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Year ${yearLevel.level}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = "Minor Subjects",
                                modifier = Modifier.size(14.dp),
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${subjects.size} minor subject(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                        text = "Minor Subjects",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Button(
                        onClick = onAddSubject,
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Minor Subject", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Minor Subject", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (subjects.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No minor subjects",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor
                            )
                            Text(
                                text = "Add cross-departmental subjects for this year level",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor.copy(alpha = 0.7f)
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
    onDelete: () -> Unit,
    onViewSections: () -> Unit = {}
) {
    var showSections by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    if (subject.sections.isNotEmpty()) {
                        Text(
                            text = "Sections: ${subject.sections.size} (${subject.sections.joinToString(", ")})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row {
                    if (subject.sections.isNotEmpty()) {
                        IconButton(onClick = { showSections = !showSections }) {
                            Icon(
                                if (showSections) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showSections) "Hide Sections" else "Show Sections",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Subject", modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Subject", modifier = Modifier.size(14.dp))
                    }
                }
            }
            
            // Expandable Sections Section
            if (showSections && subject.sections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Subject Sections",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            subject.sections.forEach { section ->
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
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
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
    }
}
