package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGradeStatusScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminGradeStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradeStatuses by viewModel.gradeStatuses.collectAsState()

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadGradeStatus()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Show loading indicator only when no data exists yet
            if (uiState.isLoading && gradeStatuses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2196F3),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Loading grade status...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Summary Card
                item {
                    SummaryCard(uiState = uiState)
                }

                // Separate minor subjects from major subjects
                val minorSubjects = gradeStatuses.filter { it.isMinorSubject }
                val majorSubjects = gradeStatuses.filter { !it.isMinorSubject }

                // Display Minor Subjects Section (separate from courses)
                if (minorSubjects.isNotEmpty()) {
                    item {
                        MinorSubjectsHeader()
                    }
                    
                    // Group minor subjects by subject name
                    val groupedMinorBySubject = minorSubjects.groupBy { it.subjectName }
                    
                    groupedMinorBySubject.forEach { (subjectName, subjectStatuses) ->
                        subjectStatuses.forEach { status ->
                            item {
                                SubjectSectionStatusCard(status = status)
                            }
                        }
                    }
                }

                // Display Major Subjects grouped by course
                val groupedByCourse = majorSubjects.groupBy { it.courseName }
                
                groupedByCourse.forEach { (courseName, courseStatuses) ->
                    item {
                        CourseHeader(courseName = courseName)
                    }
                    
                    // Group by subject within course
                    val groupedBySubject = courseStatuses.groupBy { it.subjectName }
                    
                    groupedBySubject.forEach { (subjectName, subjectStatuses) ->
                        subjectStatuses.forEach { status ->
                            item {
                                SubjectSectionStatusCard(status = status)
                            }
                        }
                    }
                }

                // Empty state
                if (gradeStatuses.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyGradeStatusCard()
                    }
                }
            }
    }
    
    // Error Message - outside LazyColumn
    uiState.error?.let { error ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(uiState: AdminGradeStatusUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Total Sections",
                    value = uiState.totalSections.toString(),
                    icon = Icons.Default.MenuBook,
                    color = Color(0xFF2196F3)
                )
                SummaryItem(
                    label = "Completed",
                    value = uiState.completedSections.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
                SummaryItem(
                    label = "Incomplete",
                    value = uiState.incompleteSections.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun CourseHeader(courseName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = courseName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

@Composable
fun MinorSubjectsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Minor Subjects",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
fun SubjectSectionStatusCard(status: SubjectSectionGradeStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status.status) {
                GradeCompletionStatus.COMPLETE -> Color(0xFFE8F5E9)
                GradeCompletionStatus.PARTIAL -> Color(0xFFFFF3E0)
                GradeCompletionStatus.INCOMPLETE -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Subject and Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "${status.subjectCode} - Section ${status.sectionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                
                // Status Badge
                StatusBadge(status = status.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Teacher Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status.teacherName.ifEmpty { "Unassigned" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { status.completionPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (status.status) {
                    GradeCompletionStatus.COMPLETE -> Color(0xFF4CAF50)
                    GradeCompletionStatus.PARTIAL -> Color(0xFFFF9800)
                    GradeCompletionStatus.INCOMPLETE -> Color(0xFFF44336)
                },
                trackColor = Color(0xFFE0E0E0)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Total Students",
                    value = status.totalStudents.toString(),
                    color = Color(0xFF2196F3)
                )
                StatItem(
                    label = "Complete",
                    value = status.studentsWithCompleteGrades.toString(),
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "Partial",
                    value = status.studentsWithPartialGrades.toString(),
                    color = Color(0xFFFF9800)
                )
                StatItem(
                    label = "No Grades",
                    value = status.studentsWithNoGrades.toString(),
                    color = Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Completion Percentage
            Text(
                text = "${status.completionPercentage}% Complete",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatusBadge(status: GradeCompletionStatus) {
    val (text, color) = when (status) {
        GradeCompletionStatus.COMPLETE -> "Complete" to Color(0xFF4CAF50)
        GradeCompletionStatus.PARTIAL -> "Partial" to Color(0xFFFF9800)
        GradeCompletionStatus.INCOMPLETE -> "Incomplete" to Color(0xFFF44336)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun EmptyGradeStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No Grade Status Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Text(
                text = "Grade status information will appear here once teachers start submitting grades.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

