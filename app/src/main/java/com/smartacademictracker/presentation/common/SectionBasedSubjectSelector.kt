package com.smartacademictracker.presentation.common

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
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject

@Composable
fun SectionBasedSubjectSelector(
    courses: List<Course>,
    yearLevels: List<YearLevel>,
    subjects: List<Subject>,
    selectedCourseId: String?,
    selectedYearLevelId: String?,
    sectionAssignments: List<com.smartacademictracker.data.model.SectionAssignment>,
    onCourseSelected: (String?) -> Unit,
    onYearLevelSelected: (String?) -> Unit,
    onSubjectWithSectionSelected: (Subject, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Course Selection
        Text(
            text = if (courses.size == 1) "Your Course" else "Select Course",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Add scroll indicator text for courses
        if (courses.size > 3) {
            Text(
                text = "📜 Scroll to see more courses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(courses) { course ->
                CourseSelectionCard(
                    course = course,
                    isSelected = course.id == selectedCourseId,
                    onClick = { onCourseSelected(if (course.id == selectedCourseId) null else course.id) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Year Level Selection
        if (selectedCourseId != null) {
            Text(
                text = if (yearLevels.size == 1) "Your Year Level" else "Select Year Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val filteredYearLevels = yearLevels.filter { it.courseId == selectedCourseId }
            
            // Add scroll indicator text for year levels
            if (filteredYearLevels.size > 3) {
                Text(
                    text = "📜 Scroll to see more year levels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredYearLevels) { yearLevel ->
                    YearLevelSelectionCard(
                        yearLevel = yearLevel,
                        isSelected = yearLevel.id == selectedYearLevelId,
                        onClick = { onYearLevelSelected(if (yearLevel.id == selectedYearLevelId) null else yearLevel.id) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Subject Selection with Sections
        if (selectedCourseId != null && selectedYearLevelId != null) {
            Text(
                text = "Select Subject and Section",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val filteredSubjects = subjects.filter { 
                it.courseId == selectedCourseId && it.yearLevelId == selectedYearLevelId 
            }
            
            if (filteredSubjects.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No subjects available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSubjects) { subject ->
                        SubjectWithSectionsCard(
                            subject = subject,
                            sectionAssignments = sectionAssignments,
                            onSectionSelected = { sectionName ->
                                onSubjectWithSectionSelected(subject, sectionName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSelectionCard(
    course: Course,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF333333)
                )
                Text(
                    text = course.code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun YearLevelSelectionCard(
    yearLevel: YearLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = yearLevel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF333333)
                )
                Text(
                    text = "Level ${yearLevel.level}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun SubjectWithSectionsCard(
    subject: Subject,
    sectionAssignments: List<com.smartacademictracker.data.model.SectionAssignment>,
    onSectionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFF333333)
                    )
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color(0xFF666666)
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = androidx.compose.ui.graphics.Color(0xFF1976D2)
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Available Sections:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Filter sections to only show those with assigned teachers
                // First check sectionAssignments, then fallback to subject.teacherId
                val sectionsWithTeachers = if (sectionAssignments.isNotEmpty()) {
                    // Use section assignments if available
                    subject.sections.filter { sectionName ->
                        sectionAssignments.any { assignment ->
                            assignment.subjectId == subject.id && 
                            assignment.sectionName == sectionName &&
                            assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                        }
                    }
                } else if (subject.teacherId != null && subject.teacherId.isNotEmpty()) {
                    // Fallback: If subject has a teacher assigned, show all sections
                    subject.sections
                } else {
                    // No teacher assigned
                    emptyList()
                }
                
                if (sectionsWithTeachers.isEmpty()) {
                    Text(
                        text = "No sections with assigned teachers available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sectionsWithTeachers.forEach { sectionName ->
                        SectionSelectionButton(
                            sectionName = sectionName,
                            onClick = { onSectionSelected(sectionName) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSelectionButton(
    sectionName: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFE8F5E8)
        )
    ) {
        Icon(
            Icons.Default.Class,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Apply to $sectionName",
            color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
            fontWeight = FontWeight.Medium
        )
    }
}
