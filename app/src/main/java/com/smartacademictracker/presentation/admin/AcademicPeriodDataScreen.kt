package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.common.LoadingStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicPeriodDataScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    viewModel: AcademicPeriodDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val academicPeriods by viewModel.academicPeriods.collectAsState()
    val periodSummaries by viewModel.periodSummaries.collectAsState()
    val selectedPeriodData by viewModel.selectedPeriodData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAcademicPeriodsAndSummaries()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
            // Loading State
            if (uiState.isLoading) {
                LoadingStateCard(
                    title = "Loading Period Data",
                    message = "Please wait while we load academic periods and their statistics"
                )
            } else {
                // Error State
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Content
                if (selectedPeriodData != null) {
                    // Show detailed data for selected period
                    SelectedPeriodDataContent(
                        periodData = selectedPeriodData!!,
                        onBack = { viewModel.clearSelectedPeriodData() }
                    )
                } else {
                    // Show period summaries
                    PeriodSummariesContent(
                        periodSummaries = periodSummaries,
                        onPeriodSelected = { periodId ->
                            viewModel.loadPeriodData(periodId)
                        }
                    )
                }
            }
        }
}

@Composable
fun PeriodSummariesContent(
    periodSummaries: List<com.smartacademictracker.data.model.AcademicPeriodSummary>,
    onPeriodSelected: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(periodSummaries) { summary ->
            PeriodSummaryCard(
                summary = summary,
                onClick = { onPeriodSelected(summary.periodId) }
            )
        }
    }
}

@Composable
fun PeriodSummaryCard(
    summary: com.smartacademictracker.data.model.AcademicPeriodSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = summary.periodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.academicYear} - ${summary.semester}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (summary.isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.School,
                    label = "Courses",
                    value = summary.statistics.totalCourses.toString()
                )
                StatItem(
                    icon = Icons.Default.Person,
                    label = "Teachers",
                    value = summary.statistics.totalTeachers.toString()
                )
                StatItem(
                    icon = Icons.Default.Assignment,
                    label = "Subjects",
                    value = summary.statistics.totalSubjects.toString()
                )
                StatItem(
                    icon = Icons.Default.Grade,
                    label = "Grades",
                    value = summary.statistics.totalGrades.toString()
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SelectedPeriodDataContent(
    periodData: com.smartacademictracker.data.model.AcademicPeriodData,
    onBack: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header with back button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = periodData.academicPeriod?.name ?: "Period Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        item {
            // Statistics Overview
            StatisticsOverviewCard(statistics = periodData.statistics)
        }
        
        item {
            // Detailed Data Sections
            DetailedDataSections(periodData = periodData)
        }
    }
}

@Composable
fun StatisticsOverviewCard(
    statistics: com.smartacademictracker.data.model.AcademicPeriodStatistics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistics Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid of statistics
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Courses" to statistics.totalCourses,
                    "Year Levels" to statistics.totalYearLevels,
                    "Subjects" to statistics.totalSubjects,
                    "Sections" to statistics.totalSections,
                    "Teachers" to statistics.totalTeachers,
                    "Students" to statistics.totalStudents,
                    "Admins" to statistics.totalAdmins,
                    "Teacher Applications" to statistics.totalTeacherApplications,
                    "Student Applications" to statistics.totalStudentApplications,
                    "Grades" to statistics.totalGrades,
                    "Active Assignments" to statistics.activeSectionAssignments,
                    "Pending Teacher Apps" to statistics.pendingTeacherApplications,
                    "Pending Student Apps" to statistics.pendingStudentApplications
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedDataSections(
    periodData: com.smartacademictracker.data.model.AcademicPeriodData
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Courses
        if (periodData.courses.isNotEmpty()) {
            DataSectionCard(
                title = "Courses (${periodData.courses.size})",
                icon = Icons.Default.School,
                items = periodData.courses.map { "${it.name} (${it.code})" }
            )
        }
        
        // Subjects
        if (periodData.subjects.isNotEmpty()) {
            DataSectionCard(
                title = "Subjects (${periodData.subjects.size})",
                icon = Icons.Default.Assignment,
                items = periodData.subjects.map { "${it.name} (${it.code})" }
            )
        }
        
        // Section Assignments
        if (periodData.sectionAssignments.isNotEmpty()) {
            DataSectionCard(
                title = "Section Assignments (${periodData.sectionAssignments.size})",
                icon = Icons.Default.Person,
                items = periodData.sectionAssignments.map { 
                    "${it.sectionName} - ${it.teacherName}" 
                }
            )
        }
        
        // Teacher Applications
        if (periodData.teacherApplications.isNotEmpty()) {
            DataSectionCard(
                title = "Teacher Applications (${periodData.teacherApplications.size})",
                icon = Icons.Default.Person,
                items = periodData.teacherApplications.map { 
                    "${it.teacherName} - ${it.subjectName} (${it.status})" 
                }
            )
        }
    }
}

@Composable
fun DataSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            items.take(5).forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            
                if (items.size > 5) {
                Text(
                    text = "... and ${items.size - 5} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
