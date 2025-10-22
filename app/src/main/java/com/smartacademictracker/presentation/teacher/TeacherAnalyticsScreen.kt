package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.common.ChartUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TeacherAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val classPerformance by viewModel.classPerformance.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAnalyticsData()
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
                text = "Class Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.refreshData() },
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Section
        AnalyticsFilterSection(
            selectedYearLevel = uiState.selectedYearLevel,
            selectedCourse = uiState.selectedCourse,
            selectedSubject = uiState.selectedSubject,
            selectedSection = uiState.selectedSection,
            yearLevels = uiState.availableYearLevels,
            courses = uiState.availableCourses,
            subjects = uiState.availableSubjects,
            sections = uiState.availableSections,
            onYearLevelChanged = { viewModel.updateYearLevelFilter(it) },
            onCourseChanged = { viewModel.updateCourseFilter(it) },
            onSubjectChanged = { viewModel.updateSubjectFilter(it) },
            onSectionChanged = { viewModel.updateSectionFilter(it) },
            onClearFilters = { viewModel.clearAllFilters() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall Class Performance
                item {
                    OverallClassPerformanceCard(
                        totalStudents = uiState.totalStudents,
                        averageGrade = uiState.classAverage,
                        passingStudents = uiState.passingStudents,
                        atRiskStudents = uiState.atRiskStudents
                    )
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
                } else {
                    item {
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
                                Text(
                                    text = "No Performance Data Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start inputting grades to see analytics",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
        }
    }
}

@Composable
fun OverallClassPerformanceCard(
    totalStudents: Int,
    averageGrade: Double?,
    passingStudents: Int,
    atRiskStudents: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Class Performance Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (averageGrade != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Class Average: ${String.format("%.1f", averageGrade)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Passing Rate: ${String.format("%.1f", (passingStudents.toDouble() / totalStudents * 100))}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "No grade data available yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Performance Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ClassStatItem("Total Students", totalStudents.toString())
                ClassStatItem("Passing", passingStudents.toString())
                ClassStatItem("At Risk", atRiskStudents.toString())
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
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubjectPerformanceOverviewCard(
    subjectName: String,
    totalStudents: Int,
    averageGrade: Double,
    passingRate: Double,
    gradeDistribution: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Average: ${String.format("%.1f", averageGrade)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Passing Rate: ${String.format("%.1f", passingRate)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Grade Distribution
            Text(
                text = "Grade Distribution",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                gradeDistribution.forEach { (grade, count) ->
                    GradeDistributionItem(grade, count, totalStudents)
                }
            }
        }
    }
}

@Composable
private fun GradeDistributionItem(
    grade: String,
    count: Int,
    totalStudents: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = grade,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${String.format("%.1f", (count.toDouble() / totalStudents * 100))}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ClassPerformanceComparisonChart(
    subjects: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Subject Performance Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            ChartUtils.SubjectComparisonChart(
                subjects = subjects,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsFilterSection(
    selectedYearLevel: String?,
    selectedCourse: String?,
    selectedSubject: String?,
    selectedSection: String?,
    yearLevels: List<String>,
    courses: List<String>,
    subjects: List<String>,
    sections: List<String>,
    onYearLevelChanged: (String?) -> Unit,
    onCourseChanged: (String?) -> Unit,
    onSubjectChanged: (String?) -> Unit,
    onSectionChanged: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    var expandedYearLevel by remember { mutableStateOf(false) }
    var expandedCourse by remember { mutableStateOf(false) }
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onClearFilters
                ) {
                    Text("Clear All")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Row 1: Year Level and Course
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year Level Filter
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Year Level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedYearLevel,
                        onExpandedChange = { expandedYearLevel = !expandedYearLevel }
                    ) {
                        OutlinedTextField(
                            value = selectedYearLevel ?: "All Year Levels",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYearLevel)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedYearLevel,
                            onDismissRequest = { expandedYearLevel = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Year Levels") },
                                onClick = { onYearLevelChanged(null) }
                            )
                            yearLevels.forEach { yearLevel ->
                                DropdownMenuItem(
                                    text = { Text(yearLevel) },
                                    onClick = { onYearLevelChanged(yearLevel) }
                                )
                            }
                        }
                    }
                }
                
                // Course Filter
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Course",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedCourse,
                        onExpandedChange = { expandedCourse = !expandedCourse }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse ?: "All Courses",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourse)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCourse,
                            onDismissRequest = { expandedCourse = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Courses") },
                                onClick = { onCourseChanged(null) }
                            )
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course) },
                                    onClick = { onCourseChanged(course) }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Filter Row 2: Subject and Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject Filter
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedSubject,
                        onExpandedChange = { expandedSubject = !expandedSubject }
                    ) {
                        OutlinedTextField(
                            value = selectedSubject ?: "All Subjects",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubject)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubject,
                            onDismissRequest = { expandedSubject = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Subjects") },
                                onClick = { onSubjectChanged(null) }
                            )
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject) },
                                    onClick = { onSubjectChanged(subject) }
                                )
                            }
                        }
                    }
                }
                
                // Section Filter
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Section",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedSection,
                        onExpandedChange = { expandedSection = !expandedSection }
                    ) {
                        OutlinedTextField(
                            value = selectedSection ?: "All Sections",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSection)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSection,
                            onDismissRequest = { expandedSection = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sections") },
                                onClick = { onSectionChanged(null) }
                            )
                            sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(section) },
                                    onClick = { onSectionChanged(section) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
