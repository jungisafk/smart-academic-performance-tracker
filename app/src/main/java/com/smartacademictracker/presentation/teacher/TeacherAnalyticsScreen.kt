package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Class Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
            
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
                    onYearLevelChanged = { viewModel.updateYearLevelFilter(it) },
                    onCourseChanged = { viewModel.updateCourseFilter(it) },
                    onSubjectChanged = { viewModel.updateSubjectFilter(it) },
                    onSectionChanged = { viewModel.updateSectionFilter(it) },
                    onClearFilters = { viewModel.clearAllFilters() }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Average: ${String.format("%.2f", averageGrade)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
                Text(
                    text = "Passing Rate: ${String.format("%.1f", passingRate)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Grade Distribution
            Text(
                text = "Grade Distribution",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
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
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = grade,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
        Text(
            text = "${String.format("%.1f", (count.toDouble() / totalStudents * 100))}%",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF2196F3)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Subject Performance Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Filter Analytics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onClearFilters,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Clear All",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
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
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedYearLevel,
                            onDismissRequest = { expandedYearLevel = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "All Year Levels",
                                        color = Color(0xFF333333),
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                onClick = { onYearLevelChanged(null) }
                            )
                            yearLevels.forEach { yearLevel ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            yearLevel,
                                            color = Color(0xFF333333),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        ) 
                                    },
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
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
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
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCourse,
                            onDismissRequest = { expandedCourse = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "All Courses",
                                        color = Color(0xFF333333),
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                onClick = { onCourseChanged(null) }
                            )
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            course,
                                            color = Color(0xFF333333),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        ) 
                                    },
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
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
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
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubject,
                            onDismissRequest = { expandedSubject = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "All Subjects",
                                        color = Color(0xFF333333),
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                onClick = { onSubjectChanged(null) }
                            )
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            subject,
                                            color = Color(0xFF333333),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        ) 
                                    },
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
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
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
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSection,
                            onDismissRequest = { expandedSection = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "All Sections",
                                        color = Color(0xFF333333),
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                onClick = { onSectionChanged(null) }
                            )
                            sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            section,
                                            color = Color(0xFF333333),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        ) 
                                    },
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
